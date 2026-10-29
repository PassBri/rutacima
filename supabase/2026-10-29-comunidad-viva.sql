-- Comunidad viva: lo que hace a SuRoute distinta de TikTok, Instagram y Facebook.
-- Correr una vez en Supabase (SQL Editor). Mientras no se corra, la web y la app siguen
-- funcionando como antes: estas ideas simplemente no se guardan en la nube.
--
-- 1. Prueba de camino: cada publicación lleva el camino que hay detrás (racha de días y pasos ganados).
alter table public.posts add column if not exists camino jsonb;
do $$ begin
  alter table public.posts add constraint posts_camino_tamano check (camino is null or pg_column_size(camino) < 2000);
exception when duplicate_object then null; end $$;

-- 6. Compromisos con fecha: {"hasta": "2026-11-30", "estado": "ABIERTO" | "LOGRADO" | "REPLANTEO"}.
alter table public.posts add column if not exists compromiso jsonb;
do $$ begin
  alter table public.posts add constraint posts_compromiso_tamano check (compromiso is null or pg_column_size(compromiso) < 1000);
exception when duplicate_object then null; end $$;

-- 8. Carta al yo futuro: {"abre": "2027-10-29"}. Hasta esa fecha se ve el sello, no el texto.
alter table public.posts add column if not exists sello jsonb;
do $$ begin
  alter table public.posts add constraint posts_sello_tamano check (sello is null or pg_column_size(sello) < 500);
exception when duplicate_object then null; end $$;

-- Cerrar un compromiso usa la política que ya existe: "editar mis publicaciones" (solo el autor).

-- 3. Lanzar una cuerda: un comentario que cuesta algo (una pregunta, un consejo o "yo también lo intento").
alter table public.comments add column if not exists cuerda text;
do $$ begin
  alter table public.comments add constraint comments_cuerda_tipo check (cuerda is null or cuerda in ('PREGUNTA', 'CONSEJO', 'YO_TAMBIEN'));
exception when duplicate_object then null; end $$;

-- 5. La expedición de la comunidad suma también los pasos reales: cada día marcado en una cordada y en un reto.
create or replace function public.expedicion(p_anio int default extract(year from now())::int)
returns table (semana int, puntos bigint)
language sql stable security definer set search_path = public as $$
  with aportes as (
    select p.created_at as cuando, 10 as pts from public.posts p
      where p.visibilidad <> 'PRIVADA' and not p.oculto
        and p.created_at >= make_date(p_anio, 1, 1) and p.created_at < make_date(p_anio + 1, 1, 1)
    union all
    select v.created_at, 1 from public.votes v
      where v.created_at >= make_date(p_anio, 1, 1) and v.created_at < make_date(p_anio + 1, 1, 1)
    union all
    select c.dia::timestamptz, 5 from public.cordada_checkins c
      where c.dia >= make_date(p_anio, 1, 1) and c.dia < make_date(p_anio + 1, 1, 1)
    union all
    select r.dia::timestamptz, 5 from public.reto_marcas r
      where r.dia >= make_date(p_anio, 1, 1) and r.dia < make_date(p_anio + 1, 1, 1)
  )
  select least((extract(doy from cuando)::int - 1) / 7, 51) as semana, sum(pts)::bigint as puntos
  from aportes group by 1 order by 1
$$;
grant execute on function public.expedicion(int) to anon, authenticated;
