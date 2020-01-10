package no.nav.su.person

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo

class StsStub {
   fun validStsToken(): MappingBuilder = WireMock.get(WireMock.urlPathEqualTo("/rest/v1/sts/token"))
      .withQueryParam("grant_type", equalTo("client_credentials"))
      .withQueryParam("scope", equalTo("openid"))
      .withBasicAuth(SRV_SUPSTONAD, SRV_SUPSTONAD_PWD)
      .withHeader("Accept", equalTo("application/json"))
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


