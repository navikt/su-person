package no.nav.su.person.pdl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.jackson.responseObject
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.Accept
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.ContentType
import no.nav.su.person.sts.StsConsumer
import org.slf4j.LoggerFactory

const val NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
const val NAV_TEMA = "Tema"
const val SUP = "SUP"

class PdlConsumer(private val pdlUrl: String, private val systembruker: StsConsumer) {
   companion object {
      private val LOG = LoggerFactory.getLogger(PdlConsumer::class.java)
      private val jsonMapper = jacksonObjectMapper()
   }

   fun person(ident: String, autorisertSaksbehandler: String): PdlPerson? {

      val query = this::class.java.getResource("/hentPerson.graphql").readText()
      val pdlRequest = PdlRequest(query, Variables(ident = ident))

      val (_, _, result) = "$pdlUrl/graphql".httpPost()
         .header(Authorization, "Bearer $autorisertSaksbehandler")
         .header(NAV_CONSUMER_TOKEN, "Bearer ${systembruker.token()}")
         .header(NAV_TEMA, SUP)
         .header(Accept, Json)
         .header(ContentType, Json)
         .body(jsonMapper.writeValueAsString(pdlRequest))
         .responseObject<PdlResponse<PdlHentPerson>>()

      val res = result.get()
      res.errors?.let {
         val error = "Exception while getting person from PDL: $it"
         LOG.info(error)
         throw RuntimeException(error)
      } ?: return res.data?.hentPerson
   }
}
