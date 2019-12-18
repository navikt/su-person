package no.nav.su.person

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import no.nav.su.person.nais.nais
import org.json.JSONObject
import java.net.URL

const val personPath = "/person"

@KtorExperimentalAPI
fun Application.app(env: Environment = Environment()) {

   val jwkConfig = getJWKConfig(env.oidcConfigUrl)
   val jwkProvider = getJWKProvider(jwkConfig)

   install(Authentication) {
      jwt {
         verifier(jwkProvider, jwkConfig["issuer"].toString())
         realm = applicationId()
         validate { credentials ->
            val groupsClaim = credentials.payload.getClaim("groups").asList(String::class.java)
            if (env.requiredGroup in groupsClaim && env.clientId in credentials.payload.audience) {
               JWTPrincipal(credentials.payload)
            } else {
               log.info(
                  "${credentials.payload.getClaim("NAVident").asString()} with audience ${credentials.payload.audience} " +
                     "is not authorized to use this app, denying access"
               )
               null
            }
         }
      }
   }
   routing {
      authenticate {
         get(personPath) {
            call.respond("hooha")
         }
      }
      nais()
   }
}

private fun getJWKProvider(jwkConfig: JSONObject): JwkProvider {
   val jwksUri = jwkConfig["jwks_uri"] ?: throw RuntimeException("Could not find JWKS URI in OIDC config")
   return JwkProviderBuilder(URL(jwksUri.toString())).build()
}

@KtorExperimentalAPI
private fun getJWKConfig(oidcConfigUrl: String): JSONObject {
   val (_, response, result) = oidcConfigUrl.httpGet().responseJson()
   if (response.statusCode != HttpStatusCode.OK.value) {
      throw RuntimeException("Could not get JWK config from provider")
   } else {
      return result.get().obj()
   }
}

@KtorExperimentalAPI
fun Application.applicationId() =
   this.environment.config.propertyOrNull("ktor.application.id")?.getString() ?: "Application"
