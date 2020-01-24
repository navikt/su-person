package no.nav.su.person.pdl

import no.nav.su.person.PdlStub
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class PDLSvarTolkTest {
   @Test
   fun `Ok svar fra PDL skal kunne parses som en person`() {
      val svar = PDLSvarTolk(PdlStub.pdlHentPersonOkJson).resultat

      when (svar) {
         is FeilFraPDL -> fail("Skulle vært PersonFraPDL")
         is PersonFraPDL -> {
            assertEquals("OLA", svar.fornavn)
            assertEquals("NORMANN", svar.etternavn)
         }
      }
   }

   @Test
   fun `Unauthenticated fra PDL skal gi feil-objekt fra tolken`() {
      val svar = PDLSvarTolk(PdlStub.pdlUnauthenticatedJson).resultat
      when (svar) {
         is PersonFraPDL -> fail("Skulle vært FeilFraPDL")
         is FeilFraPDL -> {
            assertEquals(401, svar.httpCode)
         }
      }
   }
}
