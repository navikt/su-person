package no.nav.su.person

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import io.ktor.http.HttpHeaders
import no.nav.su.person.pdl.*

class PdlStub {
   fun hentPerson(json: String): MappingBuilder {
      val query = this::class.java.getResource("/hentPerson.graphql").readText()
      val pdlRequest = PdlRequest(query, Variables(TEST_IDENT))

      return WireMock.post(WireMock.urlPathEqualTo("/graphql"))
         .withRequestBody(WireMock.equalTo(pdlRequest.toJson()))
         .withHeader(HttpHeaders.Authorization, WireMock.containing("Bearer"))
         .withHeader(NAV_CONSUMER_TOKEN, WireMock.equalTo("Bearer $STS_TOKEN"))
         .withHeader(NAV_TEMA, WireMock.equalTo(SUP))
         .withHeader(HttpHeaders.Accept, WireMock.equalTo("application/json"))
         .withHeader(HttpHeaders.ContentType, WireMock.equalTo("application/json"))
         .willReturn(WireMock.okJson(json))
   }

   companion object {
      val pdlHentPersonOkJson = """
      {
                 "data": {
                   "hentPerson": {
                     "navn": [
                       {
                         "fornavn": "OLA",
                         "etternavn": "NORMANN"
                       }
                     ]
                   }
                 }
               }
   """.trimIndent()

      val pdlUnauthenticatedJson = """
         {
                    "errors": [
                      {
                        "message": "Ikke autentisert",
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
                          "code": "unauthenticated",
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
}
