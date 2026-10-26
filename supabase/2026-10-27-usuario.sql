-- =====================================================================
-- SuRoute · @usuario único y personal
-- Pegar completo en Supabase › SQL Editor y ejecutar. Es idempotente.
--  · Siempre en minúsculas, de 3 a 30 letras, números, punto o guion bajo (único: ya lo exige profiles.username).
--  · Nunca el correo de la cuenta (toda la comunidad lo vería).
--  · Al registrarse, si llega un usuario no válido, se usa senderista_xxxx y la app pide elegir uno.
-- =====================================================================

create or replace function public.validar_usuario() returns trigger
language plpgsql security definer set search_path = public as $$
declare correo text; local text;
begin
  new.username := lower(trim(new.username));
  select lower(email) into correo from auth.users where id = new.id;
  local := regexp_replace(split_part(coalesce(correo, ''), '@', 1), '[^a-z0-9._]', '', 'g');
  if new.username !~ '^[a-z0-9._]{3,30}$'
     or new.username ~ '(gmail|hotmail|outlook|yahoo|icloud|live)(\.|$)' or new.username ~ '\.(com|net|org|es|edu|co)$'
     or (correo is not null and new.username = regexp_replace(correo, '[^a-z0-9._]', '', 'g')) then
    if tg_op = 'INSERT' then new.username := 'senderista_' || substr(new.id::text, 1, 8); return new; end if;
    if tg_op = 'UPDATE' and new.username = old.username then return new; end if;
    raise exception 'usuario_no_valido' using errcode = '22023',
      hint = 'De 3 a 30 letras minúsculas, números, punto o guion bajo, y que no sea tu correo.';
  end if;
  return new;
end $$;

drop trigger if exists al_validar_usuario on public.profiles;
create trigger al_validar_usuario before insert or update of username on public.profiles
  for each row execute function public.validar_usuario();

-- ¿Está libre este @usuario? (para avisar mientras se escribe)
create or replace function public.usuario_disponible(u text) returns boolean
language sql security definer stable set search_path = public as $$
  select lower(trim(u)) ~ '^[a-z0-9._]{3,30}$'
     and not exists (select 1 from public.profiles where username = lower(trim(u)) and id <> coalesce(auth.uid(), '00000000-0000-0000-0000-000000000000'::uuid));
$$;
grant execute on function public.usuario_disponible(text) to anon, authenticated;

-- Cambiar mi @usuario (lo valida el disparador de arriba; si ya existe, falla por la restricción única)
create or replace function public.cambiar_usuario(u text) returns text
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'Necesitas una cuenta'; end if;
  update public.profiles set username = lower(trim(u)) where id = auth.uid();
  return lower(trim(u));
end $$;
grant execute on function public.cambiar_usuario(text) to authenticated;
