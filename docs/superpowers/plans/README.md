# TheNewMovies Implementation Plans

Execute in order. Each slice ends with a compiling, installable app, so work can stop between
slices without leaving the repo broken.

| Plan | Slices | Delivers |
| --- | --- | --- |
| [2026-08-21-slice-0-foundation.md](2026-08-21-slice-0-foundation.md) | 0 | Gradle wrapper, version catalog, `build-logic` convention plugins, Spotless, `core:model`, `core:common` |
| [2026-08-21-slice-1-app-shell.md](2026-08-21-slice-1-app-shell.md) | 1 | `core:designsystem`, `core:navigation`, four feature `api` modules, `:app` with a working bottom bar and placeholder entries |
| [2026-08-21-slice-2-data-layer.md](2026-08-21-slice-2-data-layer.md) | 2 | `core:network`, `core:database`, `core:datastore`, `core:data`, `core:testing`; offline-first repositories with a sync TTL |
| [2026-08-21-slice-3-4-home-and-detail.md](2026-08-21-slice-3-4-home-and-detail.md) | 3, 4 | `core:ui`, Home feature; `core:domain`, Detail feature |
| [2026-08-21-slice-5-7-search-watchlist-readme.md](2026-08-21-slice-5-7-search-watchlist-readme.md) | 5, 6, 7 | Search feature, Watchlist feature, README, full verification pass |

Spec: [`../specs/2026-08-21-thenewmovies-design.md`](../specs/2026-08-21-thenewmovies-design.md)

Reference implementation to port from: `/Users/danhtruong/android/TheMovies`

## Where the plans diverge from the spec

Each of these came out of writing the plan in detail. All are simplifications; none change what
the app does.

1. **One Compose convention plugin, not two.** The spec lists
   `themovies.android.library.compose` and `themovies.android.application.compose`. A single
   `themovies.android.compose` branches on which AGP plugin is applied and does the same job in
   half the code.
2. **No generic `Result` in `core:common`.** With Room as the single source of truth, no
   repository method returns a `Result` — refresh failures surface as a `Boolean`. The spec's
   `Result` + `asResult()` would have shipped with zero call sites.
3. **`asEntity()` lives in `core:data`, not `core:network`.** The mapping needs to see both the
   DTO and the Room entity; putting it in `core:network` would force that module to depend on
   Room. `asExternalModel()` stays in `core:database` as specified.
4. **Navigation persistence uses `rememberNavBackStack`, not a hand-rolled `SavedStateHandle`.**
   Navigation 3 ships an Android overload that saves and restores a `NavBackStack<NavKey>` by
   reflection over `@Serializable` keys, so per-tab stacks survive process death with no
   serializer registry in `:app`.
5. **The bottom bar lives in `:app`, not `core:designsystem`.** It reads `TopLevelNavItem`,
   which knows every feature's key — that is app-level knowledge, and the design system must not
   depend on features.
6. **`UserPreferencesRepository` is an interface with an `internal` implementation.** The spec
   described a concrete class; the home ViewModel test needs a fake, and this matches the
   interface + `internal` impl + `@Binds` pattern used everywhere else.
7. **Java/Kotlin target 17, no core library desugaring.** Already recorded in the spec.

## Corrections found while executing

- **Slice 0, `core:common`:** the plan first declared `implementation(libs.hilt.android)`. That
  artifact is an AAR and cannot resolve for a pure-JVM module — it fails with
  `No matching variant of androidx.activity:activity:1.5.1`. Fixed to `libs.hilt.core`, which is
  what the blueprint prescribes for JVM modules. Catalog entry `hilt-core` added.
- **Slice 0, `MoviesDispatchersTest`:** a bare `@Dispatcher(...)` on a property lands on the
  Kotlin property, not the Java field, so the reflective lookup returned null. Fixed to
  `@field:Dispatcher(...)`.
- **Slice 0, Room plugin:** the plan hardcoded `compileOnly("androidx.room:room-gradle-plugin:2.7.1")`.
  Replaced with a `room-gradlePlugin` catalog entry so the version is declared once.

## Test counts by slice

Cumulative unit tests after each slice, useful as a smoke check that nothing was skipped:

| After slice | Unit tests | Instrumented |
| --- | --- | --- |
| 0 | 1 | — |
| 1 | 8 | — |
| 2 | 24 | 9 |
| 3 | 31 | 9 |
| 4 | 42 | 9 |
| 5 | 45 | 9 |
| 6 | 49 | 9 |

Counts are approximate — they assume no extra cases were added while implementing.
