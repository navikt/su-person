package no.nav.su.person

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod.Companion.Get
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
         WireMock.stubFor(jwtStub.stubbedJwkProvider())
         WireMock.stubFor(jwtStub.stubbedConfigProvider())
         WireMock.stubFor(stsStub.stubbedSTS())
         WireMock.stubFor(pdlStub.hentPerson(PdlStub.pdlHentPersonOkJson))
      }

      @AfterAll
      @JvmStatic
      fun stop() {
         wireMockServer.stop()
      }
   }

   @Test
   fun `hent person ok med gyldig token`() {
      withTestApplication({
         testEnv(wireMockServer)
         superson()
      }) {
         withCallId(Get, "$PERSON_PATH?ident=$TEST_IDENT") {
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
         WireMock.stubFor(pdlStub.hentPerson(PdlStub.pdlUnauthenticatedJson))
         withCallId(Get, "$PERSON_PATH?ident=$TEST_IDENT") {
            addHeader(Authorization, "Bearer ${jwtStub.createTokenFor()}")
         }
      }.apply {
         assertEquals(Unauthorized, response.status())
      }
   }
}
