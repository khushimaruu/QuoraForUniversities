-- Read-only: run in the Supabase SQL Editor BEFORE the conditional migration.
select table_name, column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public' and table_name in ('posts', 'replies', 'profiles')
order by table_name, ordinal_position;

select conrelid::regclass as table_name, conname, pg_get_constraintdef(oid) as definition
from pg_constraint
where conrelid in ('public.posts'::regclass, 'public.replies'::regclass, 'public.profiles'::regclass);

select tablename, policyname, roles, cmd, qual, with_check
from pg_policies
where schemaname = 'public' and tablename in ('posts', 'replies', 'profiles');

select relname as table_name, relrowsecurity as rls_enabled
from pg_class
where oid in ('public.posts'::regclass, 'public.replies'::regclass, 'public.profiles'::regclass);

-- Also inspect triggers for anything that changes created_at during an UPDATE.
select event_object_table, trigger_name, event_manipulation, action_statement
from information_schema.triggers
where event_object_schema = 'public' and event_object_table in ('posts', 'replies');
