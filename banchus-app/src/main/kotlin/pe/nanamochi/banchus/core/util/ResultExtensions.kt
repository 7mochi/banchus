package pe.nanamochi.banchus.core.util

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import pe.nanamochi.banchus.core.error.DatabaseError
import pe.nanamochi.banchus.core.error.StorageError
import pe.nanamochi.banchus.core.error.StorageWriteError

inline fun <V> runDatabaseCatching(block: () -> V): Result<V, DatabaseError> {
    return runCatching(block).mapError { DatabaseError(it.message) }
}

inline fun <V> runStorageCatching(block: () -> V): Result<V, StorageError> {
    return runCatching(block).mapError { StorageWriteError(it.message) }
}
