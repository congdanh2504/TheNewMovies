# TheNewMovies Slice 2 — Data Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An offline-first data layer — Room is the single source of truth, TMDB feeds it through explicit refresh calls — plus the fakes any module can test against.

**Architecture:** Three model tiers that never leak into each other: `Network*` DTOs in `core:network`, `*Entity` in `core:database`, and the models in `core:model` that features see. `core:data` owns the repository interfaces, their `internal` offline-first implementations, and both mapping directions. Reads are Room-only `Flow`s; writes come from `refresh` calls that are skipped inside a 24-hour TTL.

**Tech Stack:** Room 2.7.1 + KSP, Retrofit 2.11 + Moshi, Paging 3.3.6, Preferences DataStore, Hilt, MockK + Turbine for tests.

**Depends on:** Slice 1 complete (app installs and runs).

**Spec:** `docs/superpowers/specs/2026-08-21-thenewmovies-design.md`

**Deviation from the spec:** the spec's inventory places `asEntity()` in `core:network`. It lives in `core:data` instead, because `asEntity()` needs to see both the DTO and the entity, and putting it in `core:network` would force that module to depend on Room. `asExternalModel()` stays in `core:database` as specified.

---

## File Structure

| File | Responsibility |
| --- | --- |
| `core/network/.../model/Network*.kt` | TMDB DTOs, one file per response shape |
| `core/network/.../MoviesNetworkDataSource.kt` | The interface `core:data` talks to |
| `core/network/.../retrofit/TmdbApi.kt` | Retrofit endpoint declarations (`internal`) |
| `core/network/.../retrofit/RetrofitMoviesNetwork.kt` | `internal` implementation, category → endpoint |
| `core/network/.../di/NetworkModule.kt` | Moshi, OkHttp, Retrofit, the auth interceptor |
| `core/database/.../entity/*.kt` | Room entities |
| `core/database/.../dao/*.kt` | DAOs |
| `core/database/.../MoviesDatabase.kt` | `@Database` declaration |
| `core/database/.../model/*Ext.kt` | `asExternalModel()` per entity |
| `core/database/.../di/DatabaseModule.kt` | Database and DAO providers |
| `core/datastore/.../UserPreferencesRepository.kt` | Selected home tab |
| `core/data/.../model/NetworkEntity.kt` | `asEntity()` per DTO |
| `core/data/.../model/NetworkModel.kt` | `asExternalModel()` for search results |
| `core/data/.../repository/MoviesRepository.kt` | Interface |
| `core/data/.../repository/OfflineFirstMoviesRepository.kt` | `internal` implementation |
| `core/data/.../repository/WatchlistRepository.kt` | Interface |
| `core/data/.../repository/DefaultWatchlistRepository.kt` | `internal` implementation |
| `core/data/.../paging/MoviePagingSource.kt` | Network-backed search paging |
| `core/data/.../util/NetworkMonitor.kt` | Connectivity interface |
| `core/data/.../util/ConnectivityManagerNetworkMonitor.kt` | `internal` implementation |
| `core/data/.../util/Clock.kt` | Injectable time source, so TTL is testable |
| `core/data/.../di/DataModule.kt` | `@Binds` per concern |
| `core/testing/.../MainDispatcherRule.kt` | Swaps `Dispatchers.Main` in unit tests |
| `core/testing/.../repository/Test*Repository.kt` | Controllable fakes |
| `core/testing/.../data/TestMovies.kt` | Static test data |

---

### Task 1: Catalog and settings additions

**Files:**
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Add the new modules**

Insert into the `include` block, keeping it alphabetical:

```kotlin
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:network")
include(":core:testing")
```

- [ ] **Step 2: Add the TMDB key to `local.properties`**

`local.properties` is git-ignored, so it must be created locally. The reference repo already holds a working key.

```bash
cd /Users/danhtruong/android/TheNewMovies
grep TMDB_API_KEY /Users/danhtruong/android/TheMovies/local.properties >> local.properties
grep -q sdk.dir local.properties || grep sdk.dir /Users/danhtruong/android/TheMovies/local.properties >> local.properties
cat local.properties
```

Expected: a `TMDB_API_KEY=...` line and an `sdk.dir=...` line. If `TMDB_API_KEY` is absent from the reference repo, get a v4 read access token from https://www.themoviedb.org/settings/api and add it by hand.

- [ ] **Step 3: Commit**

```bash
git add settings.gradle.kts
git commit -m "build: include data layer modules"
```

---

### Task 2: `core:network`

**Files:**
- Create: `core/network/build.gradle.kts`
- Create: `core/network/src/main/kotlin/com/practice/thenewmovies/core/network/model/NetworkMovie.kt`
- Create: `.../model/NetworkMovieDetail.kt`, `.../model/NetworkCast.kt`, `.../model/NetworkReview.kt`, `.../model/NetworkPage.kt`
- Create: `.../MoviesNetworkDataSource.kt`
- Create: `.../retrofit/TmdbApi.kt`, `.../retrofit/RetrofitMoviesNetwork.kt`
- Create: `.../di/NetworkModule.kt`

- [ ] **Step 1: Write `core/network/build.gradle.kts`**

The key is read from `local.properties` at configuration time and fails the build when missing, rather than producing an app that 401s at runtime.

```kotlin
import java.util.Properties

plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

val tmdbApiKey: String = Properties().apply {
    val file = rootProject.file("local.properties")
    require(file.exists()) { "local.properties is missing; add TMDB_API_KEY=<your token>" }
    file.inputStream().use { load(it) }
}.getProperty("TMDB_API_KEY").orEmpty()

require(tmdbApiKey.isNotBlank()) { "TMDB_API_KEY is missing from local.properties" }

android {
    namespace = "com.practice.thenewmovies.core.network"

    defaultConfig {
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.model)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.core)
}
```

- [ ] **Step 2: Write the DTOs**

`model/NetworkMovie.kt`:

```kotlin
package com.practice.thenewmovies.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkMovie(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "overview") val overview: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    @Json(name = "release_date") val releaseDate: String,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "vote_count") val voteCount: Int,
)
```

`model/NetworkMovieDetail.kt`:

```kotlin
package com.practice.thenewmovies.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkMovieDetail(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "original_title") val originalTitle: String,
    @Json(name = "original_language") val originalLanguage: String,
    @Json(name = "overview") val overview: String?,
    @Json(name = "genres") val genres: List<NetworkGenre>,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "backdrop_path") val backdropPath: String?,
    @Json(name = "release_date") val releaseDate: String,
    @Json(name = "runtime") val runtime: Long?,
    @Json(name = "status") val status: String,
    @Json(name = "video") val video: Boolean,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "vote_count") val voteCount: Int,
)

@JsonClass(generateAdapter = true)
data class NetworkGenre(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
)
```

`model/NetworkCast.kt`:

```kotlin
package com.practice.thenewmovies.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkCast(
    @Json(name = "cast_id") val castId: Int,
    @Json(name = "character") val character: String,
    @Json(name = "name") val name: String,
    @Json(name = "profile_path") val profilePath: String?,
)

@JsonClass(generateAdapter = true)
data class NetworkCastResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "cast") val cast: List<NetworkCast>,
)
```

`model/NetworkReview.kt`:

```kotlin
package com.practice.thenewmovies.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkReview(
    @Json(name = "author") val author: String,
    @Json(name = "content") val content: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "author_details") val authorDetails: NetworkAuthorDetails? = null,
)

@JsonClass(generateAdapter = true)
data class NetworkAuthorDetails(
    @Json(name = "avatar_path") val avatarPath: String? = null,
    @Json(name = "rating") val rating: Float? = null,
)
```

`model/NetworkPage.kt`:

```kotlin
package com.practice.thenewmovies.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkPage<T>(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<T>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int,
)
```

- [ ] **Step 3: Write `MoviesNetworkDataSource.kt`**

The interface `core:data` depends on. Retrofit stays an implementation detail: swapping in a fake for tests needs no HTTP mock server.

```kotlin
package com.practice.thenewmovies.core.network

import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.network.model.NetworkCast
import com.practice.thenewmovies.core.network.model.NetworkMovie
import com.practice.thenewmovies.core.network.model.NetworkMovieDetail
import com.practice.thenewmovies.core.network.model.NetworkPage
import com.practice.thenewmovies.core.network.model.NetworkReview

interface MoviesNetworkDataSource {
    suspend fun getMovies(category: MovieCategory): List<NetworkMovie>
    suspend fun getMovieDetail(movieId: Int): NetworkMovieDetail
    suspend fun getCast(movieId: Int): List<NetworkCast>
    suspend fun getReviews(movieId: Int): List<NetworkReview>
    suspend fun searchMovies(query: String, page: Int): NetworkPage<NetworkMovie>
}
```

