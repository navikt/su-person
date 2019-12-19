package no.nav.su.person

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant.now
import java.util.Date.from
import kotlin.test.assertEquals

@KtorExperimentalAPI
internal class AuthenitcationComponentTest {

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
   fun `hent person krever autentisering`() {
      withTestApplication({
         app(testEnvironment(wireMockServer))
      }) {
         handleRequest(Get, PERSON_PATH)
      }.apply {
         assertEquals(Unauthorized, response.status())
      }
   }

   @Test
   fun `hent person ok med gyldig token`() {
      val token = jwtStub.createTokenFor()
      withTestApplication({
         app(testEnvironment(wireMockServer))
      }) {
         handleRequest(Get, PERSON_PATH) {
            addHeader(Authorization, "Bearer $token")
         }
      }.apply {
         assertEquals(OK, response.status())
         assertEquals("hooha", response.content)
      }
   }

   @Test
   fun `forespørsel uten påkrevet audience skal svare med 401`() {
      val token = jwtStub.createTokenFor(audience = "wrong_audience")

      withTestApplication({
         app(testEnvironment(wireMockServer))
      }) {
         handleRequest(Get, PERSON_PATH) {
            addHeader(Authorization, "Bearer $token")
         }
      }.apply {
         Assertions.assertEquals(Unauthorized, response.status())
      }
   }

   @Test
   fun `forespørsel uten medlemskap i påkrevet gruppe skal svare med 401`() {
      val token = jwtStub.createTokenFor(groups = listOf("WRONG_GROUP_UUID"))

      withTestApplication({
         app(testEnvironment(wireMockServer))
      }) {
         handleRequest(Get, PERSON_PATH) {
            addHeader(Authorization, "Bearer $token")
         }
      }.apply {
         Assertions.assertEquals(Unauthorized, response.status())
      }
   }

   @Test
   fun `forespørsel med utgått token skal svare med 401`() {
      val token = jwtStub.createTokenFor(expiresAt = from(now().minusSeconds(1)))

      withTestApplication({
         app(testEnvironment(wireMockServer))
      }) {
         handleRequest(Get, PERSON_PATH) {
            addHeader(Authorization, "Bearer $token")
         }
      }.apply {
         Assertions.assertEquals(Unauthorized, response.status())
      }
   }
}


