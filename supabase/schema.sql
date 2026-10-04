-- =====================================================================
-- RutaCima · Esquema de Supabase (comunidad + coach de IA)
-- Ejecuta este archivo completo en: Supabase › SQL Editor › New query › Run
-- =====================================================================

-- ---------- Perfiles (uno por usuario de Auth) ----------
create table if not exists public.profiles (
  id          uuid primary key references auth.users(id) on delete cascade,
  username    text unique not null check (char_length(username) between 3 and 30),
  nombre      text not null default '',
  bio         text not null default '',
  avatar_url  text not null default '',
  cumbre      text not null default '',
  created_at  timestamptz not null default now()
);

-- Crea el perfil automáticamente al registrarse (username y nombre vienen en los metadatos).
create or replace function public.crear_perfil() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, username, nombre)
  values (
    new.id,
    coalesce(nullif(new.raw_user_meta_data->>'username', ''), 'senderista_' || substr(new.id::text, 1, 8)),
    coalesce(new.raw_user_meta_data->>'nombre', '')
  );
  return new;
end $$;

drop trigger if exists al_crear_usuario on auth.users;
create trigger al_crear_usuario after insert on auth.users
  for each row execute function public.crear_perfil();

-- ---------- Publicaciones ----------
create table if not exists public.posts (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references public.profiles(id) on delete cascade,
  tipo         text not null check (tipo in ('LOGRO','EVIDENCIA','VISION','META','REFLEXION')),
  texto        text not null default '' check (char_length(texto) <= 2200),
  image_url    text not null default '',
  eje          text check (eje in ('VOL','MAE','VOZ','VAL','EVO','TRA')),
  anio         int  not null default extract(year from now()),
  visibilidad  text not null default 'PUBLICA' check (visibilidad in ('PUBLICA','SEGUIDORES','PRIVADA')),
  meta_titulo  text not null default '',
  created_at   timestamptz not null default now()
);
create index if not exists posts_creado on public.posts (created_at desc);
create index if not exists posts_usuario on public.posts (user_id, anio);

-- ---------- Impulsos (votos), comentarios y seguidores ----------
create table if not exists public.votes (
  post_id    uuid not null references public.posts(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, user_id)
);

create table if not exists public.comments (
  id         uuid primary key default gen_random_uuid(),
  post_id    uuid not null references public.posts(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  texto      text not null check (char_length(texto) between 1 and 1000),
  created_at timestamptz not null default now()
);
create index if not exists comments_post on public.comments (post_id, created_at);

create table if not exists public.follows (
  follower_id uuid not null references public.profiles(id) on delete cascade,
  followed_id uuid not null references public.profiles(id) on delete cascade,
  created_at  timestamptz not null default now(),
  primary key (follower_id, followed_id),
  check (follower_id <> followed_id)
);

-- ---------- Uso del coach de IA (límite diario por usuario) ----------
create table if not exists public.ai_usage (
  user_id uuid not null,
  dia     date not null default current_date,
  usos    int  not null default 0,
  primary key (user_id, dia)
);

-- ---------- Seguridad (Row Level Security) ----------
alter table public.profiles enable row level security;
alter table public.posts    enable row level security;
alter table public.votes    enable row level security;
alter table public.comments enable row level security;
alter table public.follows  enable row level security;
alter table public.ai_usage enable row level security;

-- ¿Puede el usuario actual ver la publicación?
create or replace function public.puede_ver(p public.posts) returns boolean
language sql stable as $$
  select p.visibilidad = 'PUBLICA'
      or p.user_id = auth.uid()
      or (p.visibilidad = 'SEGUIDORES' and exists (
            select 1 from public.follows f where f.follower_id = auth.uid() and f.followed_id = p.user_id))
$$;

drop policy if exists "perfiles visibles" on public.profiles;
create policy "perfiles visibles" on public.profiles for select using (true);
drop policy if exists "editar mi perfil" on public.profiles;
create policy "editar mi perfil" on public.profiles for update using (auth.uid() = id);

drop policy if exists "ver publicaciones" on public.posts;
create policy "ver publicaciones" on public.posts for select using (public.puede_ver(posts));
drop policy if exists "crear mis publicaciones" on public.posts;
create policy "crear mis publicaciones" on public.posts for insert with check (auth.uid() = user_id);
drop policy if exists "editar mis publicaciones" on public.posts;
create policy "editar mis publicaciones" on public.posts for update using (auth.uid() = user_id);
drop policy if exists "borrar mis publicaciones" on public.posts;
create policy "borrar mis publicaciones" on public.posts for delete using (auth.uid() = user_id);

drop policy if exists "ver impulsos" on public.votes;
create policy "ver impulsos" on public.votes for select using (true);
drop policy if exists "dar impulso" on public.votes;
create policy "dar impulso" on public.votes for insert with check (auth.uid() = user_id);
drop policy if exists "quitar impulso" on public.votes;
create policy "quitar impulso" on public.votes for delete using (auth.uid() = user_id);

drop policy if exists "ver comentarios" on public.comments;
create policy "ver comentarios" on public.comments for select using (
  exists (select 1 from public.posts p where p.id = post_id and public.puede_ver(p)));
drop policy if exists "comentar" on public.comments;
create policy "comentar" on public.comments for insert with check (auth.uid() = user_id);
drop policy if exists "borrar mi comentario" on public.comments;
create policy "borrar mi comentario" on public.comments for delete using (auth.uid() = user_id);

drop policy if exists "ver seguidores" on public.follows;
create policy "ver seguidores" on public.follows for select using (true);
drop policy if exists "seguir" on public.follows;
create policy "seguir" on public.follows for insert with check (auth.uid() = follower_id);
drop policy if exists "dejar de seguir" on public.follows;
create policy "dejar de seguir" on public.follows for delete using (auth.uid() = follower_id);
-- ai_usage: sin políticas → solo la Edge Function (service role) puede leerla y escribirla.

-- ---------- Fotos (Storage) ----------
insert into storage.buckets (id, name, public) values ('media', 'media', true)
  on conflict (id) do nothing;

drop policy if exists "fotos públicas" on storage.objects;
create policy "fotos públicas" on storage.objects for select using (bucket_id = 'media');
drop policy if exists "subir mis fotos" on storage.objects;
create policy "subir mis fotos" on storage.objects for insert
  with check (bucket_id = 'media' and (storage.foldername(name))[1] = auth.uid()::text);
drop policy if exists "reemplazar mis fotos" on storage.objects;
create policy "reemplazar mis fotos" on storage.objects for update
  using (bucket_id = 'media' and (storage.foldername(name))[1] = auth.uid()::text);
drop policy if exists "borrar mis fotos" on storage.objects;
create policy "borrar mis fotos" on storage.objects for delete
  using (bucket_id = 'media' and (storage.foldername(name))[1] = auth.uid()::text);
