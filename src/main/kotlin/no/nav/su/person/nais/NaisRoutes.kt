package no.nav.su.person.nais

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.Route
import io.ktor.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

const val IS_ALIVE_PATH = "/isalive"
const val IS_READY_PATH = "/isready"
const val METRICS_PATH = "/metrics"

fun Route.nais(collectorRegistry: CollectorRegistry) {
   get(IS_ALIVE_PATH) {
      call.respondText("ALIVE", ContentType.Text.Plain)
   }
   get(IS_READY_PATH) {
      call.respondText("READY", ContentType.Text.Plain)
   }
   get(METRICS_PATH) {
      val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()
      call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
         TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
      }
   }
}
