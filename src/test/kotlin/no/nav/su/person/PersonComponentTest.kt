package no.nav.su.person

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@KtorExperimentalAPI
class PersonComponentTest {

   companion object {
      const val myGroup = "myGroup"
      const val clientId = "clientId"

      val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
      val jwtStub by lazy {
         JwtStub("azure", wireMockServer)
      }

      fun testEnvironment(wireMockServer: WireMockServer) = Environment(
         oidcConfigUrl = "${wireMockServer.baseUrl()}/config",
         clientId = clientId,
         requiredGroup = myGroup
      )

      @BeforeAll
      @JvmStatic
      fun start() {
         wireMockServer.start()
         WireMock.stubFor(jwtStub.stubbedJwkProvider())
         WireMock.stubFor(jwtStub.stubbedConfigProvider())
      }

      @AfterAll
      @JvmStatic
      fun stop() {
         wireMockServer.stop()
      }

   }

   @Test
   fun `hent person krever autentisering`() {
      withTestApplication({
         app(testEnvironment(wireMockServer))
      }) {
         handleRequest(HttpMethod.Get, personPath)
      }.apply {
         assertEquals(HttpStatusCode.Unauthorized, response.status())
      }
   }

   @Test
   fun `hent person ok med gyldig token`() {
      val token = jwtStub.createTokenFor("enSaksbehandler", listOf(myGroup), clientId)
      withTestApplication({
         app(testEnvironment(wireMockServer))
      }) {
         handleRequest(HttpMethod.Get, personPath) {
            addHeader(HttpHeaders.Authorization, "Bearer $token")
         }
      }.apply {
         assertEquals(HttpStatusCode.OK, response.status())
         assertEquals("hooha", response.content)
      }
   }
}
