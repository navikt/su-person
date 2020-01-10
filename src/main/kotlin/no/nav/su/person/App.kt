package no.nav.su.person

import com.auth0.jwk.JwkProviderBuilder
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.jackson.jackson
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import no.nav.su.person.nais.nais
import no.nav.su.person.pdl.PdlConsumer
import no.nav.su.person.sts.StsConsumer
import org.json.JSONObject
import org.slf4j.Logger
import java.net.URL

const val PERSON_PATH = "/person"

@KtorExperimentalAPI
fun Application.app(env: Environment = Environment()) {

   setUncaughtExceptionHandler(logger = log)

   val stsConsumer = StsConsumer(env.STS_URL, env.SRV_SUPSTONAD, env.SRV_SUPSTONAD_PWD)
   val pdlConsumer = PdlConsumer(env.PDL_URL, stsConsumer)

   val jwkConfig = getJWKConfig(env.AZURE_WELLKNOWN_URL)
   val jwkProvider = JwkProviderBuilder(URL(jwkConfig.getString("jwks_uri"))).build()

   install(Authentication) {
      jwt {
         verifier(jwkProvider, jwkConfig.getString("issuer"))
         validate { credentials ->
            val groupsClaim = credentials.payload.getClaim("groups").asList(String::class.java)
            if (env.AZURE_REQUIRED_GROUP in groupsClaim && env.AZURE_CLIENT_ID in credentials.payload.audience) {
               JWTPrincipal(credentials.payload)
            } else {
               logInvalidCredentials(credentials)
               null
            }
         }
      }
   }

   install(ContentNegotiation) {
      jackson {
      }
   }

   routing {
      authenticate {
         get(PERSON_PATH) {
            call.respond(OK, pdlConsumer.person(call.parameters["ident"]!!, call.request.header(Authorization)!!)!!)
         }
      }
      nais()
   }
}

private fun Application.logInvalidCredentials(credentials: JWTCredential) {
   log.info(
      "${credentials.payload.getClaim("NAVident").asString()} with audience ${credentials.payload.audience} " +
         "is not authorized to use this app, denying access"
   )
}

private fun getJWKConfig(wellKnownUrl: String): JSONObject {
   val (_, response, result) = wellKnownUrl.httpGet().responseJson()
   if (response.statusCode != OK.value) {
      throw RuntimeException("Could not get JWK config from url ${wellKnownUrl}, got statuscode=${response.statusCode}")
   } else {
      return result.get().obj()
   }
}

private fun setUncaughtExceptionHandler(logger: Logger) {
   Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
      logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
   }
}
