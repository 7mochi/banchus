package pe.nanamochi.banchus.util

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import pe.nanamochi.banchus.domain.error.DatabaseError
import pe.nanamochi.banchus.domain.error.StorageError
import pe.nanamochi.banchus.domain.error.StorageWriteError

inline fun <V> runDatabaseCatching(block: () -> V): Result<V, DatabaseError> {
    return runCatching(block).mapError { DatabaseError(it.message) }
}

inline fun <V> runStorageCatching(block: () -> V): Result<V, StorageError> {
    return runCatching(block).mapError { StorageWriteError(it.message) }
}