- [ ] **Step 4: Write `retrofit/TmdbApi.kt`**

```kotlin
package com.practice.thenewmovies.core.network.retrofit

import com.practice.thenewmovies.core.network.model.NetworkCastResponse
import com.practice.thenewmovies.core.network.model.NetworkMovie
import com.practice.thenewmovies.core.network.model.NetworkMovieDetail
import com.practice.thenewmovies.core.network.model.NetworkPage
import com.practice.thenewmovies.core.network.model.NetworkReview
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface TmdbApi {

    @GET("movie/popular")
    suspend fun getPopular(): NetworkPage<NetworkMovie>

    @GET("movie/top_rated")
    suspend fun getTopRated(): NetworkPage<NetworkMovie>

    @GET("movie/now_playing")
    suspend fun getNowPlaying(): NetworkPage<NetworkMovie>

    @GET("movie/upcoming")
    suspend fun getUpcoming(): NetworkPage<NetworkMovie>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(@Path("movie_id") movieId: Int): NetworkMovieDetail

    @GET("movie/{movie_id}/credits")
    suspend fun getCredits(@Path("movie_id") movieId: Int): NetworkCastResponse

    @GET("movie/{movie_id}/reviews")
    suspend fun getReviews(@Path("movie_id") movieId: Int): NetworkPage<NetworkReview>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int,
    ): NetworkPage<NetworkMovie>
}
```

- [ ] **Step 5: Write `retrofit/RetrofitMoviesNetwork.kt`**

The `when` over `MovieCategory` is the one place endpoint choice lives, replacing the four category methods the reference repo repeated in its repository.

```kotlin
package com.practice.thenewmovies.core.network.retrofit

import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.network.MoviesNetworkDataSource
import com.practice.thenewmovies.core.network.model.NetworkCast
import com.practice.thenewmovies.core.network.model.NetworkMovie
import com.practice.thenewmovies.core.network.model.NetworkMovieDetail
import com.practice.thenewmovies.core.network.model.NetworkPage
import com.practice.thenewmovies.core.network.model.NetworkReview
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RetrofitMoviesNetwork @Inject constructor(
    private val api: TmdbApi,
) : MoviesNetworkDataSource {

    override suspend fun getMovies(category: MovieCategory): List<NetworkMovie> = when (category) {
        MovieCategory.POPULAR -> api.getPopular()
        MovieCategory.TOP_RATED -> api.getTopRated()
        MovieCategory.NOW_PLAYING -> api.getNowPlaying()
        MovieCategory.UPCOMING -> api.getUpcoming()
    }.results

    override suspend fun getMovieDetail(movieId: Int): NetworkMovieDetail =
        api.getMovieDetail(movieId)

    override suspend fun getCast(movieId: Int): List<NetworkCast> =
        api.getCredits(movieId).cast

    override suspend fun getReviews(movieId: Int): List<NetworkReview> =
        api.getReviews(movieId).results

    override suspend fun searchMovies(query: String, page: Int): NetworkPage<NetworkMovie> =
        api.searchMovies(query = query, page = page)
}
```

- [ ] **Step 6: Write `di/NetworkModule.kt`**

```kotlin
package com.practice.thenewmovies.core.network.di

import com.practice.thenewmovies.core.network.BuildConfig
import com.practice.thenewmovies.core.network.MoviesNetworkDataSource
import com.practice.thenewmovies.core.network.retrofit.RetrofitMoviesNetwork
import com.practice.thenewmovies.core.network.retrofit.TmdbApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    private const val BASE_URL = "https://api.themoviedb.org/3/"
    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun providesMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun providesOkHttpClient(): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY}")
                    .build(),
            )
        }
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun providesTmdbApi(moshi: Moshi, okHttpClient: OkHttpClient): TmdbApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(TmdbApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkDataSourceModule {

    @Binds
    internal abstract fun bindsMoviesNetworkDataSource(
        network: RetrofitMoviesNetwork,
    ): MoviesNetworkDataSource
}
```

- [ ] **Step 7: Build and commit**

Run: `./gradlew :core:network:assembleDebug`
Expected: `BUILD SUCCESSFUL`. A failure reading `local.properties` means Task 1 Step 2 was skipped.

```bash
./gradlew spotlessApply
git add core/network
git commit -m "feat(network): add TMDB data source"
```

---

### Task 3: `core:database` entities, DAOs and database

**Files:**
- Create: `core/database/build.gradle.kts`
- Create: `core/database/src/main/kotlin/com/practice/thenewmovies/core/database/entity/MovieEntity.kt`, `MovieDetailEntity.kt`, `CastEntity.kt`, `ReviewEntity.kt`, `WatchlistEntity.kt`
- Create: `.../dao/MovieDao.kt`, `MovieDetailDao.kt`, `CastDao.kt`, `ReviewDao.kt`, `WatchlistDao.kt`
- Create: `.../MoviesDatabase.kt`
- Create: `.../di/DatabaseModule.kt`

Entities match the reference repo, with `lastUpdated` renamed to `syncedAt` (the spec's name for the TTL column) and the `cast` table renamed to `movie_cast` — `cast` is a reserved word in SQL and Room quotes it inconsistently across versions.

- [ ] **Step 1: Write `core/database/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.room)
    alias(libs.plugins.themovies.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.practice.thenewmovies.core.database"
}

dependencies {
    api(projects.core.model)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
}
```

- [ ] **Step 2: Write the entities**

`entity/MovieEntity.kt`:

```kotlin
package com.practice.thenewmovies.core.database.entity

import androidx.room.Entity

@Entity(tableName = "movies", primaryKeys = ["id", "category"])
data class MovieEntity(
    val id: Int,
    val category: String,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val syncedAt: Long,
)
```

The primary key is `(id, category)`, not `id` alone as in the reference repo: the same movie legitimately appears in both Popular and Now Playing, and a single-column key silently drops it from one of the two lists.

`entity/MovieDetailEntity.kt`:

```kotlin
package com.practice.thenewmovies.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movie_details")
data class MovieDetailEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val originalTitle: String,
    val originalLanguage: String,
    val overview: String?,
    val genresJson: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val runtime: Long?,
    val status: String,
    val video: Boolean,
    val voteAverage: Double,
    val voteCount: Int,
    val syncedAt: Long,
)
```

`entity/CastEntity.kt`:

```kotlin
package com.practice.thenewmovies.core.database.entity

import androidx.room.Entity

@Entity(tableName = "movie_cast", primaryKeys = ["movieId", "castId"])
data class CastEntity(
    val movieId: Int,
    val castId: Int,
    val character: String,
    val name: String,
    val profilePath: String?,
)
```

`entity/ReviewEntity.kt`:

```kotlin
package com.practice.thenewmovies.core.database.entity

import androidx.room.Entity

@Entity(tableName = "reviews", primaryKeys = ["movieId", "author"])
data class ReviewEntity(
    val movieId: Int,
    val author: String,
    val content: String,
    val createdAt: String,
    val avatarPath: String?,
    val rating: Float?,
)
```

`entity/WatchlistEntity.kt`:

```kotlin
package com.practice.thenewmovies.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val movieId: Int,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val runtime: Int,
    val genre: String,
    val userRating: Float? = null,
)
```

- [ ] **Step 3: Write the DAOs**

`dao/MovieDao.kt`:

```kotlin
package com.practice.thenewmovies.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.practice.thenewmovies.core.database.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Upsert
    suspend fun upsertAll(movies: List<MovieEntity>)

    @Query("SELECT * FROM movies WHERE category = :category")
    fun getByCategory(category: String): Flow<List<MovieEntity>>

    @Query("SELECT MAX(syncedAt) FROM movies WHERE category = :category")
    suspend fun getSyncedAt(category: String): Long?

    @Query("DELETE FROM movies WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Transaction
    suspend fun replaceCategory(category: String, movies: List<MovieEntity>) {
        deleteByCategory(category)
        upsertAll(movies)
    }
}
```

`dao/MovieDetailDao.kt`:

```kotlin
package com.practice.thenewmovies.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.practice.thenewmovies.core.database.entity.MovieDetailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDetailDao {

    @Upsert
    suspend fun upsert(detail: MovieDetailEntity)

    @Query("SELECT * FROM movie_details WHERE id = :movieId")
    fun getById(movieId: Int): Flow<MovieDetailEntity?>

    @Query("SELECT syncedAt FROM movie_details WHERE id = :movieId")
    suspend fun getSyncedAt(movieId: Int): Long?
}
```

`dao/CastDao.kt`:

```kotlin
package com.practice.thenewmovies.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.practice.thenewmovies.core.database.entity.CastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CastDao {

    @Upsert
    suspend fun upsertAll(cast: List<CastEntity>)

    @Query("SELECT * FROM movie_cast WHERE movieId = :movieId")
    fun getByMovieId(movieId: Int): Flow<List<CastEntity>>
}
```

`dao/ReviewDao.kt`:

```kotlin
package com.practice.thenewmovies.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.practice.thenewmovies.core.database.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Upsert
    suspend fun upsertAll(reviews: List<ReviewEntity>)

    @Query("SELECT * FROM reviews WHERE movieId = :movieId")
    fun getByMovieId(movieId: Int): Flow<List<ReviewEntity>>
}
```

`dao/WatchlistDao.kt`:

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

    @Query("DELETE FROM watchlist WHERE movieId = :movieId")
    suspend fun deleteById(movieId: Int)

    @Query("SELECT * FROM watchlist ORDER BY title ASC")
    fun getAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE movieId = :movieId)")
    fun existsById(movieId: Int): Flow<Boolean>

    @Query("UPDATE watchlist SET userRating = :rating WHERE movieId = :movieId")
    suspend fun updateRating(movieId: Int, rating: Float)

    @Query("SELECT userRating FROM watchlist WHERE movieId = :movieId")
    fun getRating(movieId: Int): Flow<Float?>
}
```

- [ ] **Step 4: Write `MoviesDatabase.kt`**

```kotlin
package com.practice.thenewmovies.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.practice.thenewmovies.core.database.dao.CastDao
import com.practice.thenewmovies.core.database.dao.MovieDao
import com.practice.thenewmovies.core.database.dao.MovieDetailDao
import com.practice.thenewmovies.core.database.dao.ReviewDao
import com.practice.thenewmovies.core.database.dao.WatchlistDao
import com.practice.thenewmovies.core.database.entity.CastEntity
import com.practice.thenewmovies.core.database.entity.MovieDetailEntity
import com.practice.thenewmovies.core.database.entity.MovieEntity
import com.practice.thenewmovies.core.database.entity.ReviewEntity
import com.practice.thenewmovies.core.database.entity.WatchlistEntity

