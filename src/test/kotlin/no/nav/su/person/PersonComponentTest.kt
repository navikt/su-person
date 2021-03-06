package no.nav.su.person

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@KtorExperimentalAPI
internal class PersonComponentTest {

   @Test
   fun `hent person ok med gyldig token`() {
      withTestApplication({
         testEnv(wireMockServer)
         superson()
      }) {
         stubFor(pdlStub.hentPerson(PdlStub.pdlHentPersonOkJson))
         withCorrelationId(Get, "$PERSON_PATH?ident=$TEST_IDENT") {
            addHeader(Authorization, "Bearer ${jwtStub.createTokenFor()}")
         }
      }.apply {
         assertEquals(OK, response.status())
         val person = JSONObject(response.content!!)
         assertEquals("OLA", person.getString("fornavn"))
         assertEquals("NORMANN", person.getString("etternavn"))
      }
   }

   @Test
   fun `hent person med feil fra PDL`() {
      withTestApplication({
         testEnv(wireMockServer)
         superson()
      }) {
         stubFor(pdlStub.hentPerson(PdlStub.pdlUnauthenticatedJson))
         withCorrelationId(Get, "$PERSON_PATH?ident=$TEST_IDENT") {
            addHeader(Authorization, "Bearer ${jwtStub.createTokenFor()}")
         }
      }.apply {
         assertEquals(Unauthorized, response.status())
      }
   }

   @Test
   fun `handles actual http errors from PDL`() {
      withTestApplication({
         testEnv(wireMockServer)
         superson()
      }) {
         stubFor(pdlStub.httpError(BadRequest, "this is some error message"))
         withCorrelationId(Get, "$PERSON_PATH?ident=$TEST_IDENT") {
            addHeader(Authorization, "Bearer ${jwtStub.createTokenFor()}")
         }
      }.apply {
         assertEquals(BadRequest, response.status())
         assertEquals("this is some error message", response.content)
      }
   }

   companion object {
      private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
      private val jwtStub by lazy {
         JwtStub(wireMockServer)
      }
      private val stsStub = StsStub()
      private val pdlStub = PdlStub()


      @BeforeAll
      @JvmStatic
      fun start() {
         wireMockServer.start()
         stubFor(jwtStub.stubbedJwkProvider())
         stubFor(jwtStub.stubbedConfigProvider())
         stubFor(stsStub.stubbedSTS())
      }

      @AfterAll
      @JvmStatic
      fun stop() {
         wireMockServer.stop()
      }
   }
}
