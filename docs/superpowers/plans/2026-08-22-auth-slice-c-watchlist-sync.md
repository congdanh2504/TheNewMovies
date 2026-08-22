# Supabase Auth — Slice C: Watchlist Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the watchlist into Postgres so it follows the user to a new install, keeping Room as the read cache and keeping bookmark/rating writes instant and offline-tolerant.

**Architecture:** A row-level-secured `watchlist` table holds one row per `(user_id, movie_id)`. `WatchlistEntity` gains `userId`, `pendingSync`, and `deleted`; every read filters on the signed-in user and excludes soft-deleted rows. Writes go to Room first with `pendingSync = true`, then push to Postgrest and clear the flag on success. `syncWatchlist()` runs on the signed-out → signed-in transition: push everything pending, then pull the remote rows and replace the local non-pending ones. Postgrest access is isolated in a `WatchlistRemoteDataSource` so the repository stays unit-testable with MockK.

**Tech Stack:** Room (destructive migration to version 2), supabase-kt Postgrest, kotlinx-serialization for the row DTO, Hilt, MockK + Turbine for unit tests, instrumented Room tests.

**Depends on:** Slice A (`core:supabase`, `AuthRepository`, `SessionState`) and Slice B (a real signed-in user to scope rows to).

Spec: [`../specs/2026-08-22-supabase-auth-design.md`](../specs/2026-08-22-supabase-auth-design.md)

---

## File structure

| File | Responsibility |
| --- | --- |
| `docs/supabase/watchlist.sql` | the table, RLS, and policy — checked in so the schema is reviewable |
| `core/database/.../entity/WatchlistEntity.kt` | three new columns, composite primary key |
| `core/database/.../dao/WatchlistDao.kt` | user-scoped queries plus the sync queries |
| `core/database/.../MoviesDatabase.kt` | version 2 |
| `core/database/.../di/DatabaseModule.kt` | destructive migration |
| `core/database/.../model/WatchlistEntityExt.kt` | entity ↔ model mapping, now user-aware |
| `core/data/.../remote/WatchlistRow.kt` | the Postgres row DTO and its mapping |
| `core/data/.../remote/WatchlistRemoteDataSource.kt` | the only file that calls Postgrest |
| `core/data/.../repository/WatchlistRepository.kt` | interface gains `syncWatchlist()` |
| `core/data/.../repository/DefaultWatchlistRepository.kt` | user scoping, write-through, sync |
| `core/testing/.../repository/TestWatchlistRepository.kt` | fake gains `syncWatchlist()` |
| `app/.../ui/AppViewModel.kt` | triggers sync on sign-in |

---

### Task 1: The Postgres table

**Files:**
- Create: `docs/supabase/watchlist.sql`

- [ ] **Step 1: Write the SQL**

```sql
-- Watchlist rows, one per (user, movie). Run in the Supabase SQL editor.
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
```

- [ ] **Step 2: Run it**

Paste it into Dashboard → SQL Editor → Run. Then check Dashboard → Table Editor → `watchlist`
exists and shows the RLS-enabled badge. **If RLS is not enabled, every user can read every other
user's rows** — the anon key is public, so the policy is the only thing protecting the data.

- [ ] **Step 3: Commit**

```bash
git add docs/supabase/watchlist.sql
git commit -m "docs(supabase): add the watchlist table and its RLS policy"
```

---

### Task 2: The Room schema change

**Files:**
- Modify: `core/database/src/main/kotlin/com/practice/thenewmovies/core/database/entity/WatchlistEntity.kt`
- Modify: `core/database/src/main/kotlin/com/practice/thenewmovies/core/database/dao/WatchlistDao.kt`
- Modify: `core/database/src/main/kotlin/com/practice/thenewmovies/core/database/MoviesDatabase.kt`
- Modify: `core/database/src/main/kotlin/com/practice/thenewmovies/core/database/di/DatabaseModule.kt`
- Modify: `core/database/src/main/kotlin/com/practice/thenewmovies/core/database/model/WatchlistEntityExt.kt`
- Test: `core/database/src/androidTest/kotlin/com/practice/thenewmovies/core/database/dao/WatchlistDaoTest.kt`

- [ ] **Step 1: Write the failing test**

Replace the existing `WatchlistDaoTest` body with this — the old tests call the pre-scoping DAO
signatures and would not compile:

