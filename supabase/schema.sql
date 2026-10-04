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
  -- Los computadores de RutaCima Web entran como usuarios anónimos: no tienen perfil propio.
  if coalesce(new.is_anonymous, false) then return new; end if;
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

-- =====================================================================
-- RutaCima Web: vincular un computador y compartir tu ruta
-- Requisito: Authentication › Sign In / Providers › "Allow anonymous sign-ins" activado.
-- La web entra como usuario anónimo, muestra un código y el teléfono (con tu cuenta)
-- lo aprueba. Desde ese momento la web lee y escribe tu ruta como un dispositivo vinculado.
-- =====================================================================

-- Tu ruta en la nube: cada fila de la base del teléfono como un documento JSON con los mismos
-- campos. tipo = tabla de la app; clave = id, fecha o clave de la fila. La web es la misma app:
-- lee, crea, edita y borra lo mismo que el teléfono.
create table if not exists public.ruta_datos (
  user_id     uuid not null references auth.users(id) on delete cascade,
  tipo        text not null check (tipo in ('perfil','proposito','accion','meta_anio','meta_mes','balance',
                'agenda','mes','checklist','ejes','respuesta','coach','frases')),
  clave       text not null check (char_length(clave) between 1 and 120),
  datos       jsonb not null default '{}'::jsonb,
  actualizado timestamptz not null default now(),
  primary key (user_id, tipo, clave)
);

-- Computadores vinculados a una cuenta (web_uid = usuario anónimo de la web).
create table if not exists public.dispositivos (
  web_uid     uuid primary key references auth.users(id) on delete cascade,
  user_id     uuid not null references auth.users(id) on delete cascade,
  nombre      text not null default 'Navegador',
  creado      timestamptz not null default now(),
  ultimo_uso  timestamptz not null default now()
);
create index if not exists dispositivos_usuario on public.dispositivos (user_id);

-- Códigos de vinculación de un solo uso (5 minutos).
create table if not exists public.vinculos (
  codigo   text primary key,
  web_uid  uuid not null references auth.users(id) on delete cascade,
  nombre   text not null default 'Navegador',
  expira   timestamptz not null default now() + interval '5 minutes',
  user_id  uuid references auth.users(id) on delete cascade
);

alter table public.ruta_datos   enable row level security;
alter table public.dispositivos enable row level security;
alter table public.vinculos     enable row level security;   -- sin políticas: solo vía funciones

-- ¿Quién soy? En el teléfono, el usuario de la sesión; en un computador vinculado, el dueño
-- de la cuenta que lo vinculó. Así la web actúa como la misma persona en toda la app.
-- Un usuario anónimo que todavía no está vinculado no es nadie (null): no ve ni escribe nada.
create or replace function public.yo() returns uuid
language sql stable security definer set search_path = public as $$
  select case
    when coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false)
      then (select d.user_id from public.dispositivos d where d.web_uid = auth.uid())
    else auth.uid()
  end
$$;

create or replace function public.es_mi_ruta(dueno uuid) returns boolean
language sql stable as $$ select dueno = public.yo() $$;

drop policy if exists "leer mi ruta" on public.ruta_datos;
create policy "leer mi ruta" on public.ruta_datos for select using (public.es_mi_ruta(user_id));
drop policy if exists "escribir mi ruta" on public.ruta_datos;
create policy "escribir mi ruta" on public.ruta_datos for insert with check (public.es_mi_ruta(user_id));
drop policy if exists "actualizar mi ruta" on public.ruta_datos;
create policy "actualizar mi ruta" on public.ruta_datos for update
  using (public.es_mi_ruta(user_id)) with check (public.es_mi_ruta(user_id));
drop policy if exists "borrar de mi ruta" on public.ruta_datos;
create policy "borrar de mi ruta" on public.ruta_datos for delete using (public.es_mi_ruta(user_id));

-- La comunidad también funciona desde la web vinculada (publicar, impulsar, comentar, seguir,
-- subir fotos), siempre a nombre del dueño de la cuenta.
create or replace function public.puede_ver(p public.posts) returns boolean
language sql stable as $$
  select p.visibilidad = 'PUBLICA'
      or p.user_id = public.yo()
      or (p.visibilidad = 'SEGUIDORES' and exists (
            select 1 from public.follows f where f.follower_id = public.yo() and f.followed_id = p.user_id))
