-- =====================================================================
-- Rutaalacima · Esquema de Supabase (comunidad + coach de IA)
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
  -- Los computadores de Rutaalacima Web entran como usuarios anónimos: no tienen perfil propio.
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
-- Rutaalacima Web: vincular un computador y compartir tu ruta
-- Requisito: Authentication › Sign In / Providers › "Allow anonymous sign-ins" activado.
-- La web entra como usuario anónimo, muestra un código y el teléfono (con tu cuenta)
-- lo aprueba. Desde ese momento la web lee y escribe tu ruta como un dispositivo vinculado.
-- =====================================================================

-- Tu ruta en la nube: cada fila de la base del teléfono como un documento JSON con los mismos
-- campos. tipo = tabla de la app; clave = id, fecha o clave de la fila. La web es la misma app:
-- lee, crea, edita y borra lo mismo que el teléfono.
create table if not exists public.ruta_datos (
  user_id     uuid not null references auth.users(id) on delete cascade,
  tipo        text not null,
  clave       text not null check (char_length(clave) between 1 and 120),
  datos       jsonb not null default '{}'::jsonb,
  actualizado timestamptz not null default now(),
  primary key (user_id, tipo, clave)
);

-- Tipos de documento que se sincronizan (se redefine al volver a ejecutar este archivo).
alter table public.ruta_datos drop constraint if exists ruta_datos_tipo_check;
alter table public.ruta_datos add constraint ruta_datos_tipo_check check (tipo in (
  'perfil','proposito','accion','meta_anio','meta_mes','balance','agenda','mes','checklist','ejes','respuesta',
  'coach','vision','frases'));

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

-- =====================================================================
-- Mensajes 1 a 1 y coaches de vida
-- Mensajes: si dos personas se siguen mutuamente (o una es coach de la otra) chatean directo;
-- si no, la primera envía una solicitud con UN mensaje y la otra decide si la acepta.
-- Coaches: cualquiera puede postularse; solo aparecen en el directorio cuando el administrador
-- marca verificado = true (Table Editor › coaches). Con permiso del usuario, su coach puede ver
-- sus propósitos, metas, hábitos, ejes y vision board (nunca notas, guías ni chats con la IA).
-- =====================================================================

create table if not exists public.conversaciones (
  id             uuid primary key default gen_random_uuid(),
  a              uuid not null references public.profiles(id) on delete cascade,
  b              uuid not null references public.profiles(id) on delete cascade,
  estado         text not null default 'pendiente' check (estado in ('pendiente','aceptada','bloqueada')),
  iniciada_por   uuid not null references public.profiles(id) on delete cascade,
  bloqueada_por  uuid references public.profiles(id) on delete set null,
  creada         timestamptz not null default now(),
  ultimo_en      timestamptz not null default now(),
  check (a < b),
  unique (a, b)
);

create table if not exists public.mensajes (
  id              uuid primary key default gen_random_uuid(),
  conversacion_id uuid not null references public.conversaciones(id) on delete cascade,
  autor           uuid not null references public.profiles(id) on delete cascade,
  texto           text not null check (char_length(texto) between 1 and 2000),
  leido           boolean not null default false,
  creado          timestamptz not null default now()
);
create index if not exists mensajes_conversacion on public.mensajes (conversacion_id, creado);

create table if not exists public.reportes (
  id              uuid primary key default gen_random_uuid(),
  quien           uuid not null references public.profiles(id) on delete cascade,
  a_quien         uuid not null references public.profiles(id) on delete cascade,
  conversacion_id uuid references public.conversaciones(id) on delete set null,
  motivo          text not null default '' check (char_length(motivo) <= 1000),
  creado          timestamptz not null default now()
);

create table if not exists public.coaches (
  user_id      uuid primary key references public.profiles(id) on delete cascade,
  bio          text not null default '' check (char_length(bio) <= 1200),
  especialidad text not null default '' check (char_length(especialidad) <= 120),
  activo       boolean not null default true,
  verificado   boolean not null default false,
  creado       timestamptz not null default now()
);

