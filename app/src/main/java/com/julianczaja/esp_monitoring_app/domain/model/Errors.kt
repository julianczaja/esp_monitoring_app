package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.R
import okhttp3.Request
import okhttp3.internal.http2.ConnectionShutdownException
import okio.Timeout
import retrofit2.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.ConnectException
import java.net.SocketTimeoutException


class NoInternetConnectionException : Exception()
class GenericInternetException : Exception()

fun Throwable.getErrorMessageId(): Int = when (this) {
    is NoInternetConnectionException -> R.string.no_internet_connection_error_message
    is GenericInternetException -> R.string.generic_internet_error_message
    else -> R.string.unknown_error_message
}

class ResultCallAdapterFactory : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {

        if (getRawType(returnType) != Call::class.java) return null
        check(returnType is ParameterizedType) { "Return type must be a parameterized type." }

        val responseType = getParameterUpperBound(0, returnType)
        if (getRawType(responseType) != Result::class.java) return null
        check(responseType is ParameterizedType) { "Response type must be a parameterized type." }

        val successType = getParameterUpperBound(0, responseType)
        return ResultCallAdapter<Any>(successType)
    }
}

class ResultCallAdapter<R>(
    private val successType: Type,
) : CallAdapter<R, Call<Result<R>>> {

    override fun responseType(): Type = successType

    override fun adapt(call: Call<R>): Call<Result<R>> = ResultCall(call)
}


class ResultCall<R>(private val delegate: Call<R>) : Call<Result<R>> {

    override fun enqueue(callback: Callback<Result<R>>) = delegate.enqueue(
        object : Callback<R> {

            private fun Response<R>.toResult(): Result<R> {
                if (!isSuccessful) {
                    return Result.failure(GenericInternetException())
                }
                body()?.let { body -> return Result.success(body) }

                @Suppress("UNCHECKED_CAST")
                return Result.success(Unit) as Result<R>
            }

            override fun onResponse(call: Call<R>, response: Response<R>) {
                callback.onResponse(this@ResultCall, Response.success(response.toResult()))
            }

            override fun onFailure(call: Call<R>, throwable: Throwable) {
                val error = when (throwable) {
                    is ConnectException,
                    is SocketTimeoutException,
                    is ConnectionShutdownException,
                    -> NoInternetConnectionException()
                    else -> GenericInternetException()
                }
                callback.onResponse(this@ResultCall, Response.success(Result.failure(error)))
            }
        }
    )

    override fun clone(): Call<Result<R>> = ResultCall(delegate)
    override fun execute(): Response<Result<R>> = throw UnsupportedOperationException("Suspend function should not be blocking.")
    override fun isExecuted() = delegate.isExecuted
    override fun cancel(): Unit = delegate.cancel()
    override fun isCanceled(): Boolean = delegate.isCanceled
    override fun request(): Request = delegate.request()
    override fun timeout(): Timeout = delegate.timeout()
}