```kotlin
package com.practice.thenewmovies.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.practice.thenewmovies.core.database.MoviesDatabase
import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WatchlistDaoTest {

    private lateinit var database: MoviesDatabase
    private lateinit var dao: WatchlistDao

    private val alice = "user-alice"
    private val bob = "user-bob"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoviesDatabase::class.java,
        ).build()
        dao = database.watchlistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(
        userId: String,
        movieId: Int,
        title: String = "Dune",
        pendingSync: Boolean = false,
        deleted: Boolean = false,
    ) = WatchlistEntity(
        userId = userId,
        movieId = movieId,
        title = title,
        posterPath = null,
        backdropPath = null,
        releaseDate = "2021-10-22",
        voteAverage = 7.8,
        runtime = 155,
        genre = "Science Fiction",
        userRating = null,
        pendingSync = pendingSync,
        deleted = deleted,
    )

    @Test
    fun rowsAreScopedToTheirUser() = runTest {
        dao.upsert(entity(alice, movieId = 1, title = "Dune"))
        dao.upsert(entity(bob, movieId = 2, title = "Arrival"))

        assertEquals(listOf("Dune"), dao.getAll(alice).first().map { it.title })
        assertEquals(listOf("Arrival"), dao.getAll(bob).first().map { it.title })
    }

    @Test
    fun theSameMovieCanBeSavedByTwoUsers() = runTest {
        dao.upsert(entity(alice, movieId = 1))
        dao.upsert(entity(bob, movieId = 1))

        assertEquals(1, dao.getAll(alice).first().size)
        assertEquals(1, dao.getAll(bob).first().size)
    }

    @Test
    fun softDeletedRowsAreHiddenFromReads() = runTest {
        dao.upsert(entity(alice, movieId = 1))

        dao.markDeleted(alice, movieId = 1)

        assertTrue(dao.getAll(alice).first().isEmpty())
        assertFalse(dao.existsById(alice, movieId = 1).first())
    }

    @Test
    fun markingDeletedAlsoMarksTheRowPending() = runTest {
        dao.upsert(entity(alice, movieId = 1))

        dao.markDeleted(alice, movieId = 1)

        val pending = dao.getPending(alice)
        assertEquals(1, pending.size)
        assertTrue(pending.single().deleted)
    }

    @Test
    fun ratingAMovieMarksItPending() = runTest {
        dao.upsert(entity(alice, movieId = 1))

        dao.updateRating(alice, movieId = 1, rating = 4.5f)

        assertEquals(4.5f, dao.getRating(alice, movieId = 1).first())
        assertEquals(listOf(1), dao.getPending(alice).map { it.movieId })
    }

    @Test
    fun clearingPendingLeavesTheRow() = runTest {
        dao.upsert(entity(alice, movieId = 1, pendingSync = true))

        dao.clearPending(alice, movieId = 1)

        assertTrue(dao.getPending(alice).isEmpty())
        assertEquals(1, dao.getAll(alice).first().size)
    }

    @Test
    fun deletingSyncedRowsKeepsPendingOnes() = runTest {
        dao.upsert(entity(alice, movieId = 1, pendingSync = true))
        dao.upsert(entity(alice, movieId = 2, pendingSync = false))

        dao.deleteSynced(alice)

        assertEquals(listOf(1), dao.getAll(alice).first().map { it.movieId })
    }

    @Test
    fun hardDeleteRemovesTheRowEntirely() = runTest {
        dao.upsert(entity(alice, movieId = 1, deleted = true, pendingSync = true))

        dao.deleteById(alice, movieId = 1)

        assertTrue(dao.getPending(alice).isEmpty())
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :core:database:connectedDebugAndroidTest`
Expected: compilation failure — `WatchlistEntity` has no `userId`, and the DAO has no
`markDeleted`.

- [ ] **Step 3: Change the entity**

```kotlin
package com.practice.thenewmovies.core.database.entity

import androidx.room.Entity

@Entity(tableName = "watchlist", primaryKeys = ["userId", "movieId"])
data class WatchlistEntity(
    val userId: String,
    val movieId: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val runtime: Int,
    val genre: String,
    val userRating: Float? = null,
    /** Local change not yet accepted by Postgres. */
    val pendingSync: Boolean = false,
    /** Soft delete, so an offline un-bookmark stays replayable. */
    val deleted: Boolean = false,
)
```

The `@PrimaryKey` import goes away — the composite key is declared on `@Entity`, the same way
`MovieEntity` declares `["id", "category"]`.

- [ ] **Step 4: Change the DAO**

