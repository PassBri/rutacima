-- =====================================================================
-- SuRoute · Código de cordada para vincular la web
-- Pegar completo en Supabase › SQL Editor y ejecutar. Es idempotente.
--  · El código sigue siendo de 8 caracteres (el QR y la app no cambian), pero ahora empieza por SUR
--    y los 5 restantes guardan dos palabras de montaña y un número: «cóndor · nevado · 42».
--    La web y la app convierten entre el código y las palabras con la misma lista de 128 palabras.
--  · Combinaciones: 128 × 128 × 90 ≈ 1,5 millones; cada código vive un minuto.
-- =====================================================================
create or replace function public.crear_vinculo(nombre text default 'Navegador') returns text
language plpgsql security definer set search_path = public, extensions as $$
declare
  alfabeto constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  c text; v bigint;
begin
  if auth.uid() is null then raise exception 'Sin sesión'; end if;
  delete from public.vinculos where web_uid = auth.uid() or expira < now();
  loop
    -- palabra 1 (0–127), palabra 2 (0–127) y número (10–99)
    v := ((get_byte(gen_random_bytes(1), 0) % 128) * 128 + (get_byte(gen_random_bytes(1), 0) % 128)) * 90
         + ((get_byte(gen_random_bytes(1), 0) * 256 + get_byte(gen_random_bytes(1), 0)) % 90);
    c := '';
    for i in 1..5 loop
      c := substr(alfabeto, 1 + (v % 32)::int, 1) || c;
      v := v / 32;
    end loop;
    c := 'SUR' || c;
    exit when not exists (select 1 from public.vinculos x where x.codigo = c);
  end loop;
  insert into public.vinculos (codigo, web_uid, nombre) values (c, auth.uid(), left(coalesce(nombre, 'Navegador'), 60));
  return c;
end $$;
