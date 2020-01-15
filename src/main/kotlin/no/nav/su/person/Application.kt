package no.nav.su.person

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.XRequestId
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.jackson.jackson
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.request.header
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
import no.nav.su.person.pdl.PdlConsumer
import no.nav.su.person.sts.StsConsumer
import org.json.JSONObject
import org.slf4j.*
import org.slf4j.event.Level
import java.net.URL

const val PERSON_PATH = "/person"
const val identParamName = "ident"

private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

@KtorExperimentalAPI
fun Application.superson(
   jwkConfig: JSONObject = getJWKConfig(fromEnvironment("azure.wellKnownUrl")),
   jwkProvider: JwkProvider = JwkProviderBuilder(URL(jwkConfig.getString("jwks_uri"))).build(),
   stsConsumer: StsConsumer = StsConsumer(fromEnvironment("integrations.sts.url"), fromEnvironment("serviceuser.username"), fromEnvironment("serviceuser.password")),
   pdlConsumer: PdlConsumer = PdlConsumer(fromEnvironment("integrations.pdl.url"), stsConsumer)
) {

   setUncaughtExceptionHandler(logger = log)

   val collectorRegistry = CollectorRegistry.defaultRegistry

   install(Authentication) {
      jwt {
         verifier(jwkProvider, jwkConfig.getString("issuer"))
         validate { credentials ->
            val groupsClaim = credentials.payload.getClaim("groups").asList(String::class.java)
            if (fromEnvironment("azure.requiredGroup") in groupsClaim && fromEnvironment("azure.clientId") in credentials.payload.audience) {
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

      install(ContentNegotiation) {
         jackson()
      }

      routing {
         authenticate {
            install(CallId) {
               header(XRequestId)
               generate { "invalid" }
               verify { callId: String ->
                  if (callId == "invalid") throw RejectedCallIdException(callId) else true
               }
            }
            install(CallLogging) {
               level = Level.INFO
               intercept(ApplicationCallPipeline.Monitoring) {
                  MDC.put(XRequestId, call.callId)
               }
            }
            get(PERSON_PATH) {
               call.parameters[identParamName]?.let { personIdent ->
                  val principal = (call.authentication.principal as JWTPrincipal).payload
                  sikkerLogg.info("${principal.subject} gjør oppslag på person $personIdent")
                  call.respond(OK, pdlConsumer.person(call.parameters[identParamName]!!, call.request.header(Authorization)!!)!!)
               } ?: call.respond(HttpStatusCode.BadRequest, "query param '${identParamName}' må oppgis")

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

@KtorExperimentalAPI
fun Application.fromEnvironment(path: String): String = environment.config.property(path).getString()

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)