```kotlin
package com.practice.thenewmovies.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Upsert
    suspend fun upsert(entity: WatchlistEntity)

    @Upsert
    suspend fun upsertAll(entities: List<WatchlistEntity>)

    @Query(
        "SELECT * FROM watchlist WHERE userId = :userId AND deleted = 0 ORDER BY title ASC",
    )
    fun getAll(userId: String): Flow<List<WatchlistEntity>>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM watchlist " +
            "WHERE userId = :userId AND movieId = :movieId AND deleted = 0)",
    )
    fun existsById(userId: String, movieId: Int): Flow<Boolean>

    @Query("SELECT userRating FROM watchlist WHERE userId = :userId AND movieId = :movieId")
    fun getRating(userId: String, movieId: Int): Flow<Float?>

    @Query(
        "UPDATE watchlist SET userRating = :rating, pendingSync = 1 " +
            "WHERE userId = :userId AND movieId = :movieId",
    )
    suspend fun updateRating(userId: String, movieId: Int, rating: Float)

    /** Soft delete: keeps the row so the deletion can be pushed later. */
    @Query(
        "UPDATE watchlist SET deleted = 1, pendingSync = 1 " +
            "WHERE userId = :userId AND movieId = :movieId",
    )
    suspend fun markDeleted(userId: String, movieId: Int)

    @Query("SELECT * FROM watchlist WHERE userId = :userId AND pendingSync = 1")
    suspend fun getPending(userId: String): List<WatchlistEntity>

    @Query(
        "UPDATE watchlist SET pendingSync = 0 WHERE userId = :userId AND movieId = :movieId",
    )
    suspend fun clearPending(userId: String, movieId: Int)

    /** Hard delete, used once a soft-deleted row has been accepted by Postgres. */
    @Query("DELETE FROM watchlist WHERE userId = :userId AND movieId = :movieId")
    suspend fun deleteById(userId: String, movieId: Int)

    /** Clears the cache before a pull, leaving unsynced local changes alone. */
    @Query("DELETE FROM watchlist WHERE userId = :userId AND pendingSync = 0")
    suspend fun deleteSynced(userId: String)
}
```

- [ ] **Step 5: Bump the database version and add the destructive migration**

In `MoviesDatabase.kt`:

```kotlin
    version = 2,
```

In `DatabaseModule.kt`:

```kotlin
    @Provides
    @Singleton
    fun providesMoviesDatabase(@ApplicationContext context: Context): MoviesDatabase =
        Room.databaseBuilder(context, MoviesDatabase::class.java, "movies.db")
            // The app is unreleased and every pre-auth watchlist row belongs to no user, so
            // there is nothing worth migrating.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
```

If `fallbackToDestructiveMigration(dropAllTables = ...)` does not resolve, this Room version still
has the deprecated no-argument overload — use `fallbackToDestructiveMigration()` and leave the
comment.

- [ ] **Step 6: Update the entity mapping**

`WatchlistEntityExt.kt` — `asEntity` now needs the user and the sync flags:

```kotlin
package com.practice.thenewmovies.core.database.model

import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import com.practice.thenewmovies.core.model.WatchlistMovie

fun WatchlistEntity.asExternalModel() = WatchlistMovie(
    id = movieId,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    runtime = runtime,
    genre = genre,
    userRating = userRating,
)

fun WatchlistMovie.asEntity(
    userId: String,
    pendingSync: Boolean = false,
    deleted: Boolean = false,
) = WatchlistEntity(
    userId = userId,
    movieId = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    runtime = runtime,
    genre = genre,
    userRating = userRating,
    pendingSync = pendingSync,
    deleted = deleted,
)
```

`DefaultWatchlistRepository` will not compile until Task 4 — that is expected. To run the DAO test
now, comment out the two `asEntity()` call sites in that file, or do Tasks 2–4 before running any
whole-project build.

- [ ] **Step 7: Run the DAO test to verify it passes**

Run: `./gradlew :core:database:connectedDebugAndroidTest`
Expected: PASS, 8 tests. Confirm the count in
`core/database/build/outputs/androidTest-results/connected/**/*.xml`; `tests="0"` means the runner
found nothing.

- [ ] **Step 8: Commit, including the regenerated schema**

`exportSchema = true` and the Room plugin writes to `core/database/schemas`, so a new
`2.json` appears.

```bash
./gradlew spotlessApply
git add core/database
git commit -m "feat(database): scope watchlist rows to a user and track sync state"
```

---

### Task 3: The Postgres row DTO

**Files:**
- Modify: `core/data/build.gradle.kts`
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/remote/WatchlistRow.kt`
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/remote/WatchlistRemoteDataSource.kt`
- Test: `core/data/src/test/kotlin/com/practice/thenewmovies/core/data/remote/WatchlistRowMappingTest.kt`

