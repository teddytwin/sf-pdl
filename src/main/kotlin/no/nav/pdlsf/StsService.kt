package no.nav.pdlsf

import java.time.LocalDateTime
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.parse
import mu.KotlinLogging
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status

private val log = KotlinLogging.logger { }
private var cachedToken: StsAccessToken? = null

@ImplicitReflectionSerializer
private fun fetchNewToken(): StsAccessTokenBase = Http.client.invoke(
        (Request(Method.GET, ParamsFactory.p.stsUrl)
                .header("x-nav-apiKey", ParamsFactory.p.stsApiKey)
                .header("Authorization", "Basic ${ParamsFactory.p.credentials()}")
                .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toHeaderValue())
                .query("grant_type", "client_credentials"))
                .query("scope", "openid")
).let { response ->
    log.debug { response.toString() }
    when (response.status) {
        Status.OK -> json.parse<StsAccessToken>(response.bodyString())
        else -> {
            log.error { "Authorization request failed - ${response.toMessage()}" }
            InvalidStsAccessToken
        }
    }
}.also { token -> if (token is StsAccessToken) cachedToken = token }

@ImplicitReflectionSerializer
fun getStsToken(): StsAccessTokenBase =
        if (cachedToken == null || cachedToken.shouldRenew()) fetchNewToken()
        else cachedToken ?: fetchNewToken()

sealed class StsAccessTokenBase
object InvalidStsAccessToken : StsAccessTokenBase()
@Serializable
data class StsAccessToken(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("expires_in")
    val expiresIn: Int
) : StsAccessTokenBase() {
    @Serializable(with = IsoLocalDateTimeSerializer::class)
    val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expiresIn - 30L)
}

private fun StsAccessToken?.shouldRenew(): Boolean = this?.expirationTime?.isBefore(LocalDateTime.now()) ?: true
