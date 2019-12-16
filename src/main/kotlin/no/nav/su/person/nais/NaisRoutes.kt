package no.nav.su.person.nais

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.nais() {
   get("isalive") {
      call.respondText("ALIVE", ContentType.Text.Plain)
   }
   get("isready") {
      call.respondText("READY", ContentType.Text.Plain)
   }
}
