-- =====================================================================
-- Mercurio · esquema de Supabase (Fase 3)
-- Ejecutar UNA vez en: Supabase → SQL Editor → New query → Run
-- Es idempotente: puedes volver a ejecutarlo sin romper nada.
-- =====================================================================

-- ---------- Utilidad: mantiene updated_at al día ----------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- ---------- 1) profiles: una fila por usuario ----------
create table if not exists public.profiles (
  id           uuid primary key references auth.users (id) on delete cascade,
  display_name text,
  theme        text not null default 'liquid_glass',
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

-- ---------- 2) settings: ajustes sincronizados (JSON) ----------
create table if not exists public.settings (
  user_id    uuid primary key references auth.users (id) on delete cascade,
  data       jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

drop trigger if exists settings_set_updated_at on public.settings;
create trigger settings_set_updated_at
  before update on public.settings
  for each row execute function public.set_updated_at();

-- ---------- 3) favorites: favoritos propios de Mercurio (aún sin conectar en la app) ----------
create table if not exists public.favorites (
  user_id       uuid not null references auth.users (id) on delete cascade default auth.uid(),
  video_id      text not null,
  title         text not null,
  artist        text not null default '',
  thumbnail_url text,
  added_at      timestamptz not null default now(),
  primary key (user_id, video_id)
);

-- ---------- 4) listening_history: una fila por reproducción contada ----------
create table if not exists public.listening_history (
  id            bigint generated always as identity primary key,
  user_id       uuid not null references auth.users (id) on delete cascade default auth.uid(),
  video_id      text not null,
  title         text not null,
  artist        text not null default '',
  thumbnail_url text,
  played_at     timestamptz not null default now()
);

create index if not exists listening_history_user_played_idx
  on public.listening_history (user_id, played_at desc);

-- ---------- Seguridad: Row Level Security ----------
-- Cada usuario (incluidos los anónimos, que usan el rol "authenticated")
-- solo puede ver y tocar SUS filas. El rol "anon" (sin sesión) no accede a nada.
alter table public.profiles          enable row level security;
alter table public.settings          enable row level security;
alter table public.favorites         enable row level security;
alter table public.listening_history enable row level security;

revoke all on public.profiles, public.settings, public.favorites, public.listening_history from anon;
grant select, insert, update, delete on
  public.profiles, public.settings, public.favorites, public.listening_history
  to authenticated;
grant usage, select on all sequences in schema public to authenticated;

-- profiles (la clave es "id")
drop policy if exists "profiles_select_own" on public.profiles;
drop policy if exists "profiles_insert_own" on public.profiles;
drop policy if exists "profiles_update_own" on public.profiles;
drop policy if exists "profiles_delete_own" on public.profiles;
create policy "profiles_select_own" on public.profiles for select to authenticated
  using ((select auth.uid()) = id);
create policy "profiles_insert_own" on public.profiles for insert to authenticated
  with check ((select auth.uid()) = id);
create policy "profiles_update_own" on public.profiles for update to authenticated
  using ((select auth.uid()) = id) with check ((select auth.uid()) = id);
create policy "profiles_delete_own" on public.profiles for delete to authenticated
  using ((select auth.uid()) = id);

-- settings
drop policy if exists "settings_select_own" on public.settings;
drop policy if exists "settings_insert_own" on public.settings;
drop policy if exists "settings_update_own" on public.settings;
drop policy if exists "settings_delete_own" on public.settings;
create policy "settings_select_own" on public.settings for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "settings_insert_own" on public.settings for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "settings_update_own" on public.settings for update to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "settings_delete_own" on public.settings for delete to authenticated
  using ((select auth.uid()) = user_id);

-- favorites
drop policy if exists "favorites_select_own" on public.favorites;
drop policy if exists "favorites_insert_own" on public.favorites;
drop policy if exists "favorites_update_own" on public.favorites;
drop policy if exists "favorites_delete_own" on public.favorites;
create policy "favorites_select_own" on public.favorites for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "favorites_insert_own" on public.favorites for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "favorites_update_own" on public.favorites for update to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "favorites_delete_own" on public.favorites for delete to authenticated
  using ((select auth.uid()) = user_id);

-- listening_history
drop policy if exists "history_select_own" on public.listening_history;
drop policy if exists "history_insert_own" on public.listening_history;
drop policy if exists "history_delete_own" on public.listening_history;
create policy "history_select_own" on public.listening_history for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "history_insert_own" on public.listening_history for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "history_delete_own" on public.listening_history for delete to authenticated
  using ((select auth.uid()) = user_id);

-- =====================================================================
-- Comprobación rápida (opcional): debe mostrar rls = true en las 4 tablas
--   select tablename, rowsecurity from pg_tables
--   where schemaname = 'public'
--     and tablename in ('profiles','settings','favorites','listening_history');
-- =====================================================================
