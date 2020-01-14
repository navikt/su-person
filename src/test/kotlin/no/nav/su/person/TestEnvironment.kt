package no.nav.su.person

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import io.ktor.application.Application
import io.ktor.http.HttpHeaders.XRequestId
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockk
import no.nav.su.person.pdl.PdlConsumer
import no.nav.su.person.sts.StsConsumer
import org.json.JSONObject
import java.util.*

const val TEST_IDENT = "12345678910"
const val AZURE_ISSUER = "azure"
const val AZURE_REQUIRED_GROUP = "su-gruppa"
const val AZURE_CLIENT_ID = "clientId"
const val SRV_SUPSTONAD = "srv-supstonad"
const val SRV_SUPSTONAD_PWD = "srv-supstonad-pwd"
const val SUBJECT = "enSaksbehandler"
const val DEFAULT_CALL_ID = "callId"

fun testEnvironment(baseUrl: String = "") = Environment(
   AZURE_WELLKNOWN_URL = "$baseUrl/wellknown",
   AZURE_CLIENT_ID = AZURE_CLIENT_ID,
   AZURE_REQUIRED_GROUP = AZURE_REQUIRED_GROUP,
   SRV_SUPSTONAD = SRV_SUPSTONAD,
   SRV_SUPSTONAD_PWD = SRV_SUPSTONAD_PWD,
   STS_URL = baseUrl,
   PDL_URL = baseUrl
)


val jwtStub = JwtStub()
@KtorExperimentalAPI
fun Application.usingMocks(
   environment: Environment = testEnvironment(),
   jwkConfig: JSONObject = mockk(relaxed = true),
   jwkProvider: JwkProvider = mockk(relaxed = true),
   stsConsumer: StsConsumer = mockk(relaxed = true),
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

   app(
      env = environment,
      jwkConfig = jwkConfig,
      jwkProvider = jwkProvider,
      stsConsumer = stsConsumer,
      pdlConsumer = pdlConsumer
   )
}

fun TestApplicationEngine.withCallId(
   method: HttpMethod,
   uri: String,
   setup: TestApplicationRequest.() -> Unit = {}
): TestApplicationCall {
   return handleRequest(method, uri) {
      addHeader(XRequestId, DEFAULT_CALL_ID)
      setup()
   }
}