- [ ] **Step 1: Add the serialization plugin**

`core/data/build.gradle.kts` — the row DTO needs `@Serializable`:

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.practice.thenewmovies.core.data.remote

import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WatchlistRowMappingTest {

    private val entity = WatchlistEntity(
        userId = "user-1",
        movieId = 100,
        title = "Dune: Part Two",
        posterPath = "/poster.jpg",
        backdropPath = null,
        releaseDate = "2024-02-27",
        voteAverage = 8.2,
        runtime = 166,
        genre = "Science Fiction",
        userRating = 4.0f,
        pendingSync = true,
        deleted = false,
    )

    @Test
    fun `an entity maps to a row without its local sync flags`() {
        val row = entity.asRow()

        assertEquals("user-1", row.userId)
        assertEquals(100, row.movieId)
        assertEquals("Dune: Part Two", row.title)
        assertEquals(4.0f, row.userRating)
    }

    @Test
    fun `a row maps back to a synced entity`() {
        val mapped = entity.asRow().asEntity()

        assertEquals(entity.copy(pendingSync = false), mapped)
        assertFalse(mapped.pendingSync)
    }

    @Test
    fun `the row serialises with snake case column names`() {
        val json = Json.encodeToString(WatchlistRow.serializer(), entity.asRow())

        assertEquals(true, json.contains("\"user_id\""))
        assertEquals(true, json.contains("\"movie_id\""))
        assertEquals(true, json.contains("\"poster_path\""))
        assertEquals(true, json.contains("\"vote_average\""))
        assertEquals(true, json.contains("\"user_rating\""))
        // updated_at is defaulted by Postgres; sending it would overwrite the server's clock.
        assertEquals(false, json.contains("updated_at"))
    }
}
```

The snake-case test exists because a wrong `@SerialName` fails at runtime with a Postgrest error
about an unknown column, and nothing at compile time — the same class of silent mismatch as the
Moshi/R8 bug recorded in this directory's README.

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests '*WatchlistRowMappingTest*'`
Expected: FAIL — `Unresolved reference: WatchlistRow`.

- [ ] **Step 4: Write the DTO and mapping**

```kotlin
package com.practice.thenewmovies.core.data.remote

import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of `public.watchlist`. `updated_at` is deliberately absent: Postgres defaults it, and
 * sending a client clock would make it useless as a merge signal later.
 */
@Serializable
internal data class WatchlistRow(
    @SerialName("user_id") val userId: String,
    @SerialName("movie_id") val movieId: Int,
    @SerialName("title") val title: String,
    @SerialName("poster_path") val posterPath: String?,
    @SerialName("backdrop_path") val backdropPath: String?,
    @SerialName("release_date") val releaseDate: String,
    @SerialName("vote_average") val voteAverage: Double,
    @SerialName("runtime") val runtime: Int,
    @SerialName("genre") val genre: String,
    @SerialName("user_rating") val userRating: Float?,
)

internal fun WatchlistEntity.asRow() = WatchlistRow(
    userId = userId,
    movieId = movieId,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    runtime = runtime,
    genre = genre,
    userRating = userRating,
)

internal fun WatchlistRow.asEntity() = WatchlistEntity(
    userId = userId,
    movieId = movieId,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    runtime = runtime,
    genre = genre,
    userRating = userRating,
    pendingSync = false,
    deleted = false,
)
```

`WatchlistRow` is `internal`, so the test must live in the same module — it does.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :core:data:testDebugUnitTest --tests '*WatchlistRowMappingTest*'`
Expected: PASS, 3 tests.

- [ ] **Step 6: Write the remote data source**

```kotlin
package com.practice.thenewmovies.core.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only file in the app that talks to Postgrest.
 *
 * The `user_id` filters are belt and braces: the table's RLS policy already restricts every
 * statement to `auth.uid()`. They stay because a filter-less delete would be a catastrophic bug
 * if the policy were ever dropped.
 */
@Singleton
internal class WatchlistRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
) {

    suspend fun fetchAll(userId: String): List<WatchlistRow> =
        client.from(TABLE)
            .select { filter { eq("user_id", userId) } }
            .decodeList()

    suspend fun upsert(row: WatchlistRow) {
        client.from(TABLE).upsert(row)
    }

    suspend fun delete(userId: String, movieId: Int) {
        client.from(TABLE).delete {
            filter {
                eq("user_id", userId)
                eq("movie_id", movieId)
            }
        }
    }

    private companion object {
        const val TABLE = "watchlist"
    }
}
```

- [ ] **Step 7: Compile**

Run: `./gradlew :core:data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL, except for the `asEntity()` call sites in
`DefaultWatchlistRepository`, which Task 4 rewrites.

