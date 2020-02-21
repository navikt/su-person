package no.nav.su.person

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import io.ktor.http.HttpHeaders.Accept
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpStatusCode
import no.nav.su.person.pdl.NAV_CONSUMER_TOKEN
import no.nav.su.person.pdl.NAV_TEMA
import no.nav.su.person.pdl.SUP

private val validGraphQlJson = """{"query":"query(${'$'}ident: ID!, ${'$'}historikk: Boolean!, ${'$'}identGrupper: [IdentGruppe!]) {\n   hentPerson(ident: ${'$'}ident) {\n      navn(historikk: ${'$'}historikk) {\n         fornavn\n         mellomnavn\n         etternavn\n         metadata {\n            master\n         }\n      }\n   }\n   hentIdenter(ident: ${'$'}ident, grupper: ${'$'}identGrupper, historikk: ${'$'}historikk) {\n      identer {\n         ident\n         gruppe\n      }\n   }\n}\n\n","variables":{"ident":"12345678910","historikk":false,"identGrupper":["FOLKEREGISTERIDENT","AKTORID"]}}"""

class PdlStub {
   fun httpError(httpCode: HttpStatusCode, message: String): MappingBuilder {
      return WireMock.post(WireMock.urlPathEqualTo("/graphql"))
         .willReturn(WireMock.aResponse().withBody(message).withStatus(httpCode.value))
   }

   fun hentPerson(json: String): MappingBuilder {
      return WireMock.post(WireMock.urlPathEqualTo("/graphql"))
         .withRequestBody(WireMock.equalTo(validGraphQlJson))
         .withHeader(Authorization, WireMock.matching("^(?!\\bBearer\\b.*\\bBearer\\b)^\\bBearer\\b.*"))
         .withHeader(NAV_CONSUMER_TOKEN, WireMock.equalTo("Bearer $STS_TOKEN"))
         .withHeader(NAV_TEMA, WireMock.equalTo(SUP))
         .withHeader(Accept, WireMock.equalTo("application/json"))
         .withHeader(ContentType, WireMock.equalTo("application/json"))
         .willReturn(WireMock.okJson(json))
   }

   companion object {
      val pdlIdenter = """
         "hentIdenter": {
            "identer": [
               {
                  "ident": "12345678910",
                  "gruppe": "FOLKEREGISTERIDENT"
               },
               {
                  "ident": "10987654321",
                  "gruppe": "AKTORID"
               }
            ]
         }
      """.trimIndent()

      val pdlHentPersonOkJson = """
      {
                 "data": {
                   "hentPerson": {
                     "navn": [
                       {
                         "fornavn": "OLA",
                         "mellomnavn": "OTTO",
                         "etternavn": "NORMANN",
                         "metadata": {
                           "master": "FREG"
                           }
                       },
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
