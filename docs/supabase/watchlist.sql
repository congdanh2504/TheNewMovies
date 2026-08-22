-- Watchlist rows, one per (user, movie). Run in the Supabase SQL editor.
--
-- Room remains the app's read cache; this table is what makes a watchlist follow a user to a new
-- install. Row-level security is not optional here: the anon key ships in the app and is public
-- by design, so the policy below is the only thing standing between one user's rows and another's.
create table if not exists public.watchlist (
  user_id uuid not null references auth.users on delete cascade,
  movie_id int not null,
  title text not null,
  poster_path text,
  backdrop_path text,
  release_date text not null,
  vote_average double precision not null,
  runtime int not null,
  genre text not null,
  user_rating real,
  updated_at timestamptz not null default now(),
  primary key (user_id, movie_id)
);

alter table public.watchlist enable row level security;

-- One policy for select, insert, update and delete: a user reaches only their own rows.
create policy "own rows" on public.watchlist
  for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