@Database(
    entities = [
        MovieEntity::class,
        MovieDetailEntity::class,
        CastEntity::class,
        ReviewEntity::class,
        WatchlistEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MoviesDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun movieDetailDao(): MovieDetailDao
    abstract fun castDao(): CastDao
    abstract fun reviewDao(): ReviewDao
    abstract fun watchlistDao(): WatchlistDao
}
```

- [ ] **Step 5: Write `di/DatabaseModule.kt`**

```kotlin
package com.practice.thenewmovies.core.database.di

import android.content.Context
import androidx.room.Room
import com.practice.thenewmovies.core.database.MoviesDatabase
import com.practice.thenewmovies.core.database.dao.CastDao
import com.practice.thenewmovies.core.database.dao.MovieDao
import com.practice.thenewmovies.core.database.dao.MovieDetailDao
import com.practice.thenewmovies.core.database.dao.ReviewDao
import com.practice.thenewmovies.core.database.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun providesMoviesDatabase(@ApplicationContext context: Context): MoviesDatabase =
        Room.databaseBuilder(context, MoviesDatabase::class.java, "movies.db").build()

    @Provides
    fun providesMovieDao(database: MoviesDatabase): MovieDao = database.movieDao()

    @Provides
    fun providesMovieDetailDao(database: MoviesDatabase): MovieDetailDao = database.movieDetailDao()

    @Provides
    fun providesCastDao(database: MoviesDatabase): CastDao = database.castDao()

    @Provides
    fun providesReviewDao(database: MoviesDatabase): ReviewDao = database.reviewDao()

    @Provides
    fun providesWatchlistDao(database: MoviesDatabase): WatchlistDao = database.watchlistDao()
}
```

- [ ] **Step 6: Build and confirm the schema is exported**

Run: `./gradlew :core:database:assembleDebug`
Expected: `BUILD SUCCESSFUL`, and `core/database/schemas/com.practice.thenewmovies.core.database.MoviesDatabase/1.json` now exists.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add core/database
git commit -m "feat(database): add entities, DAOs and Room database"
```

---

### Task 4: `core:database` entity-to-model mapping

**Files:**
- Create: `core/database/src/main/kotlin/com/practice/thenewmovies/core/database/model/MovieEntityExt.kt`
- Create: `.../model/MovieDetailEntityExt.kt`, `.../model/CastEntityExt.kt`, `.../model/ReviewEntityExt.kt`, `.../model/WatchlistEntityExt.kt`
- Test: `core/database/src/test/kotlin/com/practice/thenewmovies/core/database/model/MovieDetailEntityExtTest.kt`

`asExternalModel()` lives beside the entity, so nothing outside this module needs to know how genres are stored.

- [ ] **Step 1: Write the failing test**

Genre storage is the only mapping with logic worth testing: it is JSON in a column, and malformed JSON must not crash the detail screen.

```kotlin
package com.practice.thenewmovies.core.database.model

import com.practice.thenewmovies.core.database.entity.MovieDetailEntity
import com.practice.thenewmovies.core.model.Genre
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieDetailEntityExtTest {

    private fun entity(genresJson: String) = MovieDetailEntity(
        id = 1,
        title = "Dune",
        originalTitle = "Dune",
        originalLanguage = "en",
        overview = "Sand.",
        genresJson = genresJson,
        posterPath = null,
        backdropPath = null,
        releaseDate = "2021-10-22",
        runtime = 155,
        status = "Released",
        video = false,
        voteAverage = 7.8,
        voteCount = 100,
        syncedAt = 0,
    )

    @Test
    fun `parses stored genres`() {
        val model = entity("""[{"id":878,"name":"Science Fiction"}]""").asExternalModel()

        assertEquals(listOf(Genre(id = 878, name = "Science Fiction")), model.genres)
    }

    @Test
    fun `returns no genres when the column is malformed`() {
        val model = entity("not json").asExternalModel()

        assertEquals(emptyList<Genre>(), model.genres)
    }

    @Test
    fun `round trips a genre list through the column encoding`() {
        val genres = listOf(Genre(id = 1, name = "Action"), Genre(id = 2, name = "Drama"))

        val decoded = entity(genres.asGenresJson()).asExternalModel().genres

        assertEquals(genres, decoded)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :core:database:testDebugUnitTest`
Expected: compilation failure — `Unresolved reference: asExternalModel`.

- [ ] **Step 3: Write `model/MovieDetailEntityExt.kt`**

```kotlin
package com.practice.thenewmovies.core.database.model

import com.practice.thenewmovies.core.database.entity.MovieDetailEntity
import com.practice.thenewmovies.core.model.Genre
import com.practice.thenewmovies.core.model.MovieDetail
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
// Required for the reified `encodeToString(value)`; without it the call resolves to the
// two-argument overload and fails to compile.
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class GenreJson(val id: Int, val name: String)

private val json = Json { ignoreUnknownKeys = true }

fun MovieDetailEntity.asExternalModel() = MovieDetail(
    id = id,
    title = title,
    originalTitle = originalTitle,
    originalLanguage = originalLanguage,
    overview = overview,
    genres = genresJson.asGenres(),
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    runtime = runtime,
    status = status,
    video = video,
    voteAverage = voteAverage,
    voteCount = voteCount,
)

/** Encodes genres for the `genresJson` column. */
fun List<Genre>.asGenresJson(): String =
    json.encodeToString(map { GenreJson(id = it.id, name = it.name) })

private fun String.asGenres(): List<Genre> = try {
    json.decodeFromString<List<GenreJson>>(this).map { Genre(id = it.id, name = it.name) }
} catch (e: SerializationException) {
    emptyList()
} catch (e: IllegalArgumentException) {
    emptyList()
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :core:database:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 5: Write the remaining mappers**

`model/MovieEntityExt.kt`:

```kotlin
package com.practice.thenewmovies.core.database.model

