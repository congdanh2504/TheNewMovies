# Supabase Authentication — Design

Date: 2026-08-22

## Purpose

Put the app behind an account. Today anyone who installs TheNewMovies browses TMDB and keeps a
watchlist that belongs to the device. This adds email/password sign-up and sign-in through
Supabase, gates the entire app on a session, and moves the watchlist into Postgres so it follows
the user to a new install.

Auth is additive to the existing architecture. `core:network`, `core:database`, and the four
existing features keep their current shape; the watchlist is the only feature whose data layer
changes.

## Decisions

| Question | Decision |
| --- | --- |
| Gating | Whole app. Nothing is reachable without a session |
| Methods | Email + password only. Google sign-in explicitly excluded |
| Password reset | 6-digit recovery code typed in the app; no deep link, no hosted page |
| Email confirmation | Disabled in the Supabase dashboard, so sign-up yields a live session |
| Watchlist storage | Postgres table with row-level security; Room stays the read cache |
| Offline writes | Dirty flag on the Room row plus replay on next sign-in; no WorkManager |
| Module shape | `core:supabase` transport, repositories in `core:data`, `feature:auth:api`/`impl` |
| Session gating | Two separate back stacks in `:app`, selected by session state |

Excluded deliberately:

- **Google sign-in.** Removed during design. It needs a Google Cloud OAuth client, per-keystore
  SHA-1 registration, and console configuration that buys nothing while a second sign-in method
  is not required.
- **Magic links and deep links.** Both recovery and confirmation stay in-app as typed codes, so
  no intent filter, no redirect-URL allowlist, and no hosted landing page.
- **WorkManager sync.** A dirty flag replayed at sign-in covers the one screen that holds user
  data. A background worker can be added later without changing the repository contract.
- **Anonymous sessions and account linking.** Gating the whole app removes the need for them.
- **Profile screen.** Sign-out lives in the Watch List top bar. A fifth feature module and a
  fourth bottom-bar item would exist only to hold one button.

## Modules

```
core:supabase   (new)  SupabaseClient + Hilt module. Transport only, mirrors core:network
core:model      (edit) AuthUser, SessionState, AuthResult, AuthError
core:data       (edit) AuthRepository + internal SupabaseAuthRepository;
                       WatchlistRepository gains user scoping and sync
core:database   (edit) WatchlistEntity gains userId / pendingSync / deleted; DAO queries scoped
core:designsystem (edit) MoviesTextField
core:testing    (edit) TestAuthRepository
feature:auth:api   (new) LoginKey, SignUpKey, ForgotPasswordKey, ResetPasswordKey
feature:auth:impl  (new) four screens, four ViewModels, entry functions
app             (edit) session-driven branch between the auth and main back stacks;
                       gains a core:data dependency for AuthRepository, as :app does in
                       Now in Android
```

`core:supabase` holds the client and nothing else, the same way `core:network` holds Retrofit and
nothing else. `core:data` is its only consumer, so no feature module ever imports a Supabase type.
Auth and the watchlist rows share one client.

Dependencies: the supabase-kt BOM (latest stable is `3.1.4`; pin it in the version catalog and
confirm the `Auth` and `Postgrest` plugin API against that release before writing code), the
`auth-kt` and `postgrest-kt` modules, a Ktor engine (`ktor-client-okhttp`, version matched to the
one supabase-kt is built against), and kotlinx-serialization, which `:app` already applies.

## Configuration

`core:supabase/build.gradle.kts` reads two values from `local.properties` into `BuildConfig`,
using the same fail-loud `require()` as `core:network` does for `TMDB_API_KEY`:

```properties
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=<anon key>
```

The anon key is public by design — row-level security, not key secrecy, is what protects rows —
but it stays out of git alongside the TMDB token.

Dashboard work, which cannot be done from the repository:

