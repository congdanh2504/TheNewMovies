# TheNewMovies

An Android movie browser powered by the [TMDB API](https://www.themoviedb.org/documentation/api),
structured after [Now in Android](https://github.com/android/nowinandroid): convention plugins,
`api`/`impl` feature modules, an offline-first data layer, and Navigation 3.

## Features

- **Home** — featured row, four category tabs, poster grid; remembers the selected tab
- **Search** — debounced, paged TMDB search
- **Detail** — backdrop, poster, About/Reviews/Cast tabs, watchlist toggle, rating sheet
- **Watchlist** — Room-persisted saved movies with user ratings and an empty state

Every screen reads from Room, so the app works offline with whatever it last synced.

## Architecture

```
app ──> feature:*:impl ──> feature:*:api ──> core:navigation
             │
             └──> core:{data, domain, ui, designsystem, model, common}

core:data ──> core:{network, database, datastore, model, common}
```

- **`core:model`** — pure-Kotlin domain models
- **`core:common`** — injected dispatcher qualifiers
- **`core:designsystem`** — theme, typography, themed primitives; knows no app model
- **`core:navigation`** — `NavigationState` (a back stack per tab) and `Navigator`
- **`core:network`** — TMDB DTOs and the Retrofit data source
- **`core:database`** — Room entities, DAOs, and entity-to-model mapping
- **`core:datastore`** — the selected home tab
- **`core:data`** — repository interfaces, `internal` offline-first implementations, mapping
- **`core:domain`** — `GetMovieDetailUseCase`, the one place several repositories are combined
- **`core:ui`** — composites shared by two or more features
- **`core:testing`** — fakes and the main-dispatcher rule
- **`feature:<name>:api`** — one `NavKey` plus a navigate extension
- **`feature:<name>:impl`** — ViewModel, screen, entry function

A feature reaches another feature only through its `api` module, so features compile in parallel
and `:app` is the only module that knows the whole graph. `:app` contains exactly one composable
file — it composes entries, it does not hold screens.

### Data flow

```
NetworkMovie ──asEntity()──> MovieEntity ──asExternalModel()──> Movie
 (core:network)              (core:database)                   (core:model)
```

Reads come from Room only. The network is touched by `refresh(category)` and
`refreshDetail(id)`, which no-op inside a 24-hour TTL, plus search paging (not persisted).
`MovieEntity` is keyed on `(id, category)`, so the same movie can appear in Popular and Now
Playing at once.

### Navigation

`NavigationState` holds one back stack per top-level destination, so switching tabs preserves
each tab's history and re-selecting the current tab clears it. Stacks come from
`rememberNavBackStack`, so they survive process death. `NavDisplay` only dispatches back when its
own stack has more than one entry, so `:app` adds a `BackHandler` for the tab-root case — back
from a non-first tab returns to the first tab instead of leaving the app.

## Tech stack

| Layer | Technology |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation 3 (`NavDisplay`, `entryProvider`, `NavKey`) |
| DI | Hilt |
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
2. Add it to `local.properties`:

```properties
TMDB_API_KEY=your_token_here
```

The build fails with a readable message if the key is missing.

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

- **Unit tests** cover every ViewModel (Turbine), the offline-first repository including its TTL
  behaviour, the watchlist repository, both mapping directions, and `Navigator`'s tab semantics.
- **Instrumented tests** cover `MovieDao` and `WatchlistDao` against an in-memory database.
- **Compose UI tests** cover the search results list. Search results never touch Room, so they
  are rendered over fake `PagingData` rather than seeded data.
- Fakes live in `core:testing` as a real module, so any feature can consume them with
  `testImplementation(projects.core.testing)` — already declared by the feature-impl plugin.

## Docs

- Design spec: `docs/superpowers/specs/2026-08-21-thenewmovies-design.md`
- Implementation plans and the fixes found while executing them: `docs/superpowers/plans/`
