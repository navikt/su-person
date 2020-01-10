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
import io.ktor.http.HttpStatusCode
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import no.nav.su.person.nais.nais
import org.json.JSONObject
import org.slf4j.Logger
import java.net.URL

const val PERSON_PATH = "/person"
const val OIDC_ISSUER = "issuer"
const val OIDC_GROUP_CLAIM = "groups"
const val OIDC_JWKS_URI = "jwks_uri"

@KtorExperimentalAPI
fun Application.app(env: Environment = Environment()) {

   setUncaughtExceptionHandler(logger = log)

   val collectorRegistry = CollectorRegistry.defaultRegistry

   val jwkConfig = getJWKConfig(env.oidcConfigUrl)
   val jwkProvider = JwkProviderBuilder(URL(jwkConfig.getString(OIDC_JWKS_URI))).build()

   install(Authentication) {
      jwt {
         verifier(jwkProvider, jwkConfig.getString(OIDC_ISSUER))
         validate { credentials ->
            val groupsClaim = credentials.payload.getClaim(OIDC_GROUP_CLAIM).asList(String::class.java)
            if (env.oidcRequiredGroup in groupsClaim && env.oidcClientId in credentials.payload.audience) {
               JWTPrincipal(credentials.payload)
            } else {
               logInvalidCredentials(credentials)
               null
            }
         }
      }
   }

   install(MicrometerMetrics) {
      registry = PrometheusMeterRegistry(
         PrometheusConfig.DEFAULT,
         collectorRegistry,
         Clock.SYSTEM
      )
      meterBinders = listOf(
         ClassLoaderMetrics(),
         JvmMemoryMetrics(),
         JvmGcMetrics(),
         ProcessorMetrics(),
         JvmThreadMetrics(),
         LogbackMetrics()
      )
   }

   routing {
      authenticate {
         get(PERSON_PATH) {
            call.respond("hooha")
         }
      }
      nais(collectorRegistry)
   }
}

private fun Application.logInvalidCredentials(credentials: JWTCredential) {
   log.info(
      "${credentials.payload.getClaim("NAVident").asString()} with audience ${credentials.payload.audience} " +
         "is not authorized to use this app, denying access"
   )
}

private fun getJWKConfig(oidcConfigUrl: String): JSONObject {
   val (_, response, result) = oidcConfigUrl.httpGet().responseJson()
   if (response.statusCode != HttpStatusCode.OK.value) {
      throw RuntimeException("Could not get JWK config from url ${oidcConfigUrl}, got statuscode=${response.statusCode}")
   } else {
      return result.get().obj()
   }
}

private fun setUncaughtExceptionHandler(logger: Logger) {
   Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
      logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
   }
}
