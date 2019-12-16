package no.nav.su.person

import io.ktor.application.Application
import io.ktor.routing.get
import io.ktor.routing.routing


fun Application.testApp() {
   app()
   routing {
      get("/serverError") {
         throw IllegalArgumentException()
      }
   }
}
