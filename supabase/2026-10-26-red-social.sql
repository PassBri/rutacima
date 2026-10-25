-- =====================================================================
-- SuRoute · Red social (avisos, búsqueda, primeros pasos, menciones, retos, seguridad)
-- Pegar completo en Supabase › SQL Editor y ejecutar. Es idempotente: se puede volver a correr.
-- =====================================================================

-- ---------- 1. Avisos sociales ----------
-- Preferencias de avisos por persona: {"IMPULSO":false, ...}. Lo que no aparece está encendido.
alter table public.profiles add column if not exists avisos jsonb not null default '{}'::jsonb;

create table if not exists public.notificaciones (
  id         bigint generated always as identity primary key,
  user_id    uuid not null references public.profiles(id) on delete cascade,   -- quien recibe el aviso
  actor_id   uuid references public.profiles(id) on delete cascade,            -- quien lo provocó
  tipo       text not null check (tipo in ('IMPULSO','COMENTARIO','RESPUESTA','SEGUIDOR','LIKE_COMENTARIO','MENCION','RETO','COACH','SISTEMA')),
  post_id    uuid references public.posts(id) on delete cascade,
  comment_id uuid references public.comments(id) on delete cascade,
  texto      text not null default '',
  leida      boolean not null default false,
  created_at timestamptz not null default now()
);
create index if not exists notificaciones_destino on public.notificaciones (user_id, created_at desc);
alter table public.notificaciones enable row level security;
drop policy if exists "ver mis avisos" on public.notificaciones;
create policy "ver mis avisos" on public.notificaciones for select using (user_id = public.yo());
drop policy if exists "marcar mis avisos" on public.notificaciones;
create policy "marcar mis avisos" on public.notificaciones for update using (user_id = public.yo());
drop policy if exists "borrar mis avisos" on public.notificaciones;
create policy "borrar mis avisos" on public.notificaciones for delete using (user_id = public.yo());
grant select, update, delete on public.notificaciones to anon, authenticated;

