package no.nav.su.person

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.github.kittinunf.fuel.httpGet
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpHeaders.XRequestId
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.request.header
import io.ktor.request.path
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
import no.nav.su.person.nais.IS_ALIVE_PATH
import no.nav.su.person.nais.IS_READY_PATH
import no.nav.su.person.nais.METRICS_PATH
import no.nav.su.person.nais.nais
import no.nav.su.person.pdl.FeilFraPDL
import no.nav.su.person.pdl.PdlConsumer
import no.nav.su.person.pdl.PersonFraPDL
import no.nav.su.person.sts.StsConsumer
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level
import java.net.URL

const val PERSON_PATH = "/person"
const val identParamName = "ident"

private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

@KtorExperimentalAPI
internal fun Application.superson(
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
            filter { call ->
               listOf(IS_ALIVE_PATH, IS_READY_PATH, METRICS_PATH).none {
                  call.request.path().startsWith(it)
               }
            }
         }
         get(PERSON_PATH) {
            call.parameters[identParamName]?.let { personIdent ->
               val principal = (call.authentication.principal as JWTPrincipal).payload
               sikkerLogg.info("${principal.subject} gjør oppslag på person $personIdent")
               when(val svar = pdlConsumer.person(call.parameters[identParamName]!!, call.request.header(HttpHeaders.Authorization)!!)){
                  is PersonFraPDL -> call.respond(OK, svar.toJson())
                  is FeilFraPDL -> call.respond(HttpStatusCode.fromValue(svar.httpCode), "Kan ikke hente person")
               }
            } ?: call.respond(HttpStatusCode.BadRequest, "query param '$identParamName' må oppgis")
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

fun getJWKConfig(wellKnownUrl: String): JSONObject {
   val (_, _, result) = wellKnownUrl.httpGet().responseString()
   return result.fold(
      { JSONObject(it) },
      { throw RuntimeException("Could not get JWK config from url ${wellKnownUrl}, error:${it}") }
   )
}

private fun setUncaughtExceptionHandler(logger: Logger) {
   Thread.currentThread().setUncaughtExceptionHandler { thread, err ->
      logger.error("uncaught exception in thread ${thread.name}: ${err.message}", err)
   }
}

@KtorExperimentalAPI
fun Application.fromEnvironment(path: String): String = environment.config.property(path).getString()

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)
