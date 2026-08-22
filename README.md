# TheNewMovies

An Android movie browser powered by the [TMDB API](https://www.themoviedb.org/documentation/api),
structured after [Now in Android](https://github.com/android/nowinandroid): convention plugins,
`api`/`impl` feature modules, an offline-first data layer, and Navigation 3. Accounts and the
watchlist are backed by [Supabase](https://supabase.com).

## Features

- **Accounts** — email/password sign-up and sign-in, password recovery by a 6-digit emailed code.
  The whole app sits behind a session.
- **Home** — featured row, four category tabs, poster grid; remembers the selected tab
- **Search** — debounced, paged TMDB search
- **Detail** — backdrop, poster, About/Reviews/Cast tabs, watchlist toggle, rating sheet
- **Watchlist** — saved movies with user ratings, synced to Postgres so they follow the account to
  a new install

Every screen reads from Room, so the app works offline with whatever it last synced. A bookmark or
rating made offline is written locally at once and pushed when a session next begins.

## Architecture

```
app ──> feature:*:impl ──> feature:*:api ──> core:navigation
             │
             ├──> core:{data, domain, ui, designsystem, model, common}
             │
app ──> core:data (for AuthRepository, to gate on the session)

core:data ──> core:{network, database, datastore, supabase, model, common}
```

- **`core:model`** — pure-Kotlin domain models
- **`core:common`** — injected dispatcher qualifiers
- **`core:designsystem`** — theme, typography, themed primitives; knows no app model
- **`core:navigation`** — `NavigationState` (a back stack per tab) and `Navigator`
- **`core:network`** — TMDB DTOs and the Retrofit data source
- **`core:supabase`** — the `SupabaseClient` and its Hilt module, nothing else
- **`core:database`** — Room entities, DAOs, and entity-to-model mapping
- **`core:datastore`** — the selected home tab
- **`core:data`** — repository interfaces, `internal` implementations, mapping
- **`core:domain`** — `GetMovieDetailUseCase`, the one place several repositories are combined
- **`core:ui`** — composites shared by two or more features
- **`core:testing`** — fakes and the main-dispatcher rule
- **`feature:<name>:api`** — one `NavKey` plus a navigate extension
- **`feature:<name>:impl`** — ViewModel, screen, entry function

A feature reaches another feature only through its `api` module, so features compile in parallel
and `:app` is the only module that knows the whole graph. `core:data` is the only module that sees
a Supabase type — nothing under `feature/` or `app/` imports `io.github.jan.supabase`.

### Data flow

```
NetworkMovie ──asEntity()──> MovieEntity ──asExternalModel()──> Movie
 (core:network)              (core:database)                   (core:model)

WatchlistRow <──asRow()/asEntity()──> WatchlistEntity ──asExternalModel()──> WatchlistMovie
 (Postgres)                            (core:database)                       (core:model)
```

Reads come from Room only. The network is touched by `refresh(category)` and `refreshDetail(id)`,
which no-op inside a 24-hour TTL, plus search paging (not persisted). `MovieEntity` is keyed on
`(id, category)`, so the same movie can appear in Popular and Now Playing at once.

The watchlist writes through: a bookmark or rating lands in Room first, flagged `pendingSync`, then
pushes to Postgres, and the flag clears once the server accepts it. An un-bookmark is a **soft
delete** so it stays replayable offline. `syncWatchlist()` runs when a session begins — it pushes
everything pending, then pulls the server's rows without clobbering anything still pending.
`WatchlistEntity` is keyed on `(userId, movieId)`, so two accounts on one device stay separate.

### Navigation

`NavigationState` holds one back stack per top-level destination, so switching tabs preserves each
tab's history and re-selecting the current tab clears it. Stacks come from `rememberNavBackStack`,
so they survive process death. `NavDisplay` only dispatches back when its own stack has more than
one entry, so `:app` adds a `BackHandler` for the tab-root case — back from a non-first tab returns
to the first tab instead of leaving the app.

Auth runs on a **second, independent back stack**. `MoviesApp` branches on the session: a spinner
while the stored session is read, the auth stack when signed out, the tabbed app when signed in.
Signing out destroys the whole signed-in composition, so it cannot be back-navigated around, and
`core:navigation` needs no knowledge of auth at all.

## Tech stack

| Layer | Technology |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation 3 (`NavDisplay`, `entryProvider`, `NavKey`) |
| DI | Hilt |
| Auth & sync | Supabase (`auth-kt`, `postgrest-kt`) over Ktor |
| Network | Retrofit + OkHttp + Moshi |
| Persistence | Room, Preferences DataStore |
| Paging | Paging 3 |
| Images | Coil |
| Async | Coroutines + Flow |
| Build | Gradle convention plugins in an included build |
| Format | Spotless + ktlint |

## Getting started

Requires JDK 17+ and Android Studio with AGP 8.10 support.

1. Get a TMDB **v4 read access token** from https://www.themoviedb.org/settings/api
2. Create a Supabase project and copy its URL and **anon** key from Project Settings → API
3. Put all three in `local.properties`:

```properties
TMDB_API_KEY=your_token_here
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your_anon_key_here
```

The build fails with a readable message if any of them is missing. The anon key is public by
design — row-level security, not key secrecy, is what protects rows.

Then configure the Supabase project itself, which the build cannot do for you:

4. **Authentication → Sign In / Providers → uncheck "Confirm email".** With it on, sign-up returns
   a user but no session, so the create-account screen looks like it silently does nothing.
5. **Authentication → Email Templates → Reset Password** — replace the `{{ .ConfirmationURL }}`
   link with `{{ .Token }}`, so recovery mail carries the 6-digit code the app asks for. There is
   no deep link anywhere in this design.
6. **SQL Editor** — run [`docs/supabase/watchlist.sql`](docs/supabase/watchlist.sql) to create the
   `watchlist` table and its row-level-security policy. Check the Table Editor shows the RLS badge
   afterwards; without the policy every user could read every other user's rows.

> Some networks block `api.themoviedb.org` by DNS (resolving it to `127.0.0.1`) and by resetting
> TLS on the SNI hostname. If Home shows "Couldn't load movies", check with
> `dig api.themoviedb.org` before suspecting the app — `image.tmdb.org` is usually not blocked,
> so posters can load while the API does not.

## Commands

```bash
./gradlew assembleDebug                      # build
./gradlew installDebug                       # build and install
./gradlew spotlessApply                      # format
./gradlew testDebugUnitTest                  # unit tests
./gradlew connectedDebugAndroidTest          # DAO + Compose UI tests (needs a device)
./gradlew build                              # everything, including spotlessCheck and lint
```

## Testing

106 unit tests across 18 classes, plus 23 instrumented tests (13 in `core:database`, 6 in
`feature:search:impl`, 4 in `feature:auth:impl`).

- **Unit tests** cover every ViewModel (Turbine), the offline-first repository including its TTL
  behaviour, the watchlist repository's write-through and sync paths, session-state and
  auth-error mapping, both mapping directions, form validation, and `Navigator`'s tab semantics.
- **Instrumented tests** cover `MovieDao` and `WatchlistDao` against an in-memory database,
  including that two accounts' rows stay isolated and soft-deleted rows are hidden from reads.
- **Compose UI tests** cover the search results list and the sign-in screen. Search results never
  touch Room, so they are rendered over fake `PagingData` rather than seeded data.
- Fakes live in `core:testing` as a real module, so any feature can consume them with
  `testImplementation(projects.core.testing)` — already declared by the feature-impl plugin.

`SupabaseAuthRepository` and `WatchlistRemoteDataSource` have no unit tests on purpose: they cannot
run without a live project, so they are kept as thin shims and verified by hand against a real
Supabase instance. That pass is worth doing — it caught a sync bug that all the unit tests passed
over, recorded in the plans below.

## Docs

- Design specs: `docs/superpowers/specs/`
- Implementation plans, and the fixes and wrong assumptions found while executing them:
  `docs/superpowers/plans/`
- Supabase schema: `docs/supabase/watchlist.sql`
