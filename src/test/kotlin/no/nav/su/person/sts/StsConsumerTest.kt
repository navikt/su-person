package no.nav.su.person.sts

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class STSTest {

   @Test
   fun `should deserialize to Token instance`() {
      stubFor(stsStub.validStsToken())
      assertEquals("default", stsClient().token())
   }

   @Test
   fun `should get new token when expired`() {
      stubFor(
         stsStub.validStsToken().willReturn(ok(shortLivedStsToken))
            .inScenario("token expiry")
            .whenScenarioStateIs(STARTED)
            .willSetStateTo("token expired"))

      stubFor(
         stsStub.validStsToken()
            .inScenario("token expiry")
            .whenScenarioStateIs("token expired"))

      val client = stsClient()
      val shortlived = client.token()
      val default = client.token()
      assertNotEquals(shortlived, default)
      assertEquals("default", default)
   }

   private fun stsClient() = StsConsumer(wireMockServer.baseUrl(), "SRV_SUPSTONAD", "SRV_SUPSTONAD_PWD")

   companion object {
      val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
      private val stsStub by lazy { StsStub2() }

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

class StsStub2 {
   fun validStsToken(): MappingBuilder = WireMock.get(WireMock.urlPathEqualTo("/rest/v1/sts/token"))
      .withQueryParam("grant_type", WireMock.equalTo("client_credentials"))
      .withQueryParam("scope", WireMock.equalTo("openid"))
      .withBasicAuth("SRV_SUPSTONAD", "SRV_SUPSTONAD_PWD")
      .withHeader("Accept", WireMock.equalTo("application/json"))
      .willReturn(
         WireMock.okJson(defaultStsToken)
      )

   val defaultStsToken = """
      {
        "access_token": "default",
        "token_type": "Bearer",
        "expires_in": 3600
      }
   """
}