create table if not exists public.acompanamientos (
  id              uuid primary key default gen_random_uuid(),
  coach_id        uuid not null references public.coaches(user_id) on delete cascade,
  usuario_id      uuid not null references public.profiles(id) on delete cascade,
  estado          text not null default 'solicitado' check (estado in ('solicitado','activo','terminado')),
  comparte_avance boolean not null default true,
  creado          timestamptz not null default now(),
  unique (coach_id, usuario_id),
  check (coach_id <> usuario_id)
);

alter table public.conversaciones  enable row level security;
alter table public.mensajes        enable row level security;
alter table public.reportes        enable row level security;
alter table public.coaches         enable row level security;
alter table public.acompanamientos enable row level security;

-- ¿Es coach (activo) de esta persona?
create or replace function public.es_mi_coach(coach uuid, usuario uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.acompanamientos x
                 where x.coach_id = coach and x.usuario_id = usuario and x.estado = 'activo')
$$;

-- ¿Pueden chatear directo? (se siguen mutuamente o hay acompañamiento activo)
create or replace function public.chat_directo(p uuid, q uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select (exists (select 1 from public.follows where follower_id = p and followed_id = q)
      and exists (select 1 from public.follows where follower_id = q and followed_id = p))
      or public.es_mi_coach(p, q) or public.es_mi_coach(q, p)
$$;

-- ¿Puedo escribir en esta conversación?
create or replace function public.puedo_escribir(conv uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from public.conversaciones c
    where c.id = conv and public.yo() in (c.a, c.b)
      and (c.estado = 'aceptada'
           or (c.estado = 'pendiente' and c.iniciada_por = public.yo()
               and not exists (select 1 from public.mensajes m where m.conversacion_id = c.id)))
  )
$$;

drop policy if exists "ver mis conversaciones" on public.conversaciones;
create policy "ver mis conversaciones" on public.conversaciones for select using (public.yo() in (a, b));
drop policy if exists "ver mensajes" on public.mensajes;
create policy "ver mensajes" on public.mensajes for select using (
  exists (select 1 from public.conversaciones c where c.id = conversacion_id and public.yo() in (c.a, c.b)));
drop policy if exists "escribir mensajes" on public.mensajes;
create policy "escribir mensajes" on public.mensajes for insert with check (autor = public.yo() and public.puedo_escribir(conversacion_id));
drop policy if exists "reportar" on public.reportes;
create policy "reportar" on public.reportes for insert with check (quien = public.yo());
drop policy if exists "ver coaches" on public.coaches;
create policy "ver coaches" on public.coaches for select using ((verificado and activo) or user_id = public.yo());
drop policy if exists "editar mi ficha de coach" on public.coaches;
create policy "editar mi ficha de coach" on public.coaches for update using (user_id = public.yo())
  with check (user_id = public.yo() and verificado = (select c.verificado from public.coaches c where c.user_id = public.yo()));
drop policy if exists "ver acompanamientos" on public.acompanamientos;
create policy "ver acompanamientos" on public.acompanamientos for select using (public.yo() in (coach_id, usuario_id));

-- El coach ve la ruta de quien acompaña (solo lo que sirve para guiar, y solo si la persona lo permite)
create or replace function public.puede_ver_como_coach(usuario uuid, tipo text) returns boolean
language sql stable security definer set search_path = public as $$
  select tipo in ('perfil','proposito','accion','meta_anio','meta_mes','checklist','ejes','vision')
     and exists (select 1 from public.acompanamientos x
                 where x.coach_id = public.yo() and x.usuario_id = usuario and x.estado = 'activo' and x.comparte_avance)
$$;
drop policy if exists "leer mi ruta" on public.ruta_datos;
create policy "leer mi ruta" on public.ruta_datos for select
  using (public.es_mi_ruta(user_id) or public.puede_ver_como_coach(user_id, tipo));

-- ---------- Funciones de mensajes ----------
create or replace function public.abrir_conversacion(otro uuid) returns uuid
language plpgsql security definer set search_path = public as $$
declare
  yo uuid := public.yo();
  p uuid; q uuid; c public.conversaciones;
begin
  if yo is null then raise exception 'Inicia sesión para enviar mensajes'; end if;
  if otro = yo then raise exception 'No puedes escribirte a ti mismo'; end if;
  if not exists (select 1 from public.profiles where id = otro) then raise exception 'Esa persona no existe'; end if;
  p := least(yo, otro); q := greatest(yo, otro);
  select * into c from public.conversaciones where a = p and b = q;
  if found then
    if c.estado = 'pendiente' and public.chat_directo(yo, otro) then
      update public.conversaciones set estado = 'aceptada' where id = c.id;
    end if;
    return c.id;
  end if;
  insert into public.conversaciones (a, b, iniciada_por, estado)
  values (p, q, yo, case when public.chat_directo(yo, otro) then 'aceptada' else 'pendiente' end)
  returning * into c;
  return c.id;
end $$;

create or replace function public.responder_solicitud(conv uuid, aceptar boolean) returns void
language plpgsql security definer set search_path = public as $$
declare c public.conversaciones;
begin
  select * into c from public.conversaciones where id = conv and public.yo() in (a, b);
  if not found then raise exception 'Conversación no encontrada'; end if;
  if c.iniciada_por = public.yo() then raise exception 'La otra persona es quien acepta la solicitud'; end if;
  update public.conversaciones
     set estado = case when aceptar then 'aceptada' else 'bloqueada' end,
         bloqueada_por = case when aceptar then null else public.yo() end
   where id = conv;
end $$;

create or replace function public.bloquear_conversacion(conv uuid, bloquear boolean default true) returns void
language plpgsql security definer set search_path = public as $$
declare c public.conversaciones;
begin
  select * into c from public.conversaciones where id = conv and public.yo() in (a, b);
  if not found then raise exception 'Conversación no encontrada'; end if;
  if bloquear then
    update public.conversaciones set estado = 'bloqueada', bloqueada_por = public.yo() where id = conv;
  elsif c.bloqueada_por = public.yo() then
    update public.conversaciones set estado = 'aceptada', bloqueada_por = null where id = conv;
  end if;
end $$;

create or replace function public.marcar_leidos(conv uuid) returns void
language sql security definer set search_path = public as $$
  update public.mensajes m set leido = true
   from public.conversaciones c
  where m.conversacion_id = conv and c.id = conv and public.yo() in (c.a, c.b)
    and m.autor <> public.yo() and not m.leido
$$;

-- Al llegar un mensaje se actualiza la fecha de la conversación (para ordenar la lista)
create or replace function public.al_enviar_mensaje() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  update public.conversaciones set ultimo_en = new.creado where id = new.conversacion_id;
  return new;
end $$;
drop trigger if exists al_enviar_mensaje on public.mensajes;
create trigger al_enviar_mensaje after insert on public.mensajes for each row execute function public.al_enviar_mensaje();

-- Lista de mis conversaciones con la otra persona, el último mensaje y los no leídos
create or replace function public.mis_conversaciones()
returns table (id uuid, otro uuid, otro_nombre text, otro_usuario text, otro_avatar text, estado text,
               iniciada_por uuid, bloqueada_por uuid, ultimo_texto text, ultimo_autor uuid, ultimo_en timestamptz,
               no_leidos int, es_coach boolean, es_acompanado boolean)
language sql stable security definer set search_path = public as $$
  select c.id, o.id, o.nombre, o.username, o.avatar_url, c.estado, c.iniciada_por, c.bloqueada_por,
         u.texto, u.autor, c.ultimo_en,
         (select count(*)::int from public.mensajes m where m.conversacion_id = c.id and m.autor <> public.yo() and not m.leido),
         public.es_mi_coach(o.id, public.yo()), public.es_mi_coach(public.yo(), o.id)
    from public.conversaciones c
    join public.profiles o on o.id = case when c.a = public.yo() then c.b else c.a end
    left join lateral (select m.texto, m.autor from public.mensajes m where m.conversacion_id = c.id order by m.creado desc limit 1) u on true
   where public.yo() in (c.a, c.b)
     and not (c.estado = 'bloqueada' and c.bloqueada_por <> public.yo())
   order by c.ultimo_en desc
$$;

-- ---------- Funciones de coaches ----------
create or replace function public.postularme_coach(bio text, especialidad text) returns void
language plpgsql security definer set search_path = public as $$
begin
  if public.yo() is null then raise exception 'Inicia sesión'; end if;
  insert into public.coaches (user_id, bio, especialidad) values (public.yo(), left(bio, 1200), left(especialidad, 120))
  on conflict (user_id) do update set bio = excluded.bio, especialidad = excluded.especialidad, activo = true;
end $$;

create or replace function public.directorio_coaches()
returns table (user_id uuid, nombre text, usuario text, avatar text, bio text, especialidad text, acompanados int)
language sql stable security definer set search_path = public as $$
  select c.user_id, p.nombre, p.username, p.avatar_url, c.bio, c.especialidad,
         (select count(*)::int from public.acompanamientos x where x.coach_id = c.user_id and x.estado = 'activo')
    from public.coaches c join public.profiles p on p.id = c.user_id
   where c.verificado and c.activo and c.user_id <> coalesce(public.yo(), '00000000-0000-0000-0000-000000000000')
   order by 7 desc, p.nombre
$$;

create or replace function public.solicitar_coach(coach uuid) returns uuid
language plpgsql security definer set search_path = public as $$
declare r uuid;
begin
  if public.yo() is null then raise exception 'Inicia sesión'; end if;
  if not exists (select 1 from public.coaches where user_id = coach and verificado and activo) then
    raise exception 'Ese coach no está disponible';
  end if;
  if exists (select 1 from public.acompanamientos where usuario_id = public.yo() and estado in ('solicitado','activo') and coach_id <> coach) then
    raise exception 'Ya tienes un coach o una solicitud en curso';
  end if;
  insert into public.acompanamientos (coach_id, usuario_id) values (coach, public.yo())
  on conflict (coach_id, usuario_id) do update set estado = 'solicitado'
    where public.acompanamientos.estado = 'terminado'
  returning id into r;
  if r is null then select id into r from public.acompanamientos where coach_id = coach and usuario_id = public.yo(); end if;
  return r;
end $$;

create or replace function public.responder_acompanamiento(acomp uuid, aceptar boolean) returns void
language plpgsql security definer set search_path = public as $$
declare x public.acompanamientos;
begin
  select * into x from public.acompanamientos where id = acomp and coach_id = public.yo() and estado = 'solicitado';
  if not found then raise exception 'Solicitud no encontrada'; end if;
  update public.acompanamientos set estado = case when aceptar then 'activo' else 'terminado' end where id = acomp;
  if aceptar then perform public.abrir_conversacion(x.usuario_id); end if;
end $$;

create or replace function public.terminar_acompanamiento(acomp uuid) returns void
language sql security definer set search_path = public as $$
  update public.acompanamientos set estado = 'terminado'
   where id = acomp and public.yo() in (coach_id, usuario_id)
$$;

create or replace function public.compartir_avance(acomp uuid, si boolean) returns void
language sql security definer set search_path = public as $$
  update public.acompanamientos set comparte_avance = si where id = acomp and usuario_id = public.yo()
$$;

-- Mi coach (o mi solicitud) y, si soy coach, las personas que acompaño
create or replace function public.mis_acompanamientos()
returns table (id uuid, rol text, otro uuid, otro_nombre text, otro_usuario text, otro_avatar text,
               estado text, comparte_avance boolean, especialidad text, creado timestamptz)
language sql stable security definer set search_path = public as $$
  select x.id, case when x.coach_id = public.yo() then 'coach' else 'usuario' end,
         o.id, o.nombre, o.username, o.avatar_url, x.estado, x.comparte_avance, c.especialidad, x.creado
    from public.acompanamientos x
    join public.coaches c on c.user_id = x.coach_id
    join public.profiles o on o.id = case when x.coach_id = public.yo() then x.usuario_id else x.coach_id end
   where public.yo() in (x.coach_id, x.usuario_id) and x.estado <> 'terminado'
   order by x.creado desc
$$;

revoke all on function public.abrir_conversacion(uuid), public.responder_solicitud(uuid, boolean),
  public.bloquear_conversacion(uuid, boolean), public.marcar_leidos(uuid), public.mis_conversaciones(),
  public.postularme_coach(text, text), public.directorio_coaches(), public.solicitar_coach(uuid),
  public.responder_acompanamiento(uuid, boolean), public.terminar_acompanamiento(uuid),
  public.compartir_avance(uuid, boolean), public.mis_acompanamientos() from public, anon;
grant execute on function public.abrir_conversacion(uuid), public.responder_solicitud(uuid, boolean),
  public.bloquear_conversacion(uuid, boolean), public.marcar_leidos(uuid), public.mis_conversaciones(),
  public.postularme_coach(text, text), public.directorio_coaches(), public.solicitar_coach(uuid),
  public.responder_acompanamiento(uuid, boolean), public.terminar_acompanamiento(uuid),
  public.compartir_avance(uuid, boolean), public.mis_acompanamientos() to authenticated;

-- Mensajes en vivo
do $$ begin
  if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    begin
      alter publication supabase_realtime add table public.mensajes;
    exception when duplicate_object then null;
    end;
  end if;
end $$;

-- =====================================================================
-- Audiolibros: tus propias grabaciones por capítulo
-- =====================================================================
-- Las guías se escuchan con la voz del teléfono. Si el autor sube su grabación de un capítulo,
-- esa grabación reemplaza a la voz para todos. Solo quienes estén en la tabla "autores" pueden
-- subir, cambiar o quitar grabaciones (agrega tu usuario en Table Editor › autores).

create table if not exists public.autores (
  user_id uuid primary key references auth.users (id) on delete cascade,
  creado  timestamptz not null default now()
);
alter table public.autores enable row level security;
drop policy if exists "saber si soy autor" on public.autores;
create policy "saber si soy autor" on public.autores for select using (user_id = public.yo());

create or replace function public.es_autor() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.autores where user_id = public.yo())
$$;
grant execute on function public.es_autor() to anon, authenticated;

