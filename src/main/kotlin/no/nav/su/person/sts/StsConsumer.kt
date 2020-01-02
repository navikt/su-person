package no.nav.su.person.sts

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.jackson.responseObject
import no.nav.su.person.sts.StsToken.Companion.isValid
import java.time.LocalDateTime

class StsConsumer(private val baseUrl: String, private val username: String, private val password: String) {
   private var stsToken: StsToken? = null

   fun token(): String {
      if (!isValid(stsToken)) {
         val (_, _, result) = "$baseUrl/rest/v1/sts/token?grant_type=client_credentials&scope=openid".httpGet()
            .authentication().basic(username, password)
            .header(mapOf("Accept" to "application/json"))
            .responseObject<StsToken>()

         stsToken = result.get();
      }
      return stsToken!!.accessToken
   }
}

data class StsToken(
   @JsonProperty("access_token") val accessToken: String,
   @JsonProperty("token_type") val tokenType: String,
   @JsonProperty("expires_in") val expiresIn: Int
) {
   val expirationTime = LocalDateTime.now().plusSeconds(expiresIn - 20L)

   companion object {
      fun isValid(token: StsToken?): Boolean {
         return when (token) {
            null -> false
            else -> !isExpired(token)
         }
      }

      private fun isExpired(token: StsToken) = token.expirationTime.isBefore(LocalDateTime.now())
   }
}
