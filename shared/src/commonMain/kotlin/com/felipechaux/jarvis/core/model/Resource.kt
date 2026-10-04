package com.felipechaux.jarvis.core.model

sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val error: AppError) : Resource<Nothing>
}

inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> this
}

inline fun <T> Resource<T>.onSuccess(action: (T) -> Unit): Resource<T> {
    if (this is Resource.Success) action(data)
    return this
}

inline fun <T> Resource<T>.onError(action: (AppError) -> Unit): Resource<T> {
    if (this is Resource.Error) action(error)
    return this
}
