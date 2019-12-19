package no.nav.su.person.nais

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import no.nav.su.person.JwtStub
import no.nav.su.person.app
import no.nav.su.person.testEnvironment
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@KtorExperimentalAPI
internal class NaisRoutesKtTest {

   companion object {
      private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
      private val jwtStub by lazy {
         JwtStub("azure", wireMockServer)
      }

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
   fun naisRoutes() {
      withTestApplication({
         app(testEnvironment(wireMockServer = wireMockServer))
      }) {
         handleRequest(Get, IS_ALIVE_PATH)
      }.apply {
         assertEquals(OK, response.status())
         assertEquals("ALIVE", response.content)
      }

      withTestApplication({
         app(testEnvironment(wireMockServer = wireMockServer))
      }) {
         handleRequest(Get, IS_READY_PATH)
      }.apply {
         assertEquals(OK, response.status())
         assertEquals("READY", response.content)
      }
   }
}