create table if not exists public.audios (
  workbook_id text not null,
  seccion     int  not null check (seccion >= 0),
  url         text not null check (url ~ '^https://'),
  ruta        text not null,                       -- ruta del archivo en el bucket "audios"
  duracion    int,                                 -- segundos (opcional)
  subido_por  uuid default public.yo(),
  actualizado timestamptz not null default now(),
  primary key (workbook_id, seccion)
);
alter table public.audios enable row level security;
drop policy if exists "escuchar audios" on public.audios;
create policy "escuchar audios" on public.audios for select using (true);
drop policy if exists "autor sube audios" on public.audios;
create policy "autor sube audios" on public.audios for insert with check (public.es_autor());
drop policy if exists "autor cambia audios" on public.audios;
create policy "autor cambia audios" on public.audios for update using (public.es_autor()) with check (public.es_autor());
drop policy if exists "autor quita audios" on public.audios;
create policy "autor quita audios" on public.audios for delete using (public.es_autor());
grant select on public.audios to anon, authenticated;
grant insert, update, delete on public.audios to authenticated;

insert into storage.buckets (id, name, public) values ('audios', 'audios', true) on conflict (id) do nothing;
drop policy if exists "audios públicos" on storage.objects;
create policy "audios públicos" on storage.objects for select using (bucket_id = 'audios');
drop policy if exists "autor sube archivos de audio" on storage.objects;
create policy "autor sube archivos de audio" on storage.objects for insert with check (bucket_id = 'audios' and public.es_autor());
drop policy if exists "autor reemplaza archivos de audio" on storage.objects;
create policy "autor reemplaza archivos de audio" on storage.objects for update using (bucket_id = 'audios' and public.es_autor());
drop policy if exists "autor borra archivos de audio" on storage.objects;
create policy "autor borra archivos de audio" on storage.objects for delete using (bucket_id = 'audios' and public.es_autor());