If `select { filter { ... } }` does not resolve, this postgrest-kt version uses the older
`select { eq("user_id", userId) }` shape without the `filter` wrapper. Check the resolved
version's README rather than guessing.

- [ ] **Step 8: Format and commit**

```bash
./gradlew spotlessApply
git add core/data
git commit -m "feat(data): add the watchlist Postgres row and its data source"
```

---

### Task 4: The repository

**Files:**
- Modify: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/WatchlistRepository.kt`
- Modify: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/DefaultWatchlistRepository.kt`
- Test: `core/data/src/test/kotlin/com/practice/thenewmovies/core/data/repository/DefaultWatchlistRepositoryTest.kt`

- [ ] **Step 1: Extend the interface**

```kotlin
interface WatchlistRepository {
    fun getWatchlist(): Flow<List<WatchlistMovie>>
    fun isInWatchlist(movieId: Int): Flow<Boolean>
    fun getRating(movieId: Int): Flow<Float?>
    suspend fun addToWatchlist(movie: WatchlistMovie)
    suspend fun removeFromWatchlist(movieId: Int)
    suspend fun setRating(movieId: Int, rating: Float)

    /** Pushes local changes, then pulls the server's rows. Called on sign-in. */
    suspend fun syncWatchlist()
}
```

The six existing methods keep their signatures, so Detail and Watch List need no changes.

