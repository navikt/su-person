package no.nav.su.person

import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant.now
import java.util.Date.from
import kotlin.test.assertEquals

@KtorExperimentalAPI
internal class AuthenticationTest {

   @Test
   fun `hent person krever autentisering`() {
      withTestApplication({
         testEnv()
         usingMocks()
      }) {
         withCallId(Get, PERSON_PATH)
      }.apply {
         assertEquals(Unauthorized, response.status())
      }
   }

   @Test
   fun `hent person ok med gyldig token`() {
      withTestApplication({
         testEnv()
         usingMocks()
      }) {
         withCallId(Get, "$PERSON_PATH?ident=$TEST_IDENT") {
            addHeader(Authorization, "Bearer ${jwtStub.createTokenFor()}")
         }
      }.apply {
         assertEquals(OK, response.status())
      }
   }

   @Test
   fun `forespørsel uten påkrevet audience skal svare med 401`() {
      val token = jwtStub.createTokenFor(audience = "wrong_audience")

      withTestApplication({
         testEnv()
         usingMocks()
      }) {
         withCallId(Get, PERSON_PATH) {
            addHeader(Authorization, "Bearer $token")
         }
      }.apply {
         Assertions.assertEquals(Unauthorized, response.status())
      }
   }

   @Test
   fun `forespørsel uten medlemskap i påkrevet gruppe skal svare med 401`() {
      val token = jwtStub.createTokenFor(groups = listOf("WRONG_GROUP_UUID"))

      withTestApplication({
         testEnv()
         usingMocks()
      }) {
         withCallId(Get, PERSON_PATH) {
            addHeader(Authorization, "Bearer $token")
         }
      }.apply {
         Assertions.assertEquals(Unauthorized, response.status())
      }
   }

   @Test
   fun `forespørsel med utgått token skal svare med 401`() {
      val token = jwtStub.createTokenFor(expiresAt = from(now().minusSeconds(1)))

      withTestApplication({
         testEnv()
         usingMocks()
      }) {
         withCallId(Get, PERSON_PATH) {
            addHeader(Authorization, "Bearer $token")
         }
      }.apply {
         Assertions.assertEquals(Unauthorized, response.status())
      }
   }
}


