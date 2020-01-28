package no.nav.su.person.pdl

import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.Accept
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.ContentType
import no.nav.su.person.Feil
import no.nav.su.person.sts.StsConsumer
import org.slf4j.LoggerFactory

const val NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
const val NAV_TEMA = "Tema"
const val SUP = "SUP"

internal class PdlConsumer(private val pdlUrl: String, private val systembruker: StsConsumer) {
   companion object {
      //Graphql is picky about json, this will format and escape the json string correctly
      private val gson = Gson()
      private val LOG = LoggerFactory.getLogger(PdlConsumer::class.java)
   }

   internal fun person(ident: String, autorisertSaksbehandler: String): Any {

      val query = this::class.java.getResource("/hentPerson.graphql").readText()
      val pdlRequest = PdlRequest(query, Variables(ident = ident))

      val (_, _, result) = "$pdlUrl/graphql".httpPost()
         .header(Authorization, "Bearer $autorisertSaksbehandler")
         .header(NAV_CONSUMER_TOKEN, "Bearer ${systembruker.token()}")
         .header(NAV_TEMA, SUP)
         .header(Accept, Json)
         .header(ContentType, Json)
         .body(gson.toJson(pdlRequest))
         .responseString()
      return result.fold(
         { PDLSvarTolk(it).resultat },
         {
            val errorMessage = it.response.body().asString(Json.toString())
            val statusCode = it.response.statusCode
            LOG.debug("Kall mot PDL feilet, statuskode: $statusCode, feilmelding: $errorMessage");
            Feil(statusCode, errorMessage)
         }
      )
   }
}