- [ ] **Step 2: Write the failing test**

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.data.remote.WatchlistRemoteDataSource
import com.practice.thenewmovies.core.data.remote.asRow
import com.practice.thenewmovies.core.database.dao.WatchlistDao
import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import com.practice.thenewmovies.core.model.WatchlistMovie
import com.practice.thenewmovies.core.testing.repository.TestAuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DefaultWatchlistRepositoryTest {

    private val dao = mockk<WatchlistDao>(relaxed = true)
    private val remote = mockk<WatchlistRemoteDataSource>(relaxed = true)
    private val authRepository = TestAuthRepository().apply { emitSignedIn(id = "user-1") }

    private val repository = DefaultWatchlistRepository(
        watchlistDao = dao,
        remote = remote,
        authRepository = authRepository,
    )

    private val movie = WatchlistMovie(
        id = 100,
        title = "Dune: Part Two",
        posterPath = null,
        backdropPath = null,
        releaseDate = "2024-02-27",
        voteAverage = 8.2,
        runtime = 166,
        genre = "Science Fiction",
    )

    private fun entity(
        movieId: Int = 100,
        pendingSync: Boolean = false,
        deleted: Boolean = false,
    ) = WatchlistEntity(
        userId = "user-1",
        movieId = movieId,
        title = "Dune: Part Two",
        posterPath = null,
        backdropPath = null,
        releaseDate = "2024-02-27",
        voteAverage = 8.2,
        runtime = 166,
        genre = "Science Fiction",
        userRating = null,
        pendingSync = pendingSync,
        deleted = deleted,
    )

    @Test
    fun `reads are scoped to the signed-in user`() = runTest {
        every { dao.getAll("user-1") } returns flowOf(listOf(entity()))

        assertEquals(listOf(100), repository.getWatchlist().first().map { it.id })
    }

    @Test
    fun `a signed-out user has an empty watchlist`() = runTest {
        authRepository.emitSignedOut()

        assertTrue(repository.getWatchlist().first().isEmpty())
    }

    @Test
    fun `adding writes to Room first, then pushes and clears the flag`() = runTest {
        repository.addToWatchlist(movie)

        coVerify { dao.upsert(match { it.pendingSync && it.userId == "user-1" }) }
        coVerify { remote.upsert(any()) }
        coVerify { dao.clearPending("user-1", 100) }
    }

    @Test
    fun `a failed push leaves the row pending`() = runTest {
        coEvery { remote.upsert(any()) } throws IOException("offline")

        repository.addToWatchlist(movie)

        coVerify { dao.upsert(match { it.pendingSync }) }
        coVerify(exactly = 0) { dao.clearPending(any(), any()) }
    }

    @Test
    fun `removing soft-deletes locally, then hard-deletes once accepted`() = runTest {
        repository.removeFromWatchlist(100)

        coVerify { dao.markDeleted("user-1", 100) }
        coVerify { remote.delete("user-1", 100) }
        coVerify { dao.deleteById("user-1", 100) }
    }

    @Test
    fun `a failed delete keeps the soft-deleted row for later`() = runTest {
        coEvery { remote.delete(any(), any()) } throws IOException("offline")

        repository.removeFromWatchlist(100)

        coVerify { dao.markDeleted("user-1", 100) }
        coVerify(exactly = 0) { dao.deleteById(any(), any()) }
    }

    @Test
    fun `rating writes through the same way`() = runTest {
        coEvery { dao.getPending("user-1") } returns listOf(entity(pendingSync = true))

        repository.setRating(100, 4.5f)

        coVerify { dao.updateRating("user-1", 100, 4.5f) }
        coVerify { remote.upsert(any()) }
        coVerify { dao.clearPending("user-1", 100) }
    }

    @Test
    fun `sync pushes pending rows before pulling`() = runTest {
        coEvery { dao.getPending("user-1") } returnsMany listOf(
            listOf(entity(movieId = 1, pendingSync = true)),
            emptyList(),
        )
        coEvery { remote.fetchAll("user-1") } returns listOf(entity(movieId = 2).asRow())

        repository.syncWatchlist()

        coVerify { remote.upsert(any()) }
        coVerify { dao.deleteSynced("user-1") }
        coVerify { dao.upsertAll(match { rows -> rows.map { it.movieId } == listOf(2) }) }
    }

    @Test
    fun `sync pushes a pending delete as a delete`() = runTest {
        coEvery { dao.getPending("user-1") } returnsMany listOf(
            listOf(entity(movieId = 1, pendingSync = true, deleted = true)),
            emptyList(),
        )
        coEvery { remote.fetchAll("user-1") } returns emptyList()

        repository.syncWatchlist()

        coVerify { remote.delete("user-1", 1) }
        coVerify { dao.deleteById("user-1", 1) }
    }

    @Test
    fun `the pull never overwrites a row that is still pending`() = runTest {
        coEvery { dao.getPending("user-1") } returnsMany listOf(
            emptyList(),
            listOf(entity(movieId = 5, pendingSync = true)),
        )
        coEvery { remote.fetchAll("user-1") } returns listOf(
            entity(movieId = 5).asRow(),
            entity(movieId = 6).asRow(),
        )

        repository.syncWatchlist()

        coVerify { dao.upsertAll(match { rows -> rows.map { it.movieId } == listOf(6) }) }
    }

    @Test
    fun `sync does nothing when signed out`() = runTest {
        authRepository.emitSignedOut()

        repository.syncWatchlist()

        coVerify(exactly = 0) { remote.fetchAll(any()) }
    }
}
```

The `returnsMany` in the sync tests matters: `getPending` is called twice — once to decide what to
push, once after pushing to find what must survive the pull.

- [ ] **Step 3: Run it to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests '*DefaultWatchlistRepositoryTest*'`
Expected: FAIL — the constructor takes one argument and `syncWatchlist` does not exist.

