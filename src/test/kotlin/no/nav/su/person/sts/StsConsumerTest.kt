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

internal class StsConsumerTest {

   @Test
   fun `should deserialize to Token instance`() {
      stubFor(stsRequestMapping.willReturn(ok(defaultToken)))
      assertEquals("default", stsClient().token())
   }

   @Test
   fun `should get new token when expired`() {
      stubFor(stsRequestMapping.willReturn(ok(shortLivedToken))
         .inScenario("token expiry")
         .whenScenarioStateIs(STARTED)
         .willSetStateTo("token expired"))

      stubFor(stsRequestMapping.willReturn(ok(defaultToken))
         .inScenario("token expiry")
         .whenScenarioStateIs("token expired"))

      val client = stsClient()
      val shortlived = client.token()
      val default = client.token()
      assertNotEquals(shortlived, default)
      assertEquals("default", default)
   }

   private fun stsClient() = StsConsumer(server.baseUrl(), "foo", "bar")

   companion object {
      val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

      @BeforeAll
      @JvmStatic
      fun start() {
         server.start()
      }

      @AfterAll
      @JvmStatic
      fun stop() {
         server.stop()
      }
   }

   @BeforeEach
   fun configure() {
      WireMock.configureFor(server.port())
   }
}

private val stsRequestMapping: MappingBuilder = WireMock.get(WireMock.urlPathEqualTo("/rest/v1/sts/token"))
   .withQueryParam("grant_type", WireMock.equalTo("client_credentials"))
   .withQueryParam("scope", WireMock.equalTo("openid"))
   .withBasicAuth("foo", "bar")
   .withHeader("Accept", WireMock.equalTo("application/json"))

private val defaultToken = """{
  "access_token": "default",
  "token_type": "Bearer",
  "expires_in": 3600
}""".trimIndent()

private val shortLivedToken = """{
  "access_token": "short lived",
  "token_type": "Bearer",
  "expires_in": 5
}""".trimIndent()

