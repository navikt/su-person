package no.nav.su.person.pdl

import com.google.gson.Gson
import no.nav.su.person.PdlStub.Companion.pdlHentPersonOkJson
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class PdlResponsesTest {
   @Test
   fun `PDL response ok skal gi http 200`() {
      val response = Gson().fromJson(pdlHentPersonOkJson, PdlResponse::class.java)
      assertEquals(200, response.statusCode())
   }

   @Test
   fun `PDL response unauthenticated skal gi http 401`() {
      val response = Gson().fromJson(pdlErrorWithHttpCode("unauthenticated"), PdlResponse::class.java)
      assertEquals(401, response.statusCode())
   }

   @Test
   fun `PDL response unauthorized skal gi http 403`() {
      val response = Gson().fromJson(pdlErrorWithHttpCode("unauthorized"), PdlResponse::class.java)
      assertEquals(403, response.statusCode())
   }

   @Test
   fun `PDL response not found skal gi http 404`() {
      val response = Gson().fromJson(pdlErrorWithHttpCode("not_found"), PdlResponse::class.java)
      assertEquals(404, response.statusCode())
   }

   @Test
   fun `PDL response bad request skal gi http 400`() {
      val response = Gson().fromJson(pdlErrorWithHttpCode("bad_request"), PdlResponse::class.java)
      assertEquals(400, response.statusCode())
   }

   @Test
   fun `PDL response bad request skal gi http 500`() {
      val response = Gson().fromJson(pdlErrorWithHttpCode("server_error"), PdlResponse::class.java)
      assertEquals(500, response.statusCode())
   }
}

fun pdlErrorWithHttpCode(code: String): String {
   return """
         {
                    "errors": [
                      {
                        "message": "some message",
                        "locations": [
                          {
                            "line": 2,
                            "column": 3
                          }
                        ],
                        "path": [
                          "hentPerson"
                        ],
                        "extensions": {
                          "code": "$code",
                          "classification": "ExecutionAborted"
                        }
                      }
                    ],
                    "data": {
                      "hentPerson": null
                    }
                  }
      """.trimIndent()
}
