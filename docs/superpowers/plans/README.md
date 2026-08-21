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
- **Slice 1, back at a tab root:** the plan's `MoviesApp` relied on `NavDisplay`'s `onBack` to
  reach `Navigator.goBack()`. `NavDisplay` only dispatches back when its own stack has more than
  one entry, so at a tab root back fell through and finished the activity — pressing back on Watch
  List exited the app instead of returning to Home. `NavigatorTest` passed throughout because it
  calls `goBack()` directly; the gap was app-level wiring. Fixed with a `BackHandler` enabled for
  exactly that case (tab root, not the first tab).
- **Slice 0, Room plugin:** the plan hardcoded `compileOnly("androidx.room:room-gradle-plugin:2.7.1")`.
  Replaced with a `room-gradlePlugin` catalog entry so the version is declared once.

- **Slice 2, instrumented tests ran zero tests (twice).** Two separate gaps in the library
  convention plugin, both silent: no `testInstrumentationRunner` was set, so AGP used
  `android.test.InstrumentationTestRunner` and found no JUnit4 tests; and once the runner was
  declared, `androidx.test:runner` was missing from the androidTest classpath, so the runner class
  itself threw `ClassNotFoundException` and the process crashed. Both fixed in
  `AndroidLibraryConventionPlugin`, which fixes every library module at once. Note the first
  failure mode reported `tests="0" failures="0"` — a green-looking zero.
- **Slice 2, `asGenresJson`:** `json.encodeToString(list)` resolved to the two-argument overload
  until `import kotlinx.serialization.encodeToString` was added for the reified extension.
- **Slice 2, `core:data` manifest:** lint failed the build with `MissingPermission` because
  `ConnectivityManager` is called in `core:data` while `ACCESS_NETWORK_STATE` was declared only in
  `:app`. Added a library manifest declaring it.

- **Slice 3, `core:common` had no annotation processor.** `:app` failed with
  `[Dagger/MissingBinding] @Dispatcher(IO) CoroutineDispatcher`. `core:common` declared
  `hilt-core` but ran no KSP, so `DispatchersModule` produced no aggregating metadata and Hilt
  never saw it. Added `ksp(libs.hilt.compiler)` to that module. The blueprint's `nowinandroid.hilt`
  plugin handles JVM modules for exactly this reason; declared per-module here since `core:common`
  is the only JVM module needing Hilt.
- **Slice 3, `MoviesSearchBar` on Home.** The plan wrapped the bar in a `clickable` Box, but an
  enabled `TextField` takes the tap and the parent never fires. Added an `enabled` parameter and
  pass `false` on Home, where the bar is a button.
- **Slice 3, Home spun forever when the API was unreachable.** `HomeUiState` only reported `Error`
  for `!isOnline`, so an online device whose refreshes all fail (blocked network, TMDB down, bad
  key) sat on the spinner indefinitely. Added a `refreshFailed` flow, a `LOAD_FAILED_MESSAGE`
  error state and a Retry button, plus two tests. Found by running against a network that blocks
  TMDB.

- **Slice 4, extended icons missing from features.** `:feature:detail:impl` failed on
  `Unresolved reference 'Bookmark'` etc. `core:ui` keeps `material-icons-extended` as
  `implementation` so it does not leak, yet its `MetaLabel` and `MoviesEmptyState` take
  `ImageVector` parameters — so every feature needs the artifact. Added to
  `AndroidFeatureImplConventionPlugin`.
- **Slice 4, Detail trapped the user on failure.** The plan rendered `DetailToolbar` inside the
  `Success` branch only, so the loading and error states had no back button. Hoisted it out of the
  `when` (bookmark hidden unless loaded) and added Retry to the error state.

- **Slice 5, Spotless failed only under a parallel full build.** `:core:ui:spotlessXml` failed
  with `Could not read path .../build/intermediates/merged_res/.../ic_call_decline_low.xml`, while
  the same task passed in isolation and `core:ui` has no XML sources at all. Ant include patterns
  filter Spotless's *results* but do not prune Gradle's directory walk, so it traversed `build/`
  while resource merging was deleting intermediates. Fixed by targeting an explicit `fileTree`
  that excludes `build/**`.
- **Slice 5, the search test APK crashed before any test ran.**
  `SecurityException: Permission denied (missing INTERNET permission?)` — the standalone test APK
  has no INTERNET permission, and Coil's fetch inside `MovieRow` killed the instrumentation
  process. Added `src/androidTest/AndroidManifest.xml` granting it.
- **Slice 5, `PagingData.empty()` reports refresh = Loading.** An empty-results test asserted the
  empty state but got the spinner, which is correct behaviour. The test now passes explicit
  terminal `LoadStates`, and a separate test pins the fresh-query spinner.

- **Slice 7, the release build could not parse any JSON.** `assembleRelease` succeeded and the
  app launched, but Home showed "Couldn't load movies" while the debug build loaded real data on
  the same network seconds earlier. The DTOs carried `@JsonClass(generateAdapter = true)` while no
  codegen processor was declared, so Moshi used the reflective `KotlinJsonAdapterFactory` and R8
  renamed the fields out from under it. Fixed with `ksp(libs.moshi.kotlin.codegen)`, dropping the
  reflective factory. Two lessons: a green `assembleRelease` proves nothing about runtime, and
  `runCatching` in the repository swallowed the `JsonDataException` so the only symptom was a
  failed refresh.

## Test counts by slice

Cumulative unit tests after each slice, useful as a smoke check that nothing was skipped:

| After slice | Unit tests | Instrumented |
| --- | --- | --- |
| 0 | 1 | — |
| 1 | 8 | — |
| 2 | 47 | 9 |
| 3 | 65 | 9 |
| 4 | 89 | 9 |
| 5 | 92 | 9 + 6 UI |
| 6 | 103 | 9 + 6 UI |

Counts are approximate — they assume no extra cases were added while implementing.
