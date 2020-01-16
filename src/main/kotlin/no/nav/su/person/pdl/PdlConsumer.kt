package no.nav.su.person.pdl

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.Accept
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.ContentType
import no.nav.su.person.sts.StsConsumer

const val NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
const val NAV_TEMA = "Tema"
const val SUP = "SUP"

class PdlConsumer(private val pdlUrl: String, private val systembruker: StsConsumer) {
   companion object {
      private val gson = Gson()
   }

   fun person(ident: String, autorisertSaksbehandler: String): PdlPerson {

      val query = this::class.java.getResource("/hentPerson.graphql").readText()
      val pdlRequest = PdlRequest(query, Variables(ident = ident))

      val (_, _, result) = "$pdlUrl/graphql".httpPost()
         .header(Authorization, "Bearer $autorisertSaksbehandler")
         .header(NAV_CONSUMER_TOKEN, "Bearer ${systembruker.token()}")
         .header(NAV_TEMA, SUP)
         .header(Accept, Json)
         .header(ContentType, Json)
         .body(gson.toJson(pdlRequest))
         .responseObject<PdlResponse<PdlHentPerson>>()

      result.get().errors?.let {
         throw RuntimeException("Exception while getting person from PDL: $it")
      }
      return result.get().data!!.hentPerson!!
   }
}
