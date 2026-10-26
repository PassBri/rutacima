-- Climas: la reacción "¿Qué clima te deja?" de la comunidad (web/js/32-climas.js y la app).
-- Una persona deja un solo clima por publicación: sol, tormenta, niebla, valle o cumbre.
create table if not exists public.post_climas (
  post_id    uuid not null references public.posts(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  clima      text not null check (clima in ('sol', 'tormenta', 'niebla', 'valle', 'cumbre')),
  created_at timestamptz not null default now(),
  primary key (post_id, user_id)
);
create index if not exists post_climas_post on public.post_climas(post_id);
alter table public.post_climas enable row level security;

drop policy if exists "ver climas" on public.post_climas;
create policy "ver climas" on public.post_climas for select using (
  exists (select 1 from public.posts p where p.id = post_id and public.puede_ver(p)));
drop policy if exists "dejar clima" on public.post_climas;
create policy "dejar clima" on public.post_climas for insert with check (user_id = public.yo());
drop policy if exists "cambiar clima" on public.post_climas;
create policy "cambiar clima" on public.post_climas for update using (user_id = public.yo()) with check (user_id = public.yo());
drop policy if exists "quitar clima" on public.post_climas;
create policy "quitar clima" on public.post_climas for delete using (user_id = public.yo());
