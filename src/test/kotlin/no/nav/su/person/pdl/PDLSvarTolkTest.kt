package no.nav.su.person.pdl

import no.nav.su.person.PdlStub
import no.nav.su.person.PdlStub.Companion.pdlIdenter
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
            assertEquals("12345678910", svar.fnr)
            assertEquals("10987654321", svar.aktorId)
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

   @Test
   fun `velg person med master FREG hvis eksisterer`() {
      val svar: PersonFraPDL = PDLSvarTolk(PdlStub.pdlHentPersonOkJson).resultat as PersonFraPDL
      assertEquals("OLA", svar.fornavn)
      assertEquals("OTTO", svar.mellomnavn)
      assertEquals("NORMANN", svar.etternavn)
   }

   @Test
   fun `velg person med master PDL hvis FREG ikke eksisterer`() {
      val svar: PersonFraPDL = PDLSvarTolk("""
      {
                 "data": {
                   "hentPerson": {
                     "navn": [
                       {
                         "fornavn": "ALO",
                         "mellomnavn": "OTTO",
                         "etternavn": "NNAMRON",
                         "metadata": {
                           "master": "PDL"
                           }
                        }
                     ]
                   },
                   $pdlIdenter
                 }
               }
   """.trimIndent()).resultat as PersonFraPDL
      assertEquals("ALO", svar.fornavn)
      assertEquals("OTTO", svar.mellomnavn)
      assertEquals("NNAMRON", svar.etternavn)
   }

   @Test
   fun `håndterer valgfrie returverdier`() {
      val svar: PersonFraPDL = PDLSvarTolk("""
      {
                 "data": {
                   "hentPerson": {
                     "navn": [
                       {
                         "fornavn": "ALO",
                         "mellomnavn": null,
                         "etternavn": "NNAMRON",
                         "metadata": {
                           "master": "PDL"
                           }
                        }
                     ]
                   },
                   $pdlIdenter
                 }
               }
   """.trimIndent()).resultat as PersonFraPDL

      assertEquals("ALO", svar.fornavn)
      assertEquals("", svar.mellomnavn)
      assertEquals("NNAMRON", svar.etternavn)
   }

   @Test
   fun `ignorerer case når sammenligner datakilde for person (FREG vs Freg etc)`() {
      val svar: PersonFraPDL = PDLSvarTolk("""
      {
                 "data": {
                   "hentPerson": {
                     "navn": [
                        {
                         "fornavn": "DONT",
                         "mellomnavn": PICK,
                         "etternavn": "THIS",
                         "metadata": {
                           "master": "PDL"
                           }
                        },
                       {
                         "fornavn": "PICK",
                         "mellomnavn": null,
                         "etternavn": "ME",
                         "metadata": {
                           "master": "FrEg"
                           }
                        }
                     ]
                   },
                   $pdlIdenter
                 }
               }
   """.trimIndent()).resultat as PersonFraPDL

      assertEquals("PICK", svar.fornavn)
      assertEquals("ME", svar.etternavn)
   }
}
