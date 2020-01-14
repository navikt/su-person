package no.nav.su.person

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.*

class JwtStub(private val wireMockServer: WireMockServer? = null) {

   private val privateKey: RSAPrivateKey
   val publicKey: RSAPublicKey

   init {
      wireMockServer?.apply {
         val client = WireMock.create().port(wireMockServer.port()).build()
         WireMock.configureFor(client)
      }

      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(512)

      val keyPair = keyPairGenerator.genKeyPair()
      privateKey = keyPair.private as RSAPrivateKey
      publicKey = keyPair.public as RSAPublicKey
   }

   fun createTokenFor(
      subject: String = SUBJECT,
      groups: List<String> = listOf(AZURE_REQUIRED_GROUP),
      audience: String = AZURE_CLIENT_ID,
      expiresAt: Date = Date.from(Instant.now().plusSeconds(3600))
   ): String {
      val algorithm = Algorithm.RSA256(publicKey, privateKey)

      return JWT.create()
         .withIssuer(AZURE_ISSUER)
         .withAudience(audience)
         .withKeyId("key-1234")
         .withSubject(subject)
         .withArrayClaim("groups", groups.toTypedArray())
         .withExpiresAt(expiresAt)
         .sign(algorithm)
   }

   fun stubbedJwkProvider() = WireMock.get(WireMock.urlPathEqualTo("/jwks")).willReturn(
      WireMock.okJson(
         """
{
    "keys": [
        {
            "kty": "RSA",
            "alg": "RS256",
            "kid": "key-1234",
            "e": "${kotlinx.io.core.String(Base64.getEncoder().encode(publicKey.publicExponent.toByteArray()))}",
            "n": "${kotlinx.io.core.String(Base64.getEncoder().encode(publicKey.modulus.toByteArray()))}"
        }
    ]
}
""".trimIndent()
      )
   )

   fun stubbedConfigProvider() = WireMock.get(WireMock.urlPathEqualTo("/wellknown")).willReturn(
      WireMock.okJson(
         """
{
    "jwks_uri": "${wireMockServer?.baseUrl()}/jwks",
    "token_endpoint": "${wireMockServer?.baseUrl()}/token",
    "issuer": "$AZURE_ISSUER"
}
""".trimIndent()
      )
   )
}