- [ ] **Step 4: Write the repository**

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.data.remote.WatchlistRemoteDataSource
import com.practice.thenewmovies.core.data.remote.asEntity
import com.practice.thenewmovies.core.data.remote.asRow
import com.practice.thenewmovies.core.database.dao.WatchlistDao
import com.practice.thenewmovies.core.database.model.asEntity
import com.practice.thenewmovies.core.database.model.asExternalModel
import com.practice.thenewmovies.core.model.SessionState
import com.practice.thenewmovies.core.model.WatchlistMovie
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
internal class DefaultWatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val remote: WatchlistRemoteDataSource,
    private val authRepository: AuthRepository,
) : WatchlistRepository {

    private val userId: Flow<String?> = authRepository.sessionState.map { it.userIdOrNull() }

    override fun getWatchlist(): Flow<List<WatchlistMovie>> = userId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            watchlistDao.getAll(id).map { entities -> entities.map { it.asExternalModel() } }
        }
    }

    override fun isInWatchlist(movieId: Int): Flow<Boolean> = userId.flatMapLatest { id ->
        if (id == null) flowOf(false) else watchlistDao.existsById(id, movieId)
    }

    override fun getRating(movieId: Int): Flow<Float?> = userId.flatMapLatest { id ->
        if (id == null) flowOf(null) else watchlistDao.getRating(id, movieId)
    }

    override suspend fun addToWatchlist(movie: WatchlistMovie) {
        val id = currentUserId() ?: return
        val entity = movie.asEntity(userId = id, pendingSync = true)
        watchlistDao.upsert(entity)
        if (runCatching { remote.upsert(entity.asRow()) }.isSuccess) {
            watchlistDao.clearPending(id, movie.id)
        }
    }

    override suspend fun removeFromWatchlist(movieId: Int) {
        val id = currentUserId() ?: return
        watchlistDao.markDeleted(id, movieId)
        val pushed = runCatching { remote.delete(id, movieId) }.isSuccess
        if (pushed) watchlistDao.deleteById(id, movieId)
    }

    override suspend fun setRating(movieId: Int, rating: Float) {
        val id = currentUserId() ?: return
        watchlistDao.updateRating(userId = id, movieId = movieId, rating = rating)
        push(id, movieId)
    }

    override suspend fun syncWatchlist() {
        val id = currentUserId() ?: return

        watchlistDao.getPending(id).forEach { entity ->
            if (entity.deleted) {
                if (runCatching { remote.delete(id, entity.movieId) }.isSuccess) {
                    watchlistDao.deleteById(id, entity.movieId)
                }
            } else if (runCatching { remote.upsert(entity.asRow()) }.isSuccess) {
                watchlistDao.clearPending(id, entity.movieId)
            }
        }

        val rows = runCatching { remote.fetchAll(id) }.getOrNull() ?: return
        // Anything still pending after the push failed to reach the server; the pull must not
        // clobber it with the server's older row.
        val stillPending = watchlistDao.getPending(id).map { it.movieId }.toSet()
        watchlistDao.deleteSynced(id)
        watchlistDao.upsertAll(
            rows.filterNot { it.movieId in stillPending }.map { it.asEntity() },
        )
    }

    /**
     * Pushes one already-written local row and clears its flag if the server accepts it. Used by
     * [setRating], which changes one column and so does not hold a whole entity.
     */
    private suspend fun push(userId: String, movieId: Int) {
        val entity = watchlistDao.getPending(userId).firstOrNull { it.movieId == movieId } ?: return
        if (runCatching { remote.upsert(entity.asRow()) }.isSuccess) {
            watchlistDao.clearPending(userId, movieId)
        }
    }

    private suspend fun currentUserId(): String? =
        authRepository.sessionState.first { it !is SessionState.Loading }.userIdOrNull()

    private fun SessionState.userIdOrNull(): String? =
        (this as? SessionState.SignedIn)?.user?.id
}
```

Two things worth understanding before changing this file:

- `runCatching` around each remote call is what makes an offline write succeed locally. The
  `pendingSync` flag is the record that it has not been pushed, so nothing is lost by swallowing
  the exception here — unlike the movies repository, where a swallowed `JsonDataException` hid a
  real bug (see this directory's README).
- `currentUserId()` waits past `SessionState.Loading` rather than treating it as signed out. A tap
  landing in the first frames after launch would otherwise silently do nothing.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :core:data:testDebugUnitTest --tests '*DefaultWatchlistRepositoryTest*'`
Expected: PASS, 11 tests.

- [ ] **Step 6: Format and commit**

```bash
./gradlew spotlessApply
git add core/data
git commit -m "feat(data): sync the watchlist through Postgres with a dirty flag"
```

---

### Task 5: Update the fake

**Files:**
- Modify: `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/repository/TestWatchlistRepository.kt`

- [ ] **Step 1: Add the method**

```kotlin
    var syncCount = 0
        private set

    override suspend fun syncWatchlist() {
        syncCount++
    }
```

- [ ] **Step 2: Verify the modules that use it still compile**

Run: `./gradlew :feature:detail:impl:testDebugUnitTest :feature:watchlist:impl:testDebugUnitTest`
Expected: PASS. The fake's existing behaviour is unchanged, so no existing test needs editing.

- [ ] **Step 3: Format and commit**

```bash
./gradlew spotlessApply
git add core/testing
git commit -m "test(testing): add syncWatchlist to the watchlist fake"
```

---

### Task 6: Sync on sign-in

**Files:**
- Modify: `app/src/main/kotlin/com/practice/thenewmovies/ui/AppViewModel.kt`

- [ ] **Step 1: Trigger the sync**

```kotlin
package com.practice.thenewmovies.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.thenewmovies.core.data.repository.AuthRepository
import com.practice.thenewmovies.core.data.repository.WatchlistRepository
import com.practice.thenewmovies.core.model.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authRepository.sessionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SessionState.Loading,
        )

    init {
        viewModelScope.launch {
            sessionState
                .filterIsInstance<SessionState.SignedIn>()
                .map { it.user.id }
                .distinctUntilChanged()
                .onEach { watchlistRepository.syncWatchlist() }
                .collect()
        }
    }
}
```

