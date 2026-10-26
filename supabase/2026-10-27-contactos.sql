-- =====================================================================
-- SuRoute · Encontrar a tus contactos
-- Pegar completo en Supabase › SQL Editor y ejecutar. Es idempotente: se puede volver a correr.
--
-- Privacidad:
--  · Nadie aparece si no lo activa ("Que mis contactos me encuentren").
--  · Nunca se guardan teléfonos ni correos de la agenda: el teléfono del usuario calcula
--    huellas SHA-256 ("suroute|tel|3001234567", "suroute|correo|ana@x.com") y solo envía esas huellas.
--  · La agenda no se guarda: la consulta compara y responde quiénes ya están en SuRoute.
-- =====================================================================

create extension if not exists pgcrypto with schema extensions;

create table if not exists public.encontrables (
  user_id     uuid primary key references public.profiles(id) on delete cascade,
  tel_hash    text,
  correo_hash text,
  actualizado timestamptz not null default now()
);
create index if not exists encontrables_tel on public.encontrables (tel_hash);
create index if not exists encontrables_correo on public.encontrables (correo_hash);
-- Sin políticas: solo se toca a través de las funciones de abajo.
alter table public.encontrables enable row level security;

-- Activa o desactiva que mis contactos me encuentren. El correo se toma de la cuenta; el teléfono es opcional.
create or replace function public.hacerme_encontrable(activo boolean, tel_hash text default null) returns void
language plpgsql security definer set search_path = public, extensions as $$
declare yo uuid := auth.uid(); correo text;
begin
  if yo is null then raise exception 'Necesitas una cuenta'; end if;
  if not activo then delete from public.encontrables where user_id = yo; return; end if;
  select lower(trim(email)) into correo from auth.users where id = yo;
  insert into public.encontrables (user_id, tel_hash, correo_hash)
  values (yo, nullif(trim(tel_hash), ''), case when correo is null then null else encode(digest('suroute|correo|' || correo, 'sha256'), 'hex') end)
  on conflict (user_id) do update set tel_hash = excluded.tel_hash, correo_hash = excluded.correo_hash, actualizado = now();
end $$;

create or replace function public.soy_encontrable() returns boolean
language sql security definer stable set search_path = public as $$
  select exists (select 1 from public.encontrables where user_id = auth.uid());
$$;

-- Recibe huellas de la agenda (máximo 3000) y devuelve quiénes ya están en SuRoute,
-- sin incluirme a mí ni a personas bloqueadas en cualquier dirección.
create or replace function public.encontrar_contactos(hashes text[])
returns table (hash text, id uuid, username text, nombre text, avatar_url text, cumbre text)
language sql security definer stable set search_path = public as $$
  with h as (select distinct x from unnest(hashes[1:3000]) as x where x is not null and length(x) = 64)
  select distinct on (p.id) h.x, p.id, p.username, p.nombre, p.avatar_url, p.cumbre
  from h
  join public.encontrables e on e.tel_hash = h.x or e.correo_hash = h.x
  join public.profiles p on p.id = e.user_id
  where auth.uid() is not null and p.id <> auth.uid()
    and not exists (select 1 from public.bloqueos b where (b.quien = auth.uid() and b.a_quien = p.id) or (b.quien = p.id and b.a_quien = auth.uid()))
  order by p.id;
$$;

revoke all on function public.hacerme_encontrable(boolean, text) from anon;
revoke all on function public.encontrar_contactos(text[]) from anon;
grant execute on function public.hacerme_encontrable(boolean, text) to authenticated;
grant execute on function public.soy_encontrable() to authenticated;
grant execute on function public.encontrar_contactos(text[]) to authenticated;
