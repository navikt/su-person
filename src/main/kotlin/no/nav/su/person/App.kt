package no.nav.su.person

import io.ktor.application.Application
import io.ktor.routing.routing
import no.nav.su.person.nais.nais

fun Application.app() {
   routing {
      nais()
   }
}