1. Authentication → Sign In / Providers → **uncheck Confirm email**.
2. Authentication → Email Templates → **Reset Password**: replace the `{{ .ConfirmationURL }}`
   link with `{{ .Token }}` so the recovery mail carries a 6-digit code.
3. SQL editor → run the `watchlist` table and policy from *Watchlist storage* below.

## Auth data layer

`core:model`:

```kotlin
data class AuthUser(val id: String, val email: String)

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: AuthUser) : SessionState
}

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

enum class AuthError {
    InvalidCredentials, EmailAlreadyRegistered, WeakPassword,
    InvalidCode, Network, Unknown,
}
```

`core:data`, public interface with an `internal` implementation bound by `@Binds`, matching every
other repository in the project:

```kotlin
interface AuthRepository {
    val sessionState: Flow<SessionState>
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun sendPasswordReset(email: String): AuthResult
    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResult
    suspend fun signOut()
}
```

`sessionState` maps supabase-kt's `sessionStatus` flow: `LoadingFromStorage` → `Loading`,
`Authenticated` → `SignedIn`, everything else → `SignedOut`. Session persistence needs no code —
supabase-kt stores it through multiplatform-settings, which resolves to SharedPreferences on
Android.

`resetPassword` performs `verifyEmailOtp(type = RECOVERY, …)` followed by `updateUser`, so the
screen makes one call.

The implementation catches `AuthRestException` (and `RestException` for the Postgrest path) and
maps error codes onto `AuthError`; anything unrecognised becomes `Unknown`, and IO failures become
`Network`. Nothing else in the app sees a Supabase exception type.

This reverses the original spec's "no generic `Result` in `core:common`" decision, and only for
auth. A repository refresh returning `Boolean` was right because the UI reaction is identical
whatever failed. A login screen must distinguish a wrong password from an unreachable server, so
the typed result earns its place. `AuthError` lives in `core:model` next to the other shared
types rather than in `core:common`.

## Auth UI

`feature:auth:api` exposes four `NavKey`s and their navigate extensions. `feature:auth:impl` holds
the screens; `ResetPasswordKey` carries the email address so the code screen can submit it.

| Key | Screen | Fields | Calls |
| --- | --- | --- | --- |
| `LoginKey` | Sign in | email, password | `signIn` |
| `SignUpKey` | Create account | email, password, confirm | `signUp` |
| `ForgotPasswordKey` | Forgot password | email | `sendPasswordReset` |
| `ResetPasswordKey(email)` | New password | 6-digit code, new password | `resetPassword` |

