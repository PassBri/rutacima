-- Ascenso financiero: aprobar la web con el teléfono por QR.
-- La web muestra un QR con un id y su llave pública (ECDH P-256). El teléfono, tras la huella, deja aquí
-- un mensaje cifrado solo para esa web (la llave de datos del cifrado de extremo a extremo, o un "ok").
-- Nadie más puede leerlo: ni la base de datos ni un administrador. Se borra al usarse y vence en 5 minutos.
create table if not exists public.fin_aprobaciones (
  id      text primary key check (char_length(id) between 16 and 64),
  user_id uuid not null default public.yo(),
  datos   text not null check (char_length(datos) < 2000),
  creado  timestamptz not null default now()
);
alter table public.fin_aprobaciones enable row level security;
drop policy if exists "aprobar mis finanzas" on public.fin_aprobaciones;
create policy "aprobar mis finanzas" on public.fin_aprobaciones for insert
  with check (user_id = auth.uid() and not coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false));
drop policy if exists "leer mis aprobaciones" on public.fin_aprobaciones;
create policy "leer mis aprobaciones" on public.fin_aprobaciones for select
  using (public.es_mi_ruta(user_id) and creado > now() - interval '5 minutes');
drop policy if exists "borrar mis aprobaciones" on public.fin_aprobaciones;
create policy "borrar mis aprobaciones" on public.fin_aprobaciones for delete using (public.es_mi_ruta(user_id));
grant select, insert, delete on public.fin_aprobaciones to authenticated;