-- =====================================================================
-- Comunidad segura y tu cuenta (requisitos de Google Play)
-- =====================================================================
-- Reportar publicaciones y comentarios, bloquear personas, ocultar lo reportado por varios,
-- límites contra el spam y eliminar la cuenta con todos sus datos.

-- Los reportes también pueden ser de una publicación o de un comentario
alter table public.reportes add column if not exists post_id uuid references public.posts(id) on delete cascade;
alter table public.reportes add column if not exists comment_id uuid references public.comments(id) on delete cascade;
alter table public.reportes add column if not exists revisado boolean not null default false;
create unique index if not exists reportes_un_post on public.reportes (quien, post_id) where post_id is not null;

-- Lo que reportan 3 personas distintas se oculta solo hasta que lo revises (Table Editor › posts › oculto)
alter table public.posts add column if not exists oculto boolean not null default false;
alter table public.comments add column if not exists oculto boolean not null default false;

create or replace function public.al_reportar() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.post_id is not null and (select count(distinct quien) from public.reportes where post_id = new.post_id) >= 3 then
    update public.posts set oculto = true where id = new.post_id;
  end if;
  if new.comment_id is not null and (select count(distinct quien) from public.reportes where comment_id = new.comment_id) >= 3 then
    update public.comments set oculto = true where id = new.comment_id;
  end if;
  return new;
