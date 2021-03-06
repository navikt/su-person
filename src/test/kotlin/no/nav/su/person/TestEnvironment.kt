package no.nav.su.person

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpHeaders.XCorrelationId
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockk
import no.nav.su.person.pdl.PDLSvarTolk
import no.nav.su.person.pdl.PdlConsumer
import no.nav.su.person.sts.StsConsumer
import org.json.JSONObject
import java.util.*

const val TEST_IDENT = "12345678910"
const val AZURE_ISSUER = "azure"
const val AZURE_REQUIRED_GROUP = "su-gruppa"
const val AZURE_CLIENT_ID = "clientId"
const val AZURE_WELL_KNOWN_URL = "/well-known"
const val SUBJECT = "enSaksbehandler"
const val DEFAULT_CALL_ID = "callId"
const val SERVICEUSER_USERNAME = "srvsupstonad"
const val SERVICEUSER__PASSWORD = "supersecret"

@KtorExperimentalAPI
fun Application.testEnv(wireMockServer: WireMockServer? = null) {
   val baseUrl = wireMockServer?.baseUrl() ?: ""
   (environment.config as MapApplicationConfig).apply {
      put("integrations.sts.url", baseUrl)
      put("serviceuser.username", SERVICEUSER_USERNAME)
      put("serviceuser.password", SERVICEUSER__PASSWORD)
      put("integrations.pdl.url", baseUrl)
      put("azure.requiredGroup", AZURE_REQUIRED_GROUP)
      put("azure.clientId", AZURE_CLIENT_ID)
      put("azure.wellKnownUrl", "$baseUrl$AZURE_WELL_KNOWN_URL")
      put("issuer", AZURE_ISSUER)
   }
}

val jwtStub = JwtStub()
@KtorExperimentalAPI
internal fun Application.usingMocks(
   jwkConfig: JSONObject = mockk(relaxed = true),
   jwkProvider: JwkProvider = mockk(relaxed = true),
   stsMock: StsConsumer = mockk(relaxed = true),
   pdlConsumer: PdlConsumer = mockk(relaxed = true)
) {
   val e = Base64.getEncoder().encodeToString(jwtStub.publicKey.publicExponent.toByteArray())
   val n = Base64.getEncoder().encodeToString(jwtStub.publicKey.modulus.toByteArray())
   every {
      jwkProvider.get(any())
   }.returns(Jwk("key-1234", "RSA", "RS256", null, emptyList(), null, null, null, mapOf("e" to e, "n" to n)))
   every {
      jwkConfig.getString("issuer")
   }.returns(AZURE_ISSUER)
   every {
      pdlConsumer.person(any(), any())
   }.returns(PDLSvarTolk(PdlStub.pdlHentPersonOkJson).resultat)

   superson(
      jwkConfig = jwkConfig,
      jwkProvider = jwkProvider,
      auth = stsMock,
      pdlConsumer = pdlConsumer
   )
}

fun TestApplicationEngine.withCorrelationId(
   method: HttpMethod,
   uri: String,
   setup: TestApplicationRequest.() -> Unit = {}
): TestApplicationCall {
   return handleRequest(method, uri) {
      addHeader(XCorrelationId, DEFAULT_CALL_ID)
      setup()
   }
}
