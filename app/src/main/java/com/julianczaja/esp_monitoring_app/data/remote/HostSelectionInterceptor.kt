import com.julianczaja.esp_monitoring_app.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import java.io.IOException
import javax.inject.Inject

class HostSelectionInterceptor @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val host = runBlocking { appSettingsRepository.getBaseUrl().first().toHttpUrl() }
        var request = chain.request()

        host.let {
            val newUrl = request.url.newBuilder()
                .scheme(it.scheme)
                .host(it.host)
                .port(it.port)
                .build()

            request = request.newBuilder()
                .url(newUrl)
                .build()
        }
        return chain.proceed(request)
    }
}