end $$;
drop trigger if exists al_reportar on public.reportes;
create trigger al_reportar after insert on public.reportes for each row execute function public.al_reportar();

-- Bloquear: ninguno ve las publicaciones ni los comentarios del otro, ni pueden escribirse
create table if not exists public.bloqueos (
  quien   uuid not null references public.profiles(id) on delete cascade,
  a_quien uuid not null references public.profiles(id) on delete cascade,
  creado  timestamptz not null default now(),
  primary key (quien, a_quien),
  check (quien <> a_quien)
);
alter table public.bloqueos enable row level security;
drop policy if exists "ver mis bloqueos" on public.bloqueos;
create policy "ver mis bloqueos" on public.bloqueos for select using (quien = public.yo());
drop policy if exists "bloquear" on public.bloqueos;
create policy "bloquear" on public.bloqueos for insert with check (quien = public.yo());
drop policy if exists "desbloquear" on public.bloqueos;
create policy "desbloquear" on public.bloqueos for delete using (quien = public.yo());
grant select, insert, delete on public.bloqueos to authenticated;

create or replace function public.bloqueados(x uuid, y uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.bloqueos where (quien = x and a_quien = y) or (quien = y and a_quien = x))
$$;

create or replace function public.puede_ver(p public.posts) returns boolean
language sql stable as $$
  select (p.user_id = public.yo())
      or (not p.oculto
          and not public.bloqueados(public.yo(), p.user_id)
          and (p.visibilidad = 'PUBLICA'
               or (p.visibilidad = 'SEGUIDORES' and exists (
                     select 1 from public.follows f where f.follower_id = public.yo() and f.followed_id = p.user_id))))
