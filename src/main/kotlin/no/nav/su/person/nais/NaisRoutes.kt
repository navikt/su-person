package no.nav.su.person.nais

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get

const val IS_ALIVE_PATH = "isalive"
const val IS_READY_PATH = "isready"

fun Route.nais() {
   get(IS_ALIVE_PATH) {
      call.respondText("ALIVE", ContentType.Text.Plain)
   }
   get(IS_READY_PATH) {
      call.respondText("READY", ContentType.Text.Plain)
   }
}
