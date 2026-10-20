-- Run in the Supabase SQL Editor before using threaded replies/reply voting.
-- Existing votes(post_id, reply_id, user_id, vote_type) is reused.
-- Existing replies remain top-level; never infer parents from @mention text.
begin;

alter table public.replies add column if not exists parent_reply_id uuid;
create unique index if not exists replies_id_post_unique on public.replies(id, post_id);
do $$ begin
  if not exists (select 1 from pg_constraint where conrelid = 'public.replies'::regclass and conname = 'replies_parent_same_post_fk') then
    alter table public.replies add constraint replies_parent_same_post_fk
      foreign key (parent_reply_id, post_id) references public.replies(id, post_id) on delete cascade;
  end if;
  if not exists (select 1 from pg_constraint where conrelid = 'public.replies'::regclass and conname = 'replies_parent_not_self') then
    alter table public.replies add constraint replies_parent_not_self check (parent_reply_id is distinct from id);
  end if;
end $$;
create index if not exists replies_parent_idx on public.replies(parent_reply_id);

-- If legacy duplicates exist this deliberately fails rather than deleting votes.
-- Review them with: select reply_id, user_id, count(*) from public.votes
-- where reply_id is not null group by reply_id, user_id having count(*) > 1;
do $$
declare reply_column smallint; user_column smallint;
begin
  select attnum into reply_column from pg_attribute where attrelid = 'public.votes'::regclass and attname = 'reply_id';
  select attnum into user_column from pg_attribute where attrelid = 'public.votes'::regclass and attname = 'user_id';
  if not exists (
    select 1 from pg_index where indrelid = 'public.votes'::regclass
      and indisunique and indisvalid and indpred is null and indnkeyatts = 2
      and ((indkey[0] = reply_column and indkey[1] = user_column)
        or (indkey[0] = user_column and indkey[1] = reply_column))
  ) then
    create unique index votes_reply_user_unique on public.votes(reply_id, user_id);
  end if;
end $$;
do $$ begin
  if not exists (select 1 from pg_constraint where conrelid = 'public.votes'::regclass and conname = 'votes_reply_valid') then
    alter table public.votes add constraint votes_reply_valid
      check (reply_id is null or (post_id is null and vote_type in (1, -1)));
  end if;
end $$;

-- Preserve all existing policies, including post-voting policies. These policies
-- cover authenticated votes on visible posts/replies and enforce ownership.
alter table public.votes enable row level security;
drop policy if exists campuscircle_votes_read on public.votes;
create policy campuscircle_votes_read on public.votes for select to authenticated using (
  (post_id is not null and exists (select 1 from public.posts p where p.id = votes.post_id))
  or (reply_id is not null and exists (select 1 from public.replies r where r.id = votes.reply_id))
);
drop policy if exists campuscircle_votes_insert on public.votes;
create policy campuscircle_votes_insert on public.votes for insert to authenticated with check (
  (select auth.uid()) = user_id and vote_type in (1, -1) and (
    (reply_id is null and post_id is not null and exists (select 1 from public.posts p where p.id = votes.post_id))
    or (post_id is null and reply_id is not null and exists (select 1 from public.replies r where r.id = votes.reply_id))
  )
);
drop policy if exists campuscircle_votes_update on public.votes;
create policy campuscircle_votes_update on public.votes for update to authenticated
using ((select auth.uid()) = user_id) with check (
  (select auth.uid()) = user_id and vote_type in (1, -1) and (
    (reply_id is null and post_id is not null and exists (select 1 from public.posts p where p.id = votes.post_id))
    or (post_id is null and reply_id is not null and exists (select 1 from public.replies r where r.id = votes.reply_id))
  )
);
drop policy if exists campuscircle_votes_delete on public.votes;
create policy campuscircle_votes_delete on public.votes for delete to authenticated using ((select auth.uid()) = user_id);

notify pgrst, 'reload schema';
commit;
