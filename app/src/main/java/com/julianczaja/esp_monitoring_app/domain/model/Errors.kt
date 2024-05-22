package com.julianczaja.esp_monitoring_app.domain.model

import com.julianczaja.esp_monitoring_app.R
import com.juul.kable.BluetoothDisabledException
import com.juul.kable.ConnectionLostException
import com.juul.kable.ConnectionRejectedException
import com.juul.kable.NotConnectedException
import kotlinx.serialization.SerializationException
import okhttp3.Request
import okhttp3.internal.http2.ConnectionShutdownException
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import timber.log.Timber
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.format.DateTimeParseException


class NoInternetConnectionException : Exception()
class NotFoundException : Exception()
class GenericInternetException : Exception()
class GenericServerException : Exception()
class InternalAppException : Exception()

fun Throwable.getErrorMessageId(): Int = when (this) {
    is NoInternetConnectionException -> R.string.no_internet_connection_error_message
    is NotFoundException -> R.string.not_found_error_message
    is GenericServerException -> R.string.generic_server_error_message
    is GenericInternetException -> R.string.generic_internet_error_message
    is InternalAppException -> R.string.internal_app_error_message
    is ConnectionLostException -> R.string.bluetooth_connection_lost_error_message
    is ConnectionRejectedException -> R.string.bluetooth_connection_rejected_error_message
    is NotConnectedException -> R.string.bluetooth_connection_not_connected_error_message
    is BluetoothDisabledException -> R.string.bluetooth_disabled_error_message
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
                    return when (this.code()) {
                        404 -> Result.failure(NotFoundException())
                        500 -> Result.failure(GenericServerException())
                        else -> Result.failure(GenericInternetException())
                    }
                }
                body()?.let { body -> return Result.success(body) }

                @Suppress("UNCHECKED_CAST")
                return Result.success(Unit) as Result<R>
            }

            override fun onResponse(call: Call<R>, response: Response<R>) {
                callback.onResponse(this@ResultCall, Response.success(response.toResult()))
            }

            override fun onFailure(call: Call<R>, throwable: Throwable) {
                Timber.e("Result CallAdapter error: $throwable")
                val error = when (throwable) {
                    is ConnectException,
                    is SocketTimeoutException,
                    is ConnectionShutdownException,
                    -> NoInternetConnectionException()
                    is SerializationException,
                    is DateTimeParseException,
                    -> InternalAppException()
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