import com.practice.thenewmovies.core.database.entity.MovieEntity
import com.practice.thenewmovies.core.model.Movie

fun MovieEntity.asExternalModel() = Movie(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
)
```

`model/CastEntityExt.kt`:

```kotlin
package com.practice.thenewmovies.core.database.model

import com.practice.thenewmovies.core.database.entity.CastEntity
import com.practice.thenewmovies.core.model.Cast

fun CastEntity.asExternalModel() = Cast(
    castId = castId,
    character = character,
    name = name,
    profilePath = profilePath,
)
```

`model/ReviewEntityExt.kt`:

```kotlin
package com.practice.thenewmovies.core.database.model

import com.practice.thenewmovies.core.database.entity.ReviewEntity
import com.practice.thenewmovies.core.model.Review

fun ReviewEntity.asExternalModel() = Review(
    author = author,
    content = content,
    createdAt = createdAt,
    avatarPath = avatarPath,
    rating = rating,
)
```

`model/WatchlistEntityExt.kt` — both directions, because the watchlist is the one table the user writes to:

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

fun WatchlistMovie.asEntity() = WatchlistEntity(
    movieId = id,
    title = title,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    runtime = runtime,
    genre = genre,
    userRating = userRating,
)
```

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add core/database
git commit -m "feat(database): map entities to domain models"
```

---

### Task 5: DAO instrumented tests

**Files:**
- Test: `core/database/src/androidTest/kotlin/com/practice/thenewmovies/core/database/dao/MovieDaoTest.kt`
- Test: `core/database/src/androidTest/kotlin/com/practice/thenewmovies/core/database/dao/WatchlistDaoTest.kt`

- [ ] **Step 1: Write `MovieDaoTest.kt`**

```kotlin
package com.practice.thenewmovies.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.practice.thenewmovies.core.database.MoviesDatabase
import com.practice.thenewmovies.core.database.entity.MovieEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MovieDaoTest {

    private lateinit var database: MoviesDatabase
    private lateinit var dao: MovieDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoviesDatabase::class.java,
        ).build()
        dao = database.movieDao()
    }

    @After
    fun tearDown() = database.close()

    private fun movie(id: Int, category: String, syncedAt: Long = 1_000L) = MovieEntity(
        id = id,
        category = category,
        title = "Movie $id",
        overview = "Overview",
        posterPath = null,
        backdropPath = null,
        releaseDate = "2024-01-01",
        voteAverage = 7.0,
        voteCount = 10,
        syncedAt = syncedAt,
    )

    @Test
    fun getByCategory_returnsOnlyThatCategory() = runTest {
        dao.upsertAll(listOf(movie(1, "POPULAR"), movie(2, "TOP_RATED")))

        val popular = dao.getByCategory("POPULAR").first()

        assertEquals(listOf(1), popular.map { it.id })
    }

    @Test
    fun theSameMovieCanLiveInTwoCategories() = runTest {
        dao.upsertAll(listOf(movie(1, "POPULAR"), movie(1, "NOW_PLAYING")))

        assertEquals(1, dao.getByCategory("POPULAR").first().size)
        assertEquals(1, dao.getByCategory("NOW_PLAYING").first().size)
    }

    @Test
    fun replaceCategory_dropsRowsMissingFromTheNewList() = runTest {
        dao.upsertAll(listOf(movie(1, "POPULAR"), movie(2, "POPULAR")))

        dao.replaceCategory("POPULAR", listOf(movie(3, "POPULAR")))

        assertEquals(listOf(3), dao.getByCategory("POPULAR").first().map { it.id })
    }

    @Test
    fun replaceCategory_leavesOtherCategoriesAlone() = runTest {
        dao.upsertAll(listOf(movie(1, "POPULAR"), movie(2, "UPCOMING")))

        dao.replaceCategory("POPULAR", emptyList())

        assertEquals(listOf(2), dao.getByCategory("UPCOMING").first().map { it.id })
    }

    @Test
    fun getSyncedAt_returnsNewestValueAndNullWhenEmpty() = runTest {
        assertEquals(null, dao.getSyncedAt("POPULAR"))

        dao.upsertAll(
            listOf(
                movie(1, "POPULAR", syncedAt = 100L),
                movie(2, "POPULAR", syncedAt = 500L),
            ),
        )

        assertEquals(500L, dao.getSyncedAt("POPULAR"))
    }
}
```

- [ ] **Step 2: Write `WatchlistDaoTest.kt`**

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

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MoviesDatabase::class.java,
        ).build()
        dao = database.watchlistDao()
    }

    @After
    fun tearDown() = database.close()

    private fun entry(movieId: Int, title: String = "Movie $movieId") = WatchlistEntity(
        movieId = movieId,
        title = title,
        posterPath = null,
        backdropPath = null,
        releaseDate = "2024-01-01",
        voteAverage = 7.0,
        runtime = 120,
        genre = "Action",
    )

    @Test
    fun getAll_isSortedByTitle() = runTest {
        dao.upsert(entry(1, title = "Zulu"))
        dao.upsert(entry(2, title = "Alien"))

        assertEquals(listOf(2, 1), dao.getAll().first().map { it.movieId })
    }

    @Test
    fun existsById_reflectsInsertAndDelete() = runTest {
        assertFalse(dao.existsById(1).first())

        dao.upsert(entry(1))
        assertTrue(dao.existsById(1).first())

        dao.deleteById(1)
        assertFalse(dao.existsById(1).first())
    }

    @Test
    fun updateRating_storesTheRating() = runTest {
        dao.upsert(entry(1))

        dao.updateRating(movieId = 1, rating = 4.5f)

        assertEquals(4.5f, dao.getRating(1).first())
    }

    @Test
    fun upsert_replacesAnExistingEntry() = runTest {
        dao.upsert(entry(1, title = "Old"))
        dao.upsert(entry(1, title = "New"))

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("New", all.single().title)
    }
}
```

- [ ] **Step 3: Run the instrumented tests**

An emulator or device must be connected.

Run: `./gradlew :core:database:connectedDebugAndroidTest`
Expected: `BUILD SUCCESSFUL`, 9 tests passed. With no device attached the task fails with `No connected devices!` — start an emulator and re-run.

- [ ] **Step 4: Commit**

```bash
git add core/database/src/androidTest
git commit -m "test(database): add MovieDao and WatchlistDao instrumented tests"
```

---

### Task 6: `core:datastore`

**Files:**
- Create: `core/datastore/build.gradle.kts`
- Create: `core/datastore/src/main/kotlin/com/practice/thenewmovies/core/datastore/UserPreferencesRepository.kt`
- Create: `core/datastore/src/main/kotlin/com/practice/thenewmovies/core/datastore/di/DataStoreModule.kt`

One integer of state (the selected home tab), so Preferences DataStore rather than Proto.

- [ ] **Step 1: Write `core/datastore/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

android {
    namespace = "com.practice.thenewmovies.core.datastore"
}

dependencies {
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
}
```

- [ ] **Step 2: Write `di/DataStoreModule.kt`**

Providing the `DataStore` through Hilt rather than a `Context` extension property keeps the repository a plain class with no Android dependency in its constructor.

```kotlin
package com.practice.thenewmovies.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {

    @Provides
    @Singleton
    fun providesPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("user_preferences")
    }
}
```

- [ ] **Step 3: Write `UserPreferencesRepository.kt`**

Interface plus `internal` implementation, the same shape as the repositories in `core:data`. The interface exists so `core:testing` can supply a fake — the home ViewModel reads the selected tab from here, and a unit test must not touch a real DataStore file.

```kotlin
package com.practice.thenewmovies.core.datastore

import kotlinx.coroutines.flow.Flow

data class UserPreferences(val selectedHomeTab: Int = 0)

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setSelectedHomeTab(index: Int)
}
```

`DefaultUserPreferencesRepository.kt`:

```kotlin
package com.practice.thenewmovies.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { stored ->
        UserPreferences(selectedHomeTab = stored[SELECTED_HOME_TAB] ?: 0)
    }

    override suspend fun setSelectedHomeTab(index: Int) {
        dataStore.edit { it[SELECTED_HOME_TAB] = index }
    }

    private companion object {
        val SELECTED_HOME_TAB = intPreferencesKey("selected_home_tab")
    }
}
```