$$;
drop policy if exists "ver comentarios" on public.comments;
create policy "ver comentarios" on public.comments for select using (
  (user_id = public.yo() or (not oculto and not public.bloqueados(public.yo(), user_id)))
  and exists (select 1 from public.posts p where p.id = post_id and public.puede_ver(p)));

-- Con alguien bloqueado no se abre conversación (y la que había queda bloqueada)
create or replace function public.al_bloquear() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  update public.conversaciones set estado = 'bloqueada', bloqueada_por = new.quien
   where a = least(new.quien, new.a_quien) and b = greatest(new.quien, new.a_quien) and estado <> 'bloqueada';
  delete from public.follows where (follower_id = new.quien and followed_id = new.a_quien) or (follower_id = new.a_quien and followed_id = new.quien);
  return new;
end $$;
drop trigger if exists al_bloquear on public.bloqueos;
create trigger al_bloquear after insert on public.bloqueos for each row execute function public.al_bloquear();

create or replace function public.no_si_bloqueado() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if public.bloqueados(new.a, new.b) then raise exception 'No puedes escribirle a esta persona'; end if;
  return new;
end $$;
drop trigger if exists no_si_bloqueado on public.conversaciones;
create trigger no_si_bloqueado before insert on public.conversaciones for each row execute function public.no_si_bloqueado();

