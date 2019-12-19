package no.nav.su.person

import com.github.tomakehurst.wiremock.WireMockServer

const val oidcGroupUuid = "ENLANG-AZURE-OBJECT-GRUPPE-ID"
const val clientId = "clientId"

fun testEnvironment(wireMockServer: WireMockServer) = Environment(
   oidcConfigUrl = "${wireMockServer.baseUrl()}/config",
   oidcClientId = clientId,
   oidcRequiredGroup = oidcGroupUuid
)