-- Crea un aviso (sin avisarse a uno mismo, sin avisos entre bloqueados y respetando las preferencias).
-- Los avisos que se repiten (un "me gusta" quitado y vuelto a dar) no se duplican.
create or replace function public.notificar(p_dest uuid, p_actor uuid, p_tipo text, p_post uuid, p_comment uuid, p_texto text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if p_dest is null or p_dest = p_actor then return; end if;
  if exists (select 1 from public.bloqueos b where (b.quien = p_dest and b.a_quien = p_actor) or (b.quien = p_actor and b.a_quien = p_dest)) then return; end if;
  if coalesce((select (avisos ->> p_tipo)::boolean from public.profiles where id = p_dest), true) = false then return; end if;
  delete from public.notificaciones n
   where n.user_id = p_dest and n.actor_id is not distinct from p_actor and n.tipo = p_tipo
     and n.post_id is not distinct from p_post and n.comment_id is not distinct from p_comment;
  insert into public.notificaciones (user_id, actor_id, tipo, post_id, comment_id, texto)
  values (p_dest, p_actor, p_tipo, p_post, p_comment, left(coalesce(p_texto, ''), 140));
end $$;
revoke all on function public.notificar(uuid, uuid, text, uuid, uuid, text) from public, anon, authenticated;

create or replace function public.aviso_impulso() returns trigger language plpgsql security definer set search_path = public as $$
begin
  perform public.notificar((select user_id from public.posts where id = new.post_id), new.user_id, 'IMPULSO', new.post_id, null, '');
  return new;
end $$;
drop trigger if exists aviso_impulso on public.votes;
create trigger aviso_impulso after insert on public.votes for each row execute function public.aviso_impulso();

create or replace function public.aviso_comentario() returns trigger language plpgsql security definer set search_path = public as $$
declare dueno uuid; padre uuid;
begin
  select user_id into dueno from public.posts where id = new.post_id;
  if new.parent_id is not null then
    select user_id into padre from public.comments where id = new.parent_id;
    perform public.notificar(padre, new.user_id, 'RESPUESTA', new.post_id, new.id, new.texto);
  end if;
  if dueno is distinct from padre then
    perform public.notificar(dueno, new.user_id, 'COMENTARIO', new.post_id, new.id, new.texto);
  end if;
  return new;
end $$;
drop trigger if exists aviso_comentario on public.comments;
create trigger aviso_comentario after insert on public.comments for each row execute function public.aviso_comentario();

create or replace function public.aviso_seguidor() returns trigger language plpgsql security definer set search_path = public as $$
begin
  perform public.notificar(new.followed_id, new.follower_id, 'SEGUIDOR', null, null, '');
  return new;
end $$;
drop trigger if exists aviso_seguidor on public.follows;
create trigger aviso_seguidor after insert on public.follows for each row execute function public.aviso_seguidor();

create or replace function public.aviso_like_comentario() returns trigger language plpgsql security definer set search_path = public as $$
declare c record;
begin
  select user_id, post_id, texto into c from public.comments where id = new.comment_id;
  perform public.notificar(c.user_id, new.user_id, 'LIKE_COMENTARIO', c.post_id, new.comment_id, c.texto);
  return new;
end $$;
drop trigger if exists aviso_like_comentario on public.comment_likes;
create trigger aviso_like_comentario after insert on public.comment_likes for each row execute function public.aviso_like_comentario();

-- Al quitar el "me gusta" o dejar de seguir, el aviso se va
create or replace function public.quitar_aviso() returns trigger language plpgsql security definer set search_path = public as $$
begin
  if tg_table_name = 'votes' then
    delete from public.notificaciones where tipo = 'IMPULSO' and actor_id = old.user_id and post_id = old.post_id and not leida;
  elsif tg_table_name = 'follows' then
    delete from public.notificaciones where tipo = 'SEGUIDOR' and actor_id = old.follower_id and user_id = old.followed_id and not leida;
  elsif tg_table_name = 'comment_likes' then
    delete from public.notificaciones where tipo = 'LIKE_COMENTARIO' and actor_id = old.user_id and comment_id = old.comment_id and not leida;
  end if;
  return old;
end $$;
drop trigger if exists quitar_aviso on public.votes;
create trigger quitar_aviso after delete on public.votes for each row execute function public.quitar_aviso();
drop trigger if exists quitar_aviso on public.follows;
create trigger quitar_aviso after delete on public.follows for each row execute function public.quitar_aviso();
drop trigger if exists quitar_aviso on public.comment_likes;
create trigger quitar_aviso after delete on public.comment_likes for each row execute function public.quitar_aviso();

-- Guardar mis preferencias de avisos (también desde un computador vinculado)
create or replace function public.mis_avisos(p_avisos jsonb) returns void
language sql security definer set search_path = public as $$
  update public.profiles set avisos = coalesce(p_avisos, '{}'::jsonb) where id = public.yo();
$$;
grant execute on function public.mis_avisos(jsonb) to anon, authenticated;

-- ---------- 5. Menciones e invitaciones ----------
-- @usuario en una publicación o un comentario avisa a esa persona
create or replace function public.aviso_menciones() returns trigger language plpgsql security definer set search_path = public as $$
declare u text; dest uuid; post uuid; ya uuid[] := '{}';
begin
  -- Cada tabla con su campo (en PL/pgSQL los campos de NEW se resuelven al ejecutar cada sentencia)
  if tg_table_name = 'posts' then
    post := new.id;
    if new.visibilidad = 'PRIVADA' then return new; end if;
  else
    post := new.post_id;
    -- Quien ya recibe el aviso del comentario o de la respuesta no recibe además el de la mención
    ya := array[(select user_id from public.posts where id = new.post_id), (select user_id from public.comments where id = new.parent_id)];
  end if;
  for u in select distinct lower(m[1]) from regexp_matches(coalesce(new.texto, ''), '(?:^|[^a-z0-9._])@([a-z0-9._]{3,30})', 'gi') as m loop
    select id into dest from public.profiles where username = u;
    if dest is not null and not coalesce(dest = any(ya), false) then
      perform public.notificar(dest, new.user_id, 'MENCION', post, case when tg_table_name = 'comments' then new.id else null end, new.texto);
    end if;
  end loop;
  return new;
end $$;
drop trigger if exists aviso_menciones on public.posts;
create trigger aviso_menciones after insert on public.posts for each row execute function public.aviso_menciones();
drop trigger if exists aviso_menciones on public.comments;
create trigger aviso_menciones after insert on public.comments for each row execute function public.aviso_menciones();

-- Invitación personal: quien crea su cuenta con el enlace de alguien queda siguiéndose con esa persona
create or replace function public.aceptar_invitacion(p_usuario text) returns boolean
language plpgsql security definer set search_path = public as $$
declare yo uuid := public.yo(); otro uuid;
begin
  select id into otro from public.profiles where username = lower(p_usuario);
  if yo is null or otro is null or otro = yo then return false; end if;
  if public.bloqueados(yo, otro) then return false; end if;
  insert into public.follows (follower_id, followed_id) values (yo, otro) on conflict do nothing;
  insert into public.follows (follower_id, followed_id) values (otro, yo) on conflict do nothing;
  return true;
end $$;
grant execute on function public.aceptar_invitacion(text) to anon, authenticated;

-- ---------- 6. Carrusel y video, retos colectivos y coaches verificados ----------
-- Varias fotos o un video por publicación: [{"t":"img"|"vid","u":"https://…"}]. image_url sigue siendo la portada.
alter table public.posts add column if not exists media jsonb not null default '[]'::jsonb;
do $$ begin
  alter table public.posts add constraint posts_media_max check (jsonb_typeof(media) = 'array' and jsonb_array_length(media) <= 10);
exception when duplicate_object then null; end $$;

-- Retos del mes, abiertos a toda la comunidad (los crea la moderación)
create table if not exists public.retos (
  id          uuid primary key default gen_random_uuid(),
  titulo      text not null check (char_length(titulo) between 3 and 80),
  descripcion text not null default '' check (char_length(descripcion) <= 600),
  eje         text check (eje in ('VOL','MAE','VOZ','VAL','EVO','TRA')),
  inicio      date not null default current_date,
  fin         date not null default (current_date + 29),
  creado_por  uuid references public.profiles(id) on delete set null,
  created_at  timestamptz not null default now(),
  check (fin >= inicio)
);
create table if not exists public.reto_participantes (
  reto_id uuid not null references public.retos(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  unido   timestamptz not null default now(),
  primary key (reto_id, user_id)
);
create table if not exists public.reto_marcas (
  reto_id uuid not null references public.retos(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  dia     date not null default current_date,
  primary key (reto_id, user_id, dia)
);
alter table public.retos enable row level security;
alter table public.reto_participantes enable row level security;
alter table public.reto_marcas enable row level security;
drop policy if exists "ver retos" on public.retos;
create policy "ver retos" on public.retos for select using (true);
drop policy if exists "moderar retos" on public.retos;
create policy "moderar retos" on public.retos for all using (public.es_moderador()) with check (public.es_moderador());
drop policy if exists "ver participantes" on public.reto_participantes;
create policy "ver participantes" on public.reto_participantes for select using (true);
drop policy if exists "unirme" on public.reto_participantes;
create policy "unirme" on public.reto_participantes for insert with check (user_id = public.yo());
drop policy if exists "salirme" on public.reto_participantes;
create policy "salirme" on public.reto_participantes for delete using (user_id = public.yo());
drop policy if exists "ver marcas" on public.reto_marcas;
create policy "ver marcas" on public.reto_marcas for select using (true);
drop policy if exists "marcar" on public.reto_marcas;
create policy "marcar" on public.reto_marcas for insert with check (user_id = public.yo()
  and exists (select 1 from public.reto_participantes p where p.reto_id = reto_marcas.reto_id and p.user_id = public.yo()));
drop policy if exists "desmarcar" on public.reto_marcas;
create policy "desmarcar" on public.reto_marcas for delete using (user_id = public.yo());
grant select on public.retos, public.reto_participantes, public.reto_marcas to anon, authenticated;
grant insert, update, delete on public.retos to authenticated;
grant insert, delete on public.reto_participantes, public.reto_marcas to anon, authenticated;

-- Retos vigentes con su gente y mi avance
create or replace function public.retos_activos() returns table (
  id uuid, titulo text, descripcion text, eje text, inicio date, fin date,
  participantes int, marcas_hoy int, yo_participo boolean, mis_dias int, hoy_marcado boolean)
language sql stable security definer set search_path = public as $$
  select r.id, r.titulo, r.descripcion, r.eje, r.inicio, r.fin,
    (select count(*)::int from public.reto_participantes p where p.reto_id = r.id),
    (select count(*)::int from public.reto_marcas m where m.reto_id = r.id and m.dia = current_date),
    exists (select 1 from public.reto_participantes p where p.reto_id = r.id and p.user_id = public.yo()),
    (select count(*)::int from public.reto_marcas m where m.reto_id = r.id and m.user_id = public.yo()),
    exists (select 1 from public.reto_marcas m where m.reto_id = r.id and m.user_id = public.yo() and m.dia = current_date)
  from public.retos r where current_date between r.inicio and r.fin order by r.inicio desc
$$;
grant execute on function public.retos_activos() to anon, authenticated;

-- Coaches por verificar (solo moderación) y verificar o rechazar
create or replace function public.coaches_por_verificar() returns table (user_id uuid, nombre text, username text, bio text, especialidad text, creado timestamptz)
language sql stable security definer set search_path = public as $$
  select c.user_id, p.nombre, p.username, c.bio, c.especialidad, c.creado
  from public.coaches c join public.profiles p on p.id = c.user_id
  where public.es_moderador() and not c.verificado and c.activo order by c.creado
$$;
create or replace function public.verificar_coach(p_user uuid, p_si boolean) returns void
language plpgsql security definer set search_path = public as $$
begin
  if not public.es_moderador() then raise exception 'Solo moderación'; end if;
  if p_si then update public.coaches set verificado = true where user_id = p_user;
  else update public.coaches set activo = false where user_id = p_user; end if;
  perform public.notificar(p_user, null, 'COACH', null, null,
    case when p_si then 'Tu perfil de coach de vida fue verificado. Ya apareces en el directorio.' else 'Tu postulación como coach de vida no fue aprobada por ahora.' end);
end $$;
grant execute on function public.coaches_por_verificar(), public.verificar_coach(uuid, boolean) to authenticated;

-- ---------- 7. Seguridad ----------
-- Silenciar: dejo de ver a alguien (y sus avisos) sin bloquearlo; esa persona no se entera
create table if not exists public.silencios (
  quien   uuid not null references public.profiles(id) on delete cascade,
  a_quien uuid not null references public.profiles(id) on delete cascade,
  creado  timestamptz not null default now(),
  primary key (quien, a_quien), check (quien <> a_quien)
);
alter table public.silencios enable row level security;
drop policy if exists "mis silencios" on public.silencios;
create policy "mis silencios" on public.silencios for all using (quien = public.yo()) with check (quien = public.yo());
grant select, insert, delete on public.silencios to anon, authenticated;

-- Los avisos tampoco llegan de quien silencié
create or replace function public.notificar(p_dest uuid, p_actor uuid, p_tipo text, p_post uuid, p_comment uuid, p_texto text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if p_dest is null or p_dest = p_actor then return; end if;
  if exists (select 1 from public.bloqueos b where (b.quien = p_dest and b.a_quien = p_actor) or (b.quien = p_actor and b.a_quien = p_dest)) then return; end if;
  if exists (select 1 from public.silencios s where s.quien = p_dest and s.a_quien = p_actor) then return; end if;
  if coalesce((select (avisos ->> p_tipo)::boolean from public.profiles where id = p_dest), true) = false then return; end if;
  delete from public.notificaciones n
   where n.user_id = p_dest and n.actor_id is not distinct from p_actor and n.tipo = p_tipo
     and n.post_id is not distinct from p_post and n.comment_id is not distinct from p_comment;
  insert into public.notificaciones (user_id, actor_id, tipo, post_id, comment_id, texto)
  values (p_dest, p_actor, p_tipo, p_post, p_comment, left(coalesce(p_texto, ''), 140));
end $$;
revoke all on function public.notificar(uuid, uuid, text, uuid, uuid, text) from public, anon, authenticated;

-- Quién puede comentar mis publicaciones: TODOS, SIGUEN (personas que yo sigo) o NADIE
alter table public.profiles add column if not exists comentarios_de text not null default 'TODOS';
do $$ begin
  alter table public.profiles add constraint profiles_comentarios_de check (comentarios_de in ('TODOS','SIGUEN','NADIE'));
exception when duplicate_object then null; end $$;
create or replace function public.puede_comentar() returns trigger language plpgsql security definer set search_path = public as $$
declare dueno uuid; regla text;
begin
  select p.user_id, pr.comentarios_de into dueno, regla from public.posts p join public.profiles pr on pr.id = p.user_id where p.id = new.post_id;
  if dueno is null or dueno = new.user_id or regla = 'TODOS' then return new; end if;
  if regla = 'NADIE' then raise exception 'Esta persona cerró los comentarios de sus publicaciones'; end if;
  if not exists (select 1 from public.follows f where f.follower_id = dueno and f.followed_id = new.user_id) then
    raise exception 'Esta persona solo recibe comentarios de quienes sigue';
  end if;
  return new;
end $$;
drop trigger if exists puede_comentar on public.comments;
create trigger puede_comentar before insert on public.comments for each row execute function public.puede_comentar();

-- Ocultar comentarios en mis publicaciones (quien lo escribió lo sigue viendo y no recibe aviso)
alter table public.comments add column if not exists oculto_autor boolean not null default false;
drop policy if exists "ver comentarios" on public.comments;
create policy "ver comentarios" on public.comments for select using (
  (user_id = public.yo() or (not oculto and not public.bloqueados(public.yo(), user_id)
     and (not oculto_autor or exists (select 1 from public.posts p where p.id = post_id and p.user_id = public.yo()))))
  and exists (select 1 from public.posts p where p.id = post_id and public.puede_ver(p)));
create or replace function public.ocultar_comentario(p_comment uuid, p_si boolean) returns void
language plpgsql security definer set search_path = public as $$
begin
  update public.comments c set oculto_autor = p_si
   where c.id = p_comment and exists (select 1 from public.posts p where p.id = c.post_id and p.user_id = public.yo());
  if not found then raise exception 'Solo puedes ocultar comentarios de tus publicaciones'; end if;
end $$;
create or replace function public.mis_comentarios_de(p_regla text) returns void
language sql security definer set search_path = public as $$
  update public.profiles set comentarios_de = p_regla where id = public.yo() and p_regla in ('TODOS','SIGUEN','NADIE');
$$;
grant execute on function public.ocultar_comentario(uuid, boolean), public.mis_comentarios_de(text) to anon, authenticated;

-- Menores (14 a 17 años): lo que publican llega solo a sus seguidores y solo reciben mensajes de quienes siguen
alter table public.profiles add column if not exists menor boolean not null default false;
create or replace function public.mi_edad_menor(p_si boolean) returns void
language sql security definer set search_path = public as $$ update public.profiles set menor = p_si where id = public.yo(); $$;
grant execute on function public.mi_edad_menor(boolean) to anon, authenticated;
create or replace function public.proteger_menor_post() returns trigger language plpgsql security definer set search_path = public as $$
begin
  if new.visibilidad = 'PUBLICA' and coalesce((select menor from public.profiles where id = new.user_id), false) then new.visibilidad := 'SEGUIDORES'; end if;
  return new;
end $$;
drop trigger if exists proteger_menor_post on public.posts;
create trigger proteger_menor_post before insert or update of visibilidad on public.posts for each row execute function public.proteger_menor_post();
create or replace function public.proteger_menor_chat() returns trigger language plpgsql security definer set search_path = public as $$
declare otro uuid := case when new.a = new.iniciada_por then new.b else new.a end;
begin
  if coalesce((select menor from public.profiles where id = otro), false)
     and not exists (select 1 from public.follows f where f.follower_id = otro and f.followed_id = new.iniciada_por) then
    raise exception 'Esta persona solo recibe mensajes de quienes sigue';
  end if;
  return new;
end $$;
drop trigger if exists proteger_menor_chat on public.conversaciones;
create trigger proteger_menor_chat before insert on public.conversaciones for each row execute function public.proteger_menor_chat();

-- Quien reportó recibe la respuesta de la moderación
create or replace function public.responder_reportes() returns trigger language plpgsql security definer set search_path = public as $$
declare q uuid; msg text;
begin
  msg := case
    when new.accion in ('ocultar','eliminar') then 'Revisamos tu reporte y tomamos medidas. Gracias por cuidar la comunidad.'
    when new.accion = 'suspender' then 'Revisamos tu reporte: la cuenta fue suspendida. Gracias por cuidar la comunidad.'
    when new.accion in ('restaurar','descartar') then 'Revisamos tu reporte y no encontramos una falta a las normas. Gracias por avisar.'
    else null end;
  if msg is null then return new; end if;
  for q in select distinct r.quien from public.reportes r
    where (new.tipo = 'post' and r.post_id = new.objetivo) or (new.tipo = 'comentario' and r.comment_id = new.objetivo)
       or (new.tipo = 'conversacion' and r.conversacion_id = new.objetivo) or (new.tipo = 'persona' and r.a_quien = new.objetivo)
  loop perform public.notificar(q, null, 'SISTEMA', null, null, msg); end loop;
  return new;
end $$;
drop trigger if exists responder_reportes on public.moderacion_log;
create trigger responder_reportes after insert on public.moderacion_log for each row execute function public.responder_reportes();

-- Apelar una suspensión
create table if not exists public.apelaciones (
  id        bigint generated always as identity primary key,
  user_id   uuid not null references public.profiles(id) on delete cascade,
  texto     text not null check (char_length(texto) between 10 and 1500),
  estado    text not null default 'pendiente' check (estado in ('pendiente','aceptada','rechazada')),
  respuesta text not null default '',
  creado    timestamptz not null default now()
);
alter table public.apelaciones enable row level security;
drop policy if exists "ver mis apelaciones" on public.apelaciones;
create policy "ver mis apelaciones" on public.apelaciones for select using (user_id = public.yo() or public.es_moderador());
drop policy if exists "apelar" on public.apelaciones;
create policy "apelar" on public.apelaciones for insert with check (user_id = public.yo()
  and exists (select 1 from public.suspensiones s where s.user_id = public.yo() and s.hasta > now())
  and not exists (select 1 from public.apelaciones a where a.user_id = public.yo() and a.estado = 'pendiente'));
grant select, insert on public.apelaciones to anon, authenticated;
create or replace function public.resolver_apelacion(p_id bigint, p_aceptar boolean, p_respuesta text) returns void
language plpgsql security definer set search_path = public as $$
declare u uuid;
begin
  if not public.es_moderador() then raise exception 'Solo moderación'; end if;
  update public.apelaciones set estado = case when p_aceptar then 'aceptada' else 'rechazada' end, respuesta = left(coalesce(p_respuesta, ''), 500)
   where id = p_id and estado = 'pendiente' returning user_id into u;
  if u is null then return; end if;
  if p_aceptar then perform public.suspender(u, 0, 'Apelación aceptada'); end if;
  perform public.notificar(u, null, 'SISTEMA', null, null,
    case when p_aceptar then 'Aceptamos tu apelación: ya puedes volver a participar en la comunidad.' else 'Revisamos tu apelación y la suspensión se mantiene. ' || coalesce(p_respuesta, '') end);
end $$;
grant execute on function public.resolver_apelacion(bigint, boolean, text) to authenticated;
-- Al eliminar por moderación, los reportes se borran con la publicación: se avisa antes a quienes reportaron
create or replace function public.avisar_reportes_al_borrar() returns trigger language plpgsql security definer set search_path = public as $$
declare q uuid;
begin
  if not public.es_moderador() or old.user_id = public.yo() then return old; end if;
  for q in select distinct r.quien from public.reportes r
    where (tg_table_name = 'posts' and r.post_id = old.id) or (tg_table_name = 'comments' and r.comment_id = old.id)
  loop perform public.notificar(q, null, 'SISTEMA', null, null, 'Revisamos tu reporte y tomamos medidas. Gracias por cuidar la comunidad.'); end loop;
  return old;
end $$;
drop trigger if exists avisar_reportes_al_borrar on public.posts;
create trigger avisar_reportes_al_borrar before delete on public.posts for each row execute function public.avisar_reportes_al_borrar();
drop trigger if exists avisar_reportes_al_borrar on public.comments;
create trigger avisar_reportes_al_borrar before delete on public.comments for each row execute function public.avisar_reportes_al_borrar();