$$;
drop policy if exists "editar mi perfil" on public.profiles;
create policy "editar mi perfil" on public.profiles for update using (id = public.yo());
drop policy if exists "crear mis publicaciones" on public.posts;
create policy "crear mis publicaciones" on public.posts for insert with check (user_id = public.yo());
drop policy if exists "editar mis publicaciones" on public.posts;
create policy "editar mis publicaciones" on public.posts for update using (user_id = public.yo());
drop policy if exists "borrar mis publicaciones" on public.posts;
create policy "borrar mis publicaciones" on public.posts for delete using (user_id = public.yo());
drop policy if exists "dar impulso" on public.votes;
create policy "dar impulso" on public.votes for insert with check (user_id = public.yo());
drop policy if exists "quitar impulso" on public.votes;
create policy "quitar impulso" on public.votes for delete using (user_id = public.yo());
drop policy if exists "comentar" on public.comments;
create policy "comentar" on public.comments for insert with check (user_id = public.yo());
drop policy if exists "borrar mi comentario" on public.comments;
create policy "borrar mi comentario" on public.comments for delete using (user_id = public.yo());
drop policy if exists "seguir" on public.follows;
create policy "seguir" on public.follows for insert with check (follower_id = public.yo());
drop policy if exists "dejar de seguir" on public.follows;
create policy "dejar de seguir" on public.follows for delete using (follower_id = public.yo());
drop policy if exists "subir mis fotos" on storage.objects;
create policy "subir mis fotos" on storage.objects for insert
  with check (bucket_id = 'media' and (storage.foldername(name))[1] = public.yo()::text);
drop policy if exists "reemplazar mis fotos" on storage.objects;
create policy "reemplazar mis fotos" on storage.objects for update
  using (bucket_id = 'media' and (storage.foldername(name))[1] = public.yo()::text);
drop policy if exists "borrar mis fotos" on storage.objects;
create policy "borrar mis fotos" on storage.objects for delete
  using (bucket_id = 'media' and (storage.foldername(name))[1] = public.yo()::text);

drop policy if exists "ver mis dispositivos" on public.dispositivos;
create policy "ver mis dispositivos" on public.dispositivos for select using (user_id = auth.uid() or web_uid = auth.uid());
drop policy if exists "desvincular" on public.dispositivos;
create policy "desvincular" on public.dispositivos for delete using (user_id = auth.uid() or web_uid = auth.uid());

-- La web pide un código (8 caracteres, sin letras confusas como O/0 o I/1).
create or replace function public.crear_vinculo(nombre text default 'Navegador') returns text
language plpgsql security definer set search_path = public, extensions as $$
declare
  alfabeto constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  c text;
begin
  if auth.uid() is null then raise exception 'Sin sesión'; end if;
  delete from public.vinculos where web_uid = auth.uid() or expira < now();
  loop
    c := '';
    for i in 1..8 loop
      c := c || substr(alfabeto, 1 + (get_byte(gen_random_bytes(1), 0) % 32), 1);
    end loop;
    exit when not exists (select 1 from public.vinculos v where v.codigo = c);
  end loop;
  insert into public.vinculos (codigo, web_uid, nombre) values (c, auth.uid(), left(coalesce(nombre, 'Navegador'), 60));
  return c;
end $$;

-- El teléfono (con cuenta, no anónima) aprueba el código. Devuelve el nombre del computador.
create or replace function public.aprobar_vinculo(codigo text) returns text
language plpgsql security definer set search_path = public as $$
declare v public.vinculos;
begin
  if auth.uid() is null or coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false) then
    raise exception 'Inicia sesión en la app para vincular';
  end if;
  select * into v from public.vinculos x
   where x.codigo = upper(replace(aprobar_vinculo.codigo, '-', '')) and x.expira > now() and x.user_id is null;
  if not found then raise exception 'El código no existe o ya venció. Genera uno nuevo en la web.'; end if;
  update public.vinculos set user_id = auth.uid() where vinculos.codigo = v.codigo;
  insert into public.dispositivos (web_uid, user_id, nombre) values (v.web_uid, auth.uid(), v.nombre)
    on conflict (web_uid) do update set user_id = excluded.user_id, nombre = excluded.nombre, creado = now(), ultimo_uso = now();
  return v.nombre;
end $$;

-- La web pregunta a qué cuenta está vinculada (y marca el último uso). null = todavía no.
create or replace function public.mi_vinculo() returns uuid
language plpgsql security definer set search_path = public as $$
declare dueno uuid;
begin
  update public.dispositivos set ultimo_uso = now() where web_uid = auth.uid() returning user_id into dueno;
  return dueno;
end $$;

revoke all on function public.crear_vinculo(text), public.aprobar_vinculo(text), public.mi_vinculo() from public, anon;
grant execute on function public.crear_vinculo(text), public.aprobar_vinculo(text), public.mi_vinculo() to authenticated;
grant execute on function public.yo(), public.es_mi_ruta(uuid) to anon, authenticated;

-- Cambios en vivo: la web ve al instante lo que se sincroniza desde el teléfono.
do $$ begin
  if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    begin
      alter publication supabase_realtime add table public.ruta_datos;
    exception when duplicate_object then null;
    end;
  end if;
end $$;