Add the binding to `di/DataStoreModule.kt`, after the `DataStoreModule` object:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal abstract class UserPreferencesModule {

    @Binds
    internal abstract fun bindsUserPreferencesRepository(
        repository: DefaultUserPreferencesRepository,
    ): UserPreferencesRepository
}
```

with `import com.practice.thenewmovies.core.datastore.DefaultUserPreferencesRepository`, `import com.practice.thenewmovies.core.datastore.UserPreferencesRepository` and `import dagger.Binds` added to that file.

- [ ] **Step 4: Build and commit**

Run: `./gradlew :core:datastore:assembleDebug`
Expected: `BUILD SUCCESSFUL`

```bash
./gradlew spotlessApply
git add core/datastore
git commit -m "feat(datastore): store the selected home tab"
```

---

### Task 7: `core:data` mappers and interfaces

**Files:**
- Create: `core/data/build.gradle.kts`
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/model/NetworkEntity.kt`
- Create: `.../model/NetworkModel.kt`
- Create: `.../repository/MoviesRepository.kt`, `.../repository/WatchlistRepository.kt`
- Create: `.../util/Clock.kt`, `.../util/NetworkMonitor.kt`
- Test: `core/data/src/test/kotlin/com/practice/thenewmovies/core/data/model/NetworkEntityTest.kt`

- [ ] **Step 1: Write `core/data/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
    alias(libs.plugins.themovies.android.hilt)
}

android {
    namespace = "com.practice.thenewmovies.core.data"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.network)

    api(libs.paging.runtime)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.mockk)
}
```

- [ ] **Step 2: Write the failing mapper test**

Image-path prefixing is the mapping rule worth pinning: TMDB returns a relative path, the UI needs an absolute URL, and a null path must stay null rather than becoming a URL that 404s.

```kotlin
package com.practice.thenewmovies.core.data.model

import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.network.model.NetworkMovie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkEntityTest {

    private fun networkMovie(
        posterPath: String? = "/poster.jpg",
        backdropPath: String? = "/backdrop.jpg",
    ) = NetworkMovie(
        id = 1,
        title = "Dune",
        overview = "Sand.",
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = "2021-10-22",
        voteAverage = 7.8,
        voteCount = 100,
    )

    @Test
    fun `prefixes relative image paths`() {
        val entity = networkMovie().asEntity(MovieCategory.POPULAR, syncedAt = 42L)

        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", entity.posterPath)
        assertEquals("https://image.tmdb.org/t/p/w500/backdrop.jpg", entity.backdropPath)
    }

    @Test
    fun `keeps missing image paths null`() {
        val entity = networkMovie(posterPath = null, backdropPath = null)
            .asEntity(MovieCategory.POPULAR, syncedAt = 42L)

        assertNull(entity.posterPath)
        assertNull(entity.backdropPath)
    }

    @Test
    fun `carries the category and sync timestamp`() {
        val entity = networkMovie().asEntity(MovieCategory.UPCOMING, syncedAt = 42L)

        assertEquals("UPCOMING", entity.category)
        assertEquals(42L, entity.syncedAt)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: compilation failure — `Unresolved reference: asEntity`.

- [ ] **Step 4: Write `model/NetworkEntity.kt`**

```kotlin
package com.practice.thenewmovies.core.data.model

import com.practice.thenewmovies.core.database.entity.CastEntity
import com.practice.thenewmovies.core.database.entity.MovieDetailEntity
import com.practice.thenewmovies.core.database.entity.MovieEntity
import com.practice.thenewmovies.core.database.entity.ReviewEntity
import com.practice.thenewmovies.core.database.model.asGenresJson
import com.practice.thenewmovies.core.model.Genre
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.network.model.NetworkCast
import com.practice.thenewmovies.core.network.model.NetworkMovie
import com.practice.thenewmovies.core.network.model.NetworkMovieDetail
import com.practice.thenewmovies.core.network.model.NetworkReview

internal const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

internal fun String?.asImageUrl(): String? = when {
    this == null -> null
    startsWith("http") -> this
    else -> IMAGE_BASE_URL + this
}

fun NetworkMovie.asEntity(category: MovieCategory, syncedAt: Long) = MovieEntity(
    id = id,
    category = category.name,
    title = title,
    overview = overview,
    posterPath = posterPath.asImageUrl(),
    backdropPath = backdropPath.asImageUrl(),
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    syncedAt = syncedAt,
)

fun NetworkMovieDetail.asEntity(syncedAt: Long) = MovieDetailEntity(
    id = id,
    title = title,
    originalTitle = originalTitle,
    originalLanguage = originalLanguage,
    overview = overview,
    genresJson = genres.map { Genre(id = it.id, name = it.name) }.asGenresJson(),
    posterPath = posterPath.asImageUrl(),
    backdropPath = backdropPath.asImageUrl(),
    releaseDate = releaseDate,
    runtime = runtime,
    status = status,
    video = video,
    voteAverage = voteAverage,
    voteCount = voteCount,
    syncedAt = syncedAt,
)

fun NetworkCast.asEntity(movieId: Int) = CastEntity(
    movieId = movieId,
    castId = castId,
    character = character,
    name = name,
    profilePath = profilePath.asImageUrl(),
)

fun NetworkReview.asEntity(movieId: Int) = ReviewEntity(
    movieId = movieId,
    author = author,
    content = content,
    createdAt = createdAt,
    avatarPath = authorDetails?.avatarPath.asImageUrl(),
    rating = authorDetails?.rating,
)
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 6: Write `model/NetworkModel.kt`**

Search results go straight from the network to the UI without touching Room, so they need their own mapping.

```kotlin
package com.practice.thenewmovies.core.data.model

import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.network.model.NetworkMovie

fun NetworkMovie.asExternalModel() = Movie(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath.asImageUrl(),
    backdropPath = backdropPath.asImageUrl(),
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
)
```

- [ ] **Step 7: Write the repository interfaces**

`repository/MoviesRepository.kt`:

```kotlin
package com.practice.thenewmovies.core.data.repository

import androidx.paging.PagingData
import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import kotlinx.coroutines.flow.Flow

/**
 * Reads always come from local storage, so every screen works offline. Network access happens
 * only through the `refresh` functions, which no-op inside the sync TTL.
 */
interface MoviesRepository {

    fun getMovies(category: MovieCategory): Flow<List<Movie>>

    fun getMovieDetail(movieId: Int): Flow<MovieDetail?>

    fun getCast(movieId: Int): Flow<List<Cast>>

    fun getReviews(movieId: Int): Flow<List<Review>>

    /** Network-backed and not persisted: page numbering belongs to the server. */
    fun searchMoviesPaged(query: String): Flow<PagingData<Movie>>

    /** Returns false when the network call failed; cached data is still readable. */
    suspend fun refresh(category: MovieCategory): Boolean

    suspend fun refreshDetail(movieId: Int): Boolean
}
```

`repository/WatchlistRepository.kt`:

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.model.WatchlistMovie
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun getWatchlist(): Flow<List<WatchlistMovie>>
    fun isInWatchlist(movieId: Int): Flow<Boolean>
    fun getRating(movieId: Int): Flow<Float?>
    suspend fun addToWatchlist(movie: WatchlistMovie)
    suspend fun removeFromWatchlist(movieId: Int)
    suspend fun setRating(movieId: Int, rating: Float)
}
```

- [ ] **Step 8: Write `util/Clock.kt` and `util/NetworkMonitor.kt`**

`Clock` exists so the TTL is testable without waiting 24 hours or stubbing statics.

```kotlin
package com.practice.thenewmovies.core.data.util

/** Wall-clock time, injected so TTL logic is testable. */
fun interface Clock {
    fun nowMillis(): Long
}
```

```kotlin
package com.practice.thenewmovies.core.data.util

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
```

- [ ] **Step 9: Declare the connectivity permission in this module**

`ConnectivityManagerNetworkMonitor` calls `ConnectivityManager`, and lint fails the build if the
permission is only declared in `:app`. Create `core/data/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required by ConnectivityManagerNetworkMonitor. Declared here rather than only in :app so
         the requirement travels with the code that needs it. -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
