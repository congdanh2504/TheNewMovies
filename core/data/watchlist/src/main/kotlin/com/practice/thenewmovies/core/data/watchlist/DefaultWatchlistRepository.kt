/*
 * Copyright 2026 TheNewMovies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.practice.thenewmovies.core.data.watchlist

import com.practice.thenewmovies.core.data.auth.AuthRepository
import com.practice.thenewmovies.core.database.dao.WatchlistDao
import com.practice.thenewmovies.core.database.model.asEntity
import com.practice.thenewmovies.core.database.model.asExternalModel
import com.practice.thenewmovies.core.model.SessionState
import com.practice.thenewmovies.core.model.WatchlistMovie
import kotlinx.coroutines.CancellationException
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
        // runCatching is what makes an offline write succeed locally. The pendingSync flag is
        // the record that it has not been pushed, so nothing is lost by swallowing the exception
        // here -- unlike the movies repository, where a swallowed JsonDataException hid a real
        // bug (see this directory's README).
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
        // syncWatchlist() is best-effort by contract: every unsent local change is already
        // marked pendingSync, so a failure here -- including one from Room itself, not just the
        // network calls already wrapped below -- loses nothing. The next sign-in retries.
        // CancellationException must still propagate or structured concurrency breaks (see
        // AuthErrorMapping.toAuthError()); OutOfMemoryError and friends are not caught either.
        try {
            syncWatchlistOrThrow()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Swallowed on purpose -- see contract note above.
        }
    }

    private suspend fun syncWatchlistOrThrow() {
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

    // currentUserId() waits past SessionState.Loading rather than treating it as signed out. A
    // tap landing in the first frames after launch would otherwise silently do nothing.
    private suspend fun currentUserId(): String? =
        authRepository.sessionState.first { it !is SessionState.Loading }.userIdOrNull()

    private fun SessionState.userIdOrNull(): String? =
        (this as? SessionState.SignedIn)?.user?.id
}
