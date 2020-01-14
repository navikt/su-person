package no.nav.su.person.nais

import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import no.nav.su.person.usingMocks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@KtorExperimentalAPI
internal class NaisRoutesComponentTest {

   @Test
   fun naisRoutes() {
      withTestApplication({
         usingMocks()
      }) {
         handleRequest(Get, IS_ALIVE_PATH)
      }.apply {
         assertEquals(OK, response.status())
         assertEquals("ALIVE", response.content)
      }

      withTestApplication({
         usingMocks()
      }) {
         handleRequest(Get, IS_READY_PATH)
      }.apply {
         assertEquals(OK, response.status())
         assertEquals("READY", response.content)
      }

      withTestApplication({
         usingMocks()
      }) {
         handleRequest(Get, METRICS_PATH)
      }.apply {
         assertEquals(OK, response.status())
      }
   }
}