</manifest>
```

- [ ] **Step 10: Commit**

```bash
./gradlew spotlessApply
git add core/data
git commit -m "feat(data): add repository interfaces and network mapping"
```

---

### Task 8: `OfflineFirstMoviesRepository`

**Files:**
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/OfflineFirstMoviesRepository.kt`
- Test: `core/data/src/test/kotlin/com/practice/thenewmovies/core/data/repository/OfflineFirstMoviesRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

The TTL is the interesting behaviour: a screen open must not hit the network when the cache is fresh, must hit it when stale, and must report failure without losing cached rows.

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.database.dao.CastDao
import com.practice.thenewmovies.core.database.dao.MovieDao
import com.practice.thenewmovies.core.database.dao.MovieDetailDao
import com.practice.thenewmovies.core.database.dao.ReviewDao
import com.practice.thenewmovies.core.database.entity.MovieEntity
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.network.MoviesNetworkDataSource
import com.practice.thenewmovies.core.network.model.NetworkMovie
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class OfflineFirstMoviesRepositoryTest {

    private val network = mockk<MoviesNetworkDataSource>(relaxed = true)
    private val movieDao = mockk<MovieDao>(relaxed = true)
    private val movieDetailDao = mockk<MovieDetailDao>(relaxed = true)
    private val castDao = mockk<CastDao>(relaxed = true)
    private val reviewDao = mockk<ReviewDao>(relaxed = true)
    private var now = 1_000_000L

    private val repository = OfflineFirstMoviesRepository(
        network = network,
        movieDao = movieDao,
        movieDetailDao = movieDetailDao,
        castDao = castDao,
        reviewDao = reviewDao,
        clock = { now },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private val networkMovie = NetworkMovie(
        id = 1,
        title = "Dune",
        overview = "Sand.",
        posterPath = "/poster.jpg",
        backdropPath = null,
        releaseDate = "2021-10-22",
        voteAverage = 7.8,
        voteCount = 100,
    )

    private val movieEntity = MovieEntity(
        id = 1,
        category = "POPULAR",
        title = "Dune",
        overview = "Sand.",
        posterPath = null,
        backdropPath = null,
        releaseDate = "2021-10-22",
        voteAverage = 7.8,
        voteCount = 100,
        syncedAt = 0L,
    )

    @Test
    fun `getMovies reads from the database and maps to models`() = runTest {
        coEvery { movieDao.getByCategory("POPULAR") } returns flowOf(listOf(movieEntity))

        val movies = repository.getMovies(MovieCategory.POPULAR).first()

        assertEquals(listOf("Dune"), movies.map { it.title })
    }

    @Test
    fun `refresh skips the network while the cache is fresh`() = runTest {
        coEvery { movieDao.getSyncedAt("POPULAR") } returns now - 1_000L

        assertTrue(repository.refresh(MovieCategory.POPULAR))

        coVerify(exactly = 0) { network.getMovies(any()) }
        coVerify(exactly = 0) { movieDao.replaceCategory(any(), any()) }
    }

    @Test
    fun `refresh calls the network when nothing is cached`() = runTest {
        coEvery { movieDao.getSyncedAt("POPULAR") } returns null
        coEvery { network.getMovies(MovieCategory.POPULAR) } returns listOf(networkMovie)

        assertTrue(repository.refresh(MovieCategory.POPULAR))

        coVerify(exactly = 1) { movieDao.replaceCategory("POPULAR", any()) }
    }

    @Test
    fun `refresh calls the network once the TTL has elapsed`() = runTest {
        coEvery { movieDao.getSyncedAt("POPULAR") } returns now - TTL_MS - 1
        coEvery { network.getMovies(MovieCategory.POPULAR) } returns listOf(networkMovie)

        assertTrue(repository.refresh(MovieCategory.POPULAR))

        coVerify(exactly = 1) { network.getMovies(MovieCategory.POPULAR) }
    }

    @Test
    fun `refresh reports failure and writes nothing when the network throws`() = runTest {
        coEvery { movieDao.getSyncedAt("POPULAR") } returns null
        coEvery { network.getMovies(MovieCategory.POPULAR) } throws IOException("offline")

        assertFalse(repository.refresh(MovieCategory.POPULAR))

        coVerify(exactly = 0) { movieDao.replaceCategory(any(), any()) }
    }

    @Test
    fun `refreshDetail stores detail cast and reviews together`() = runTest {
        coEvery { movieDetailDao.getSyncedAt(1) } returns null
        coEvery { network.getMovieDetail(1) } returns detailFixture()
        coEvery { network.getCast(1) } returns emptyList()
        coEvery { network.getReviews(1) } returns emptyList()

        assertTrue(repository.refreshDetail(1))

        coVerify(exactly = 1) { movieDetailDao.upsert(any()) }
        coVerify(exactly = 1) { castDao.upsertAll(any()) }
        coVerify(exactly = 1) { reviewDao.upsertAll(any()) }
    }

    @Test
    fun `refreshDetail skips the network while the cache is fresh`() = runTest {
        coEvery { movieDetailDao.getSyncedAt(1) } returns now - 1_000L

        assertTrue(repository.refreshDetail(1))

        coVerify(exactly = 0) { network.getMovieDetail(any()) }
    }

    private fun detailFixture() = com.practice.thenewmovies.core.network.model.NetworkMovieDetail(
        id = 1,
        title = "Dune",
        originalTitle = "Dune",
        originalLanguage = "en",
        overview = "Sand.",
        genres = emptyList(),
        posterPath = null,
        backdropPath = null,
        releaseDate = "2021-10-22",
        runtime = 155,
        status = "Released",
        video = false,
        voteAverage = 7.8,
        voteCount = 100,
    )
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: compilation failure — `Unresolved reference: OfflineFirstMoviesRepository`.

- [ ] **Step 3: Write `OfflineFirstMoviesRepository.kt`**

`TTL_MS` is `internal` so the test can reference it instead of duplicating the number.

```kotlin
package com.practice.thenewmovies.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.practice.thenewmovies.core.common.network.Dispatcher
import com.practice.thenewmovies.core.common.network.MoviesDispatchers
import com.practice.thenewmovies.core.data.model.asEntity
import com.practice.thenewmovies.core.data.paging.MoviePagingSource
import com.practice.thenewmovies.core.data.util.Clock
import com.practice.thenewmovies.core.database.dao.CastDao
import com.practice.thenewmovies.core.database.dao.MovieDao
import com.practice.thenewmovies.core.database.dao.MovieDetailDao
import com.practice.thenewmovies.core.database.dao.ReviewDao
import com.practice.thenewmovies.core.database.model.asExternalModel
import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import com.practice.thenewmovies.core.network.MoviesNetworkDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal const val TTL_MS = 24L * 60 * 60 * 1000
private const val SEARCH_PAGE_SIZE = 20