Add `import kotlinx.coroutines.flow.collect` if the compiler asks for it.

`distinctUntilChanged()` on the user id, not on the state: a token refresh re-emits
`SignedIn` with the same user, and syncing on every refresh would hammer Postgrest for nothing.

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Format and commit**

```bash
./gradlew spotlessApply
git add app
git commit -m "feat(app): sync the watchlist when a session begins"
```

---

### Task 7: Full verification, on a device

- [ ] **Step 1: Run everything**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run the instrumented tests**

Run: `./gradlew connectedDebugAndroidTest`
Expected: PASS. Check the XML counts, not just the exit code.

- [ ] **Step 3: Install and verify against the live table**

Run: `./gradlew installDebug`

- [ ] Sign in, bookmark a movie from Detail → the row appears in Dashboard → Table Editor →
      `watchlist`, with the right `user_id`.
- [ ] Rate that movie → `user_rating` updates on the same row rather than adding a second one.
- [ ] Un-bookmark it → the row disappears from the table.
- [ ] Clear the app's data (`adb shell pm clear com.practice.thenewmovies`), sign in again →
      the watchlist repopulates from Postgres. This is the whole point of the slice.
- [ ] Sign in as a second account → its watchlist is empty, and the first account's rows are
      untouched. Then sign back in as the first → its rows are still there.
- [ ] Turn off all networking, bookmark a movie → it appears in Watch List immediately. Confirm
      the row is pending:

      ```bash
      adb exec-out run-as com.practice.thenewmovies \
        sqlite3 /data/data/com.practice.thenewmovies/databases/movies.db \
        "select movieId, pendingSync, deleted from watchlist;"
      ```

- [ ] Turn networking back on, sign out and back in → the pending row reaches Postgres and
      `pendingSync` returns to 0.
- [ ] Offline, un-bookmark a synced movie → it leaves the list. Check the row is
      `deleted=1, pendingSync=1`. Reconnect, sign out and in → the row is gone from both
      Postgres and Room.

- [ ] **Step 4: Commit anything Spotless changed**

```bash
git status --short
git add -A && git commit -m "style: apply spotless" || echo "nothing to commit"
```

## Slice C done when

- `./gradlew build` and `connectedDebugAndroidTest` are green
- Every box in Task 7 Step 3 is checked
- A fresh install signed in as an existing user shows that user's watchlist

## Findings from executing tasks 2 to 4

- **Tasks 2, 3 and 4 were executed as one commit**, not three. They are mutually dependent:
  `WatchlistRow`'s mapping needs the new entity columns, and `DefaultWatchlistRepository` will not
  compile without both. Splitting them would have meant committing a tree that does not build.
- **Both unverified API shapes in this plan turned out correct**, checked against the resolved
  sources rather than assumed: postgrest-kt 3.1.4 exposes `select { filter { eq(...) } }` and
  `delete { filter { ... } }` as written, and Room 2.7.1 has
  `fallbackToDestructiveMigration(dropAllTables: Boolean)` directly — no deprecated fallback needed.
- **`core:data` needed `testImplementation(projects.core.testing)`.** That looks like a dependency
  cycle (`core:testing` depends on `core:data`) but is not one — a test configuration depending on
  another module's main output is fine, and both the targeted test task and the full build confirm it.
- **`@OptIn(ExperimentalCoroutinesApi::class)` is still required** for `flatMapLatest` at the pinned
  coroutines 1.10.2.
- **Room stores `Boolean` as INTEGER 0/1**, so the DAO's raw `pendingSync = 1` predicates match what
  Room generates — proven on-device rather than reasoned about.
- **`deleteSynced` then `upsertAll` is not wrapped in a `@Transaction`**, so a live collector on
  `getAll()` can observe an empty list for an instant mid-sync. Sync only runs at sign-in, so this is
  a cosmetic flicker rather than data loss; it is left alone deliberately.

## Known limits, stated rather than hidden

- **Sync runs only at sign-in.** A change made on another device appears after a sign-out and
  sign-in, not while the app is open. Realtime subscriptions or a pull-to-refresh would fix it;
  neither is in this design.
- **Local pending changes always win.** `updated_at` is stored but never compared. Two devices
  editing the same movie's rating while both offline will resolve to whichever syncs last.
- **A pending row belonging to a user who never signs in again is never pushed.** Nothing prunes
  it; it is a few hundred bytes.