Each ViewModel owns one `AuthUiState(email, password, …, isSubmitting, error: AuthError?)`. Form
state is hoisted into the ViewModel so the screens stay stateless and previewable, matching the
existing screens. Validation also lives in the ViewModel: a non-blank email containing `@`, a
password of at least six characters (Supabase's own floor), and matching confirmation. Invalid
input never reaches the network.

`core:designsystem` gains `MoviesTextField`: labelled, error-aware, with an optional password
visibility toggle. Three screens need it and `MoviesSearchBar` is search-shaped, so bending it
would serve neither. No button wrapper is added — Material 3 `Button` under `MoviesTheme` is
already correct.

## Session gating

`MoviesApp` gains a small `AppViewModel` exposing `sessionState`, and branches on it:

| State | Renders |
| --- | --- |
| `Loading` | Themed full-screen box with a centred progress indicator |
| `SignedOut` | Auth `NavDisplay` over its own `rememberNavBackStack(LoginKey)` |
| `SignedIn` | The existing tabbed `NavDisplay`, unchanged |

Two independent back stacks mean sign-out cannot be back-navigated around, and `NavigationState`
needs no change at all — auth keys never enter a tab sub-stack. The main stack is destroyed on
sign-out, so no stale movie screen can restore behind a login form.

The `Loading` state exists to prevent a login flash on cold start: the stored session resolves at
the speed of a SharedPreferences read, which is fast but not synchronous with the first frame.
That is also why no `core-splashscreen` dependency is needed.

Screens keep applying their own `statusBarsPadding()`, including the auth screens — the Scaffold
in `:app` zeroes its content insets.

Sign-out is an icon button in the Watch List `MoviesTopAppBar`. It calls `signOut()` and lets the
session flow swap the stack; the screen does not navigate.

## Watchlist storage

```sql
create table public.watchlist (
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

create policy "own rows" on public.watchlist
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
```

One `for all` policy covers select, insert, update, and delete. Isolation is enforced by Postgres
rather than by client queries remembering to filter.

`WatchlistEntity` gains three columns:

- `userId: String` — joins the primary key, so it becomes `(userId, movieId)`
- `pendingSync: Boolean` — this row has a local change not yet accepted by Postgres
- `deleted: Boolean` — soft delete, because an offline un-bookmark must stay replayable; a hard
  delete would leave nothing to push

Every DAO read filters `userId = :userId AND deleted = 0`.

The schema version bumps with a **destructive migration**. The app is unreleased and every
existing watchlist row predates user accounts, so a real migration would carry data that belongs
to nobody.

`WatchlistRepository` keeps its six existing methods unchanged, so the Detail and Watch List
screens are untouched, and gains one: `suspend fun syncWatchlist()`. The implementation resolves
the current user from `AuthRepository.sessionState` and writes through.

```
toggle / rate → Room upsert with pendingSync = true      (UI updates immediately)
              → push the row to Postgrest
              → success: clear pendingSync
                failure: leave the flag set and return
```

`syncWatchlist()` is called on the `SignedOut → SignedIn` transition, observed by `AppViewModel`
in `:app`. It pushes every
pending row (upsert, or delete where `deleted = 1`), then pulls all remote rows for the user and
replaces the local non-pending ones. Local pending changes win, because they are the newer intent.
`updated_at` is stored so a field-level merge remains possible later without a schema change.

Sign-out leaves Room rows in place. They are `userId`-scoped, so a second account on the same
device cannot read them, and anything still pending survives to sync at the next sign-in.

## Testing

`core:testing` gains `TestAuthRepository`: a settable `sessionState` plus per-method failure
injection. Every feature module already receives `core:testing` on its test classpath through
`AndroidFeatureImplConventionPlugin`.

- **Unit — auth ViewModels:** validation rejects bad input without calling the repository,
  `isSubmitting` toggles around the call, each `AuthError` reaches the UI state, success clears
  the error.
- **Unit — watchlist sync:** a pending row is pushed and its flag cleared; a failed push leaves
  the flag set; the pull replaces non-pending rows and preserves pending ones; a soft-deleted row
  is pushed as a delete.
- **Unit — mapping:** `WatchlistEntity` to and from the Postgres row DTO, both directions.
- **Instrumented DAO:** two users' rows are isolated, `deleted` rows are excluded from reads.
- **Compose UI:** the login screen renders an error message and disables the button while
  submitting.

Known gap, stated rather than papered over: `SupabaseAuthRepository` cannot be unit-tested without
a live server, so it stays a thin mapping shim and is verified by hand on a device — sign up, sign
in, wrong password, reset by code, sign out, offline bookmark then reconnect. The Moshi/R8 episode
recorded in `docs/superpowers/plans/README.md` is the precedent: a green suite proved nothing
about the real call path.

## Build order

Three slices, each ending with an installable app:

1. **Transport and repository.** `core:supabase`, `core:model` additions, `AuthRepository` and its
   implementation, `TestAuthRepository`, unit tests. No UI; the app is unchanged.
2. **Auth UI and gating.** `MoviesTextField`, `feature:auth:api`/`impl`, the four screens, the
   `:app` session branch, sign-out in the Watch List bar. The app is now behind a login.
3. **Watchlist sync.** Postgres table and policy, the Room schema change, user scoping,
   write-through with the dirty flag, sign-in sync, tests.