@Singleton
internal class OfflineFirstMoviesRepository @Inject constructor(
    private val network: MoviesNetworkDataSource,
    private val movieDao: MovieDao,
    private val movieDetailDao: MovieDetailDao,
    private val castDao: CastDao,
    private val reviewDao: ReviewDao,
    private val clock: Clock,
    @Dispatcher(MoviesDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : MoviesRepository {

    override fun getMovies(category: MovieCategory): Flow<List<Movie>> =
        movieDao.getByCategory(category.name)
            .map { entities -> entities.map { it.asExternalModel() } }

    override fun getMovieDetail(movieId: Int): Flow<MovieDetail?> =
        movieDetailDao.getById(movieId).map { it?.asExternalModel() }

    override fun getCast(movieId: Int): Flow<List<Cast>> =
        castDao.getByMovieId(movieId).map { entities -> entities.map { it.asExternalModel() } }

    override fun getReviews(movieId: Int): Flow<List<Review>> =
        reviewDao.getByMovieId(movieId).map { entities -> entities.map { it.asExternalModel() } }

    override fun searchMoviesPaged(query: String): Flow<PagingData<Movie>> =
        Pager(PagingConfig(pageSize = SEARCH_PAGE_SIZE, enablePlaceholders = false)) {
            MoviePagingSource(network = network, query = query)
        }.flow

    override suspend fun refresh(category: MovieCategory): Boolean = withContext(ioDispatcher) {
        if (isFresh(movieDao.getSyncedAt(category.name))) return@withContext true

        runCatching {
            val now = clock.nowMillis()
            val movies = network.getMovies(category)
            movieDao.replaceCategory(
                category = category.name,
                movies = movies.map { it.asEntity(category = category, syncedAt = now) },
            )
        }.isSuccess
    }

    override suspend fun refreshDetail(movieId: Int): Boolean = withContext(ioDispatcher) {
        if (isFresh(movieDetailDao.getSyncedAt(movieId))) return@withContext true

        runCatching {
            val now = clock.nowMillis()
            val detail = network.getMovieDetail(movieId)
            val cast = network.getCast(movieId)
            val reviews = network.getReviews(movieId)

            movieDetailDao.upsert(detail.asEntity(syncedAt = now))
            castDao.upsertAll(cast.map { it.asEntity(movieId) })
            reviewDao.upsertAll(reviews.map { it.asEntity(movieId) })
        }.isSuccess
    }

    private fun isFresh(syncedAt: Long?): Boolean =
        syncedAt != null && clock.nowMillis() - syncedAt < TTL_MS
}
```

- [ ] **Step 4: Write `paging/MoviePagingSource.kt`**

The test above compiles only once this exists.

```kotlin
package com.practice.thenewmovies.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.practice.thenewmovies.core.data.model.asExternalModel
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.network.MoviesNetworkDataSource
import java.io.IOException

internal class MoviePagingSource(
    private val network: MoviesNetworkDataSource,
    private val query: String,
) : PagingSource<Int, Movie>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> {
        val page = params.key ?: 1
        return try {
            val response = network.searchMovies(query = query, page = page)
            LoadResult.Page(
                data = response.results.map { it.asExternalModel() },
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page >= response.totalPages) null else page + 1,
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: retrofit2.HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
}
```

`retrofit2.HttpException` needs Retrofit on this module's compile classpath. Add to `core/data/build.gradle.kts`:

```kotlin
    implementation(libs.retrofit)
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 10 tests passed.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add core/data
git commit -m "feat(data): add offline-first movies repository with sync TTL"
```

---

### Task 9: Watchlist repository, network monitor and DI

**Files:**
- Create: `core/data/src/main/kotlin/com/practice/thenewmovies/core/data/repository/DefaultWatchlistRepository.kt`
- Create: `.../util/ConnectivityManagerNetworkMonitor.kt`
- Create: `.../di/DataModule.kt`
- Test: `core/data/src/test/kotlin/com/practice/thenewmovies/core/data/repository/DefaultWatchlistRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.database.dao.WatchlistDao
import com.practice.thenewmovies.core.database.entity.WatchlistEntity
import com.practice.thenewmovies.core.model.WatchlistMovie
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultWatchlistRepositoryTest {

    private val dao = mockk<WatchlistDao>(relaxed = true)
    private val repository = DefaultWatchlistRepository(dao)

    @Test
    fun `getWatchlist maps entities to models`() = runTest {
        every { dao.getAll() } returns flowOf(
            listOf(
                WatchlistEntity(
                    movieId = 7,
                    title = "Dune",
                    posterPath = null,
                    backdropPath = null,
                    releaseDate = "2021-10-22",
                    voteAverage = 7.8,
                    runtime = 155,
                    genre = "Science Fiction",
                    userRating = 4.5f,
                ),
            ),
        )

        val movies = repository.getWatchlist().first()

        assertEquals(7, movies.single().id)
        assertEquals(4.5f, movies.single().userRating)
    }

    @Test
    fun `addToWatchlist writes the mapped entity`() = runTest {
        repository.addToWatchlist(
            WatchlistMovie(
                id = 7,
                title = "Dune",
                posterPath = null,
                backdropPath = null,
                releaseDate = "2021-10-22",
                voteAverage = 7.8,
                runtime = 155,
                genre = "Science Fiction",
            ),
        )

        coVerify(exactly = 1) { dao.upsert(match { it.movieId == 7 }) }
    }

    @Test
    fun `setRating delegates to the dao`() = runTest {
        repository.setRating(movieId = 7, rating = 3f)

        coVerify(exactly = 1) { dao.updateRating(movieId = 7, rating = 3f) }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "*DefaultWatchlistRepositoryTest*"`
Expected: compilation failure — `Unresolved reference: DefaultWatchlistRepository`.

- [ ] **Step 3: Write `DefaultWatchlistRepository.kt`**

```kotlin
package com.practice.thenewmovies.core.data.repository

import com.practice.thenewmovies.core.database.dao.WatchlistDao
import com.practice.thenewmovies.core.database.model.asEntity
import com.practice.thenewmovies.core.database.model.asExternalModel
import com.practice.thenewmovies.core.model.WatchlistMovie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultWatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao,
) : WatchlistRepository {

    override fun getWatchlist(): Flow<List<WatchlistMovie>> =
        watchlistDao.getAll().map { entities -> entities.map { it.asExternalModel() } }

    override fun isInWatchlist(movieId: Int): Flow<Boolean> = watchlistDao.existsById(movieId)

    override fun getRating(movieId: Int): Flow<Float?> = watchlistDao.getRating(movieId)

    override suspend fun addToWatchlist(movie: WatchlistMovie) =
        watchlistDao.upsert(movie.asEntity())

    override suspend fun removeFromWatchlist(movieId: Int) = watchlistDao.deleteById(movieId)

    override suspend fun setRating(movieId: Int, rating: Float) =
        watchlistDao.updateRating(movieId = movieId, rating = rating)
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, 13 tests passed.

- [ ] **Step 5: Write `util/ConnectivityManagerNetworkMonitor.kt`**

Ported from the reference repo, which already handles the multi-network case correctly.

```kotlin
package com.practice.thenewmovies.core.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ConnectivityManagerNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {

    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            channel.trySend(false)
            channel.close()
            return@callbackFlow
        }

        val availableNetworks = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                availableNetworks += network
                channel.trySend(true)
            }

            override fun onLost(network: Network) {
                availableNetworks -= network
                channel.trySend(availableNetworks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        channel.trySend(connectivityManager.isCurrentlyConnected())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .conflate()
        .distinctUntilChanged()
}

private fun ConnectivityManager.isCurrentlyConnected(): Boolean = activeNetwork
    ?.let(::getNetworkCapabilities)
    ?.let { capabilities ->
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } == true
```

- [ ] **Step 6: Write `di/DataModule.kt`**

One `@Binds` per concern, plus the single `Clock` provider. This is the only place the `internal` implementations are named.

```kotlin
package com.practice.thenewmovies.core.data.di

import com.practice.thenewmovies.core.data.repository.DefaultWatchlistRepository
import com.practice.thenewmovies.core.data.repository.MoviesRepository
import com.practice.thenewmovies.core.data.repository.OfflineFirstMoviesRepository
import com.practice.thenewmovies.core.data.repository.WatchlistRepository
import com.practice.thenewmovies.core.data.util.Clock
import com.practice.thenewmovies.core.data.util.ConnectivityManagerNetworkMonitor
import com.practice.thenewmovies.core.data.util.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    internal abstract fun bindsMoviesRepository(
        repository: OfflineFirstMoviesRepository,
    ): MoviesRepository

    @Binds
    internal abstract fun bindsWatchlistRepository(
        repository: DefaultWatchlistRepository,
    ): WatchlistRepository

    @Binds
    internal abstract fun bindsNetworkMonitor(
        monitor: ConnectivityManagerNetworkMonitor,
    ): NetworkMonitor
}

@Module
@InstallIn(SingletonComponent::class)
internal object ClockModule {

    @Provides
    fun providesClock(): Clock = Clock { System.currentTimeMillis() }
}
```

- [ ] **Step 7: Build and commit**

Run: `./gradlew :core:data:assembleDebug :core:data:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`

```bash
./gradlew spotlessApply
git add core/data
git commit -m "feat(data): add watchlist repository, network monitor and DI wiring"
```

---

### Task 10: `core:testing`

**Files:**
- Create: `core/testing/build.gradle.kts`
- Create: `core/testing/src/main/kotlin/com/practice/thenewmovies/core/testing/MainDispatcherRule.kt`
- Create: `.../data/TestMovies.kt`
- Create: `.../repository/TestMoviesRepository.kt`, `.../repository/TestWatchlistRepository.kt`, `.../repository/TestNetworkMonitor.kt`

Fakes ship in a real module (not `src/test`) so every feature module can consume them with a plain `testImplementation(projects.core.testing)` — which the feature-impl convention plugin already declares.

- [ ] **Step 1: Write `core/testing/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.themovies.android.library)
}

android {
    namespace = "com.practice.thenewmovies.core.testing"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)

    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    implementation(libs.paging.runtime)
}
```

- [ ] **Step 2: Write `MainDispatcherRule.kt`**

```kotlin
package com.practice.thenewmovies.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Replaces `Dispatchers.Main` so ViewModels under test can launch into `viewModelScope`. */
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

- [ ] **Step 3: Write `data/TestMovies.kt`**

```kotlin
package com.practice.thenewmovies.core.testing.data

import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.Genre
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import com.practice.thenewmovies.core.model.WatchlistMovie

val testMovies = listOf(
    Movie(
        id = 1,
        title = "Dune",
        overview = "Sand.",
        posterPath = "https://image.tmdb.org/t/p/w500/dune.jpg",
        backdropPath = null,
        releaseDate = "2021-10-22",
        voteAverage = 7.8,
        voteCount = 1000,
    ),
    Movie(
        id = 2,
        title = "Arrival",
        overview = "Squid ink.",
        posterPath = null,
        backdropPath = null,
        releaseDate = "2016-11-11",
        voteAverage = 7.6,
        voteCount = 900,
    ),
)

val testMovieDetail = MovieDetail(
    id = 1,
    title = "Dune",
    originalTitle = "Dune",
    originalLanguage = "en",
    overview = "Sand.",
    genres = listOf(Genre(id = 878, name = "Science Fiction")),
    posterPath = null,
    backdropPath = null,
    releaseDate = "2021-10-22",
    runtime = 155,
    status = "Released",
    video = false,
    voteAverage = 7.8,
    voteCount = 1000,
)

val testCast = listOf(
    Cast(castId = 1, character = "Paul", name = "Timothee Chalamet", profilePath = null),
)

val testReviews = listOf(
    Review(author = "critic", content = "Long.", createdAt = "2021-11-01", rating = 8f),
)

val testWatchlistMovie = WatchlistMovie(
    id = 1,
    title = "Dune",
    posterPath = null,
    backdropPath = null,
    releaseDate = "2021-10-22",
    voteAverage = 7.8,
    runtime = 155,
    genre = "Science Fiction",
)
```

- [ ] **Step 4: Write `repository/TestMoviesRepository.kt`**

Backed by `MutableStateFlow`s so a test drives the screen state directly, and it records refresh calls so a test can assert a screen refreshed on open.

```kotlin
package com.practice.thenewmovies.core.testing.repository

import androidx.paging.PagingData
import com.practice.thenewmovies.core.data.repository.MoviesRepository
import com.practice.thenewmovies.core.model.Cast
import com.practice.thenewmovies.core.model.Movie
import com.practice.thenewmovies.core.model.MovieCategory
import com.practice.thenewmovies.core.model.MovieDetail
import com.practice.thenewmovies.core.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class TestMoviesRepository : MoviesRepository {

    private val moviesByCategory =
        MovieCategory.entries.associateWith { MutableStateFlow(emptyList<Movie>()) }
    private val detail = MutableStateFlow<MovieDetail?>(null)
    private val cast = MutableStateFlow(emptyList<Cast>())
    private val reviews = MutableStateFlow(emptyList<Review>())
    private val searchResults = MutableStateFlow(PagingData.empty<Movie>())

    var refreshSucceeds: Boolean = true
    val refreshedCategories = mutableListOf<MovieCategory>()
    val refreshedDetailIds = mutableListOf<Int>()

    override fun getMovies(category: MovieCategory): Flow<List<Movie>> =
        moviesByCategory.getValue(category).asStateFlow()

    override fun getMovieDetail(movieId: Int): Flow<MovieDetail?> = detail.asStateFlow()

    override fun getCast(movieId: Int): Flow<List<Cast>> = cast.asStateFlow()

    override fun getReviews(movieId: Int): Flow<List<Review>> = reviews.asStateFlow()

    override fun searchMoviesPaged(query: String): Flow<PagingData<Movie>> =
        flowOf(searchResults.value)

    override suspend fun refresh(category: MovieCategory): Boolean {
        refreshedCategories += category
        return refreshSucceeds
    }

    override suspend fun refreshDetail(movieId: Int): Boolean {
        refreshedDetailIds += movieId
        return refreshSucceeds
    }

    fun emitMovies(category: MovieCategory, movies: List<Movie>) {
        moviesByCategory.getValue(category).value = movies
    }

    fun emitDetail(movieDetail: MovieDetail?) {
        detail.value = movieDetail
    }

    fun emitCast(value: List<Cast>) {
        cast.value = value
    }

    fun emitReviews(value: List<Review>) {
        reviews.value = value
    }

    fun emitSearchResults(movies: List<Movie>) {
        searchResults.value = PagingData.from(movies)
    }
}
```

- [ ] **Step 5: Write `repository/TestWatchlistRepository.kt`**

```kotlin
package com.practice.thenewmovies.core.testing.repository

import com.practice.thenewmovies.core.data.repository.WatchlistRepository
import com.practice.thenewmovies.core.model.WatchlistMovie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class TestWatchlistRepository : WatchlistRepository {

    private val movies = MutableStateFlow(emptyList<WatchlistMovie>())

    override fun getWatchlist(): Flow<List<WatchlistMovie>> = movies

    override fun isInWatchlist(movieId: Int): Flow<Boolean> =
        movies.map { list -> list.any { it.id == movieId } }

    override fun getRating(movieId: Int): Flow<Float?> =
        movies.map { list -> list.firstOrNull { it.id == movieId }?.userRating }

    override suspend fun addToWatchlist(movie: WatchlistMovie) {
        movies.value = movies.value.filterNot { it.id == movie.id } + movie
    }

    override suspend fun removeFromWatchlist(movieId: Int) {
        movies.value = movies.value.filterNot { it.id == movieId }
    }

    override suspend fun setRating(movieId: Int, rating: Float) {
        movies.value = movies.value.map {
            if (it.id == movieId) it.copy(userRating = rating) else it
        }
    }

    fun emit(value: List<WatchlistMovie>) {
        movies.value = value
    }
}
```

- [ ] **Step 6a: Write `repository/TestUserPreferencesRepository.kt`**

Add `api(projects.core.datastore)` to `core/testing/build.gradle.kts` first.

```kotlin
package com.practice.thenewmovies.core.testing.repository

import com.practice.thenewmovies.core.datastore.UserPreferences
import com.practice.thenewmovies.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestUserPreferencesRepository : UserPreferencesRepository {

    private val state = MutableStateFlow(UserPreferences())

    override val preferences: Flow<UserPreferences> = state

    override suspend fun setSelectedHomeTab(index: Int) {
        state.value = state.value.copy(selectedHomeTab = index)
    }
}
```

- [ ] **Step 6: Write `repository/TestNetworkMonitor.kt`**

```kotlin
package com.practice.thenewmovies.core.testing.repository

import com.practice.thenewmovies.core.data.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestNetworkMonitor : NetworkMonitor {

    private val online = MutableStateFlow(true)

    override val isOnline: Flow<Boolean> = online

    fun setOnline(isOnline: Boolean) {
        online.value = isOnline
    }
}
```

- [ ] **Step 7: Build and commit**

Run: `./gradlew :core:testing:assembleDebug`
Expected: `BUILD SUCCESSFUL`

```bash
./gradlew spotlessApply
git add core/testing
git commit -m "test(testing): add fakes and dispatcher rule"
```

---

### Task 11: Verify the slice

- [ ] **Step 1: Full build with unit tests**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL` — 16 unit tests across `:core:navigation`, `:core:common`, `:core:database` and `:core:data`.

- [ ] **Step 2: Instrumented tests**

Run: `./gradlew :core:database:connectedDebugAndroidTest`
Expected: `BUILD SUCCESSFUL`, 9 tests passed.

- [ ] **Step 3: Confirm no feature can reach the network directly**

Run: `./gradlew :core:data:dependencies --configuration debugCompileClasspath | grep -c "core:network"`
Expected: `1` — `core:network` is an `implementation` dependency of `core:data`, so it does not leak onto any feature's compile classpath.

- [ ] **Step 4: Formatting**

Run: `./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`

---

## Done when

- `MoviesRepository` and `WatchlistRepository` are the only data types features can see; both implementations are `internal`.
- Reads come from Room; the network is touched only by `refresh`, `refreshDetail` and search paging.
- `refresh` is provably skipped inside the 24-hour TTL (unit test) and provably retried after it.
- `core:testing` fakes compile against the real interfaces, so a signature change breaks the fakes at compile time.
