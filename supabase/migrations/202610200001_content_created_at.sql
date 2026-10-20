-- Conditional migration; NOT executed by the Android app or by this change.
-- Existing non-null timestamps are never updated. Legacy missing dates require
-- an authentic backfill before this transaction can succeed.
begin;
do $$
declare
    content_table text;
    timestamp_type text;
    missing_dates boolean;
begin
    foreach content_table in array array['posts', 'replies'] loop
        -- Do not add DEFAULT now() until after checking historical rows: that
        -- would silently date all historical content at migration time.
        execute format('alter table public.%I add column if not exists created_at timestamptz', content_table);

        select data_type into timestamp_type
        from information_schema.columns
        where table_schema = 'public' and table_name = content_table and column_name = 'created_at';
        if timestamp_type <> 'timestamp with time zone' then
            raise exception 'public.%.created_at is %. Confirm the original timezone before converting to timestamptz.', content_table, timestamp_type;
        end if;

        execute format('select exists (select 1 from public.%I where created_at is null)', content_table)
        into missing_dates;
        if missing_dates then
            raise exception 'public.% has records without created_at. Backfill from authentic historical data, then rerun. No dates have been overwritten.', content_table;
        end if;

        execute format('alter table public.%I alter column created_at set default now(), alter column created_at set not null', content_table);
    end loop;
end $$;
commit;
