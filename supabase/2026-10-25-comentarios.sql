-- Comentarios con respuestas y banderines de ánimo (como "me gusta" en los comentarios).
-- Pegar en Supabase › SQL Editor y ejecutar una vez. Es idempotente: se puede volver a correr.
alter table public.comments add column if not exists parent_id uuid references public.comments(id) on delete cascade;
create index if not exists comments_padre on public.comments (parent_id);

create table if not exists public.comment_likes (
  comment_id uuid not null references public.comments(id) on delete cascade,
  user_id    uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (comment_id, user_id)
);
alter table public.comment_likes enable row level security;
drop policy if exists "ver banderines" on public.comment_likes;
create policy "ver banderines" on public.comment_likes for select using (true);
drop policy if exists "dar banderin" on public.comment_likes;
create policy "dar banderin" on public.comment_likes for insert with check (user_id = public.yo());
drop policy if exists "quitar banderin" on public.comment_likes;
create policy "quitar banderin" on public.comment_likes for delete using (user_id = public.yo());
