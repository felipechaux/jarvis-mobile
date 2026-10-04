package com.felipechaux.jarvis.core.network

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.core.model.Resource
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException

/** Runs a request and maps transport and HTTP failures to [AppError]. */
suspend inline fun <reified T> safeApiCall(block: () -> HttpResponse): Resource<T> = try {
    val response = block()
    when (response.status.value) {
        in 200..299 -> Resource.Success(response.body<T>())
        else -> Resource.Error(errorFor(response.status.value, response.bodyAsText()))
    }
} catch (e: CancellationException) {
    throw e
} catch (e: HttpRequestTimeoutException) {
    Resource.Error(AppError.Unreachable)
} catch (e: IOException) {
    Resource.Error(AppError.Unreachable)
} catch (e: Exception) {
    Resource.Error(AppError.Unknown(e))
}

fun errorFor(status: Int, body: String): AppError = when (status) {
    401 -> AppError.Unauthorized
    426 -> AppError.IncompatibleProtocol(body.take(200))
    else -> AppError.Server(status, body.take(200))
}