-- Límites contra el spam (por persona)
create or replace function public.limite(cuantos bigint, maximo int, que text) returns void
language plpgsql as $$
begin
  if cuantos >= maximo then raise exception 'Vas muy rápido: espera un momento antes de % de nuevo', que; end if;
end $$;

create or replace function public.limitar_mensajes() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  perform public.limite((select count(*) from public.mensajes where autor = new.autor and creado > now() - interval '1 minute'), 20, 'enviar mensajes');
  return new;
end $$;
drop trigger if exists limitar_mensajes on public.mensajes;
create trigger limitar_mensajes before insert on public.mensajes for each row execute function public.limitar_mensajes();

create or replace function public.limitar_solicitudes() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.estado = 'pendiente' then
    perform public.limite((select count(*) from public.conversaciones where iniciada_por = new.iniciada_por
                            and estado = 'pendiente' and creada > now() - interval '1 day'), 15, 'escribir a personas nuevas');
  end if;
  return new;
end $$;
drop trigger if exists limitar_solicitudes on public.conversaciones;
create trigger limitar_solicitudes before insert on public.conversaciones for each row execute function public.limitar_solicitudes();

create or replace function public.limitar_publicaciones() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  perform public.limite((select count(*) from public.posts where user_id = new.user_id and created_at > now() - interval '1 day'), 20, 'publicar');
  return new;
end $$;
drop trigger if exists limitar_publicaciones on public.posts;
create trigger limitar_publicaciones before insert on public.posts for each row execute function public.limitar_publicaciones();

create or replace function public.limitar_comentarios() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  perform public.limite((select count(*) from public.comments where user_id = new.user_id and created_at > now() - interval '1 hour'), 60, 'comentar');
  return new;
end $$;
drop trigger if exists limitar_comentarios on public.comments;
create trigger limitar_comentarios before insert on public.comments for each row execute function public.limitar_comentarios();

-- Eliminar la cuenta: borra el usuario y, en cascada, su perfil, ruta, publicaciones, comentarios,
-- impulsos, mensajes, coaches, acompañamientos y computadores vinculados; y sus archivos.
create or replace function public.eliminar_mi_cuenta() returns void
language plpgsql security definer set search_path = public as $$
declare
  yo uuid := public.yo();
begin
  if yo is null then raise exception 'Inicia sesión para eliminar tu cuenta'; end if;
  delete from storage.objects where bucket_id = 'media' and (storage.foldername(name))[1] = yo::text;
  delete from auth.users where id in (select web_uid from public.dispositivos where user_id = yo);
  delete from auth.users where id = yo;
end $$;
revoke all on function public.eliminar_mi_cuenta() from public, anon;
grant execute on function public.eliminar_mi_cuenta() to authenticated;

-- Limpieza: computadores sin usar en 90 días y sesiones anónimas que nunca se vincularon
create or replace function public.limpiar_sesiones_web() returns void
language plpgsql security definer set search_path = public as $$
begin
  delete from auth.users u where u.id in (select web_uid from public.dispositivos where ultimo_uso < now() - interval '90 days');
  delete from auth.users u where coalesce(u.is_anonymous, false)
    and u.created_at < now() - interval '1 day'
    and not exists (select 1 from public.dispositivos d where d.web_uid = u.id)
    and not exists (select 1 from public.vinculos v where v.web_uid = u.id and v.expira > now());
end $$;
revoke all on function public.limpiar_sesiones_web() from public, anon, authenticated;
-- Si tu proyecto tiene la extensión pg_cron (Database › Extensions), se programa sola cada noche:
do $$ begin
  if exists (select 1 from pg_extension where extname = 'pg_cron') then
    perform cron.schedule('limpiar-sesiones-web', '17 4 * * *', 'select public.limpiar_sesiones_web()');
  end if;
end $$;
