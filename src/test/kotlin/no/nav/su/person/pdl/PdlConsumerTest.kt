package no.nav.su.person.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import no.nav.su.person.PdlStub
import no.nav.su.person.PdlStub.Companion.pdlHentPersonOkJson
import no.nav.su.person.PdlStub.Companion.pdlUnauthenticatedJson
import no.nav.su.person.TEST_IDENT
import no.nav.su.person.sts.StsConsumer
import org.junit.jupiter.api.*
import kotlin.test.assertEquals

internal class PdlConsumerTest {

   @Test
   fun `should get Person from PDL`() {
      stubFor(pdlStub.hentPerson(pdlHentPersonOkJson))
      val pdlPerson = pdlConsumer().person(TEST_IDENT, "Bearer token").navn[0]
      assertEquals("OLA", pdlPerson.fornavn)
      assertEquals("NORMANN", pdlPerson.etternavn)
   }

   @Test
   fun `should throw exception when error from PDL`() {
      stubFor(pdlStub.hentPerson(pdlUnauthenticatedJson))
      assertThrows<RuntimeException> { pdlConsumer().person(TEST_IDENT, "Bearer token") }
   }

   private fun pdlConsumer() = PdlConsumer(wireMockServer.baseUrl(), configureStsMock())

   private fun configureStsMock(): StsConsumer {
      val stsMock = mockk<StsConsumer>()
      every {
         stsMock.token()
      }.returns("default")
      return stsMock
   }

   companion object {
      val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
      private val pdlStub by lazy { PdlStub() }

      @BeforeAll
      @JvmStatic
      fun start() {
         wireMockServer.start()
      }

      @AfterAll
      @JvmStatic
      fun stop() {
         wireMockServer.stop()
      }
   }

   @BeforeEach
   fun configure() {
      WireMock.configureFor(wireMockServer.port())
   }
}
