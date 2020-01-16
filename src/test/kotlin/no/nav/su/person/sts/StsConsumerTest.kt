package no.nav.su.person.sts

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import no.nav.su.person.SRV_SUPSTONAD
import no.nav.su.person.SRV_SUPSTONAD_PWD
import no.nav.su.person.StsStub
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals

internal class StsConsumerTest {

   @Test
   fun `should deserialize to Token instance`() {
      stubFor(stsStub.validStsToken())
      assertEquals("default", stsClient().token())
   }

   @Test
   fun `throws exception unable to get token`() {
      stubFor(WireMock.get(WireMock.urlPathEqualTo("/rest/v1/sts/token")).willReturn(WireMock.badRequest()))
      assertThrows<RuntimeException> {
         stsClient().token()
      }
   }

   @Test
   fun `should get new token when expired`() {
      stubFor(stsStub.validStsToken().willReturn(ok(shortLivedStsToken))
         .inScenario("token expiry")
         .whenScenarioStateIs(STARTED)
         .willSetStateTo("token expired"))

      stubFor(stsStub.validStsToken()
         .inScenario("token expiry")
         .whenScenarioStateIs("token expired"))

      val client = stsClient()
      val shortlived = client.token()
      val default = client.token()
      assertNotEquals(shortlived, default)
      assertEquals("default", default)
   }

   private fun stsClient() = StsConsumer(wireMockServer.baseUrl(), SRV_SUPSTONAD, SRV_SUPSTONAD_PWD)

   companion object {
      val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
      private val stsStub by lazy { StsStub() }

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

private val shortLivedStsToken = """{
  "access_token": "short lived",
  "token_type": "Bearer",
  "expires_in": 5
}""".trimIndent()

