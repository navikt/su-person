package no.nav.su.person.pdl

import org.json.JSONObject

internal class PDLSvarTolk(pdlData: String) {
   val resultat: TolketSvar = pdlData.tolk()
}

private fun String.tolk(): TolketSvar {
   val json = JSONObject(this)
   return if (json.has("errors")) {
      FeilFraPDL(json.getJSONArray("errors").get(0) as JSONObject)
   } else {
      PersonFraPDL(json.getJSONObject("data").getJSONObject("hentPerson"))
   }
}

internal sealed class TolketSvar
internal class PersonFraPDL(person: JSONObject) : TolketSvar() {
   val fornavn: String = person.getJSONArray("navn").getJSONObject(0).getString("fornavn")
   val etternavn: String = person.getJSONArray("navn").getJSONObject(0).getString("etternavn")

   fun toJson(): String = """
      {
         "fornavn": "$fornavn",
         "etternavn": "$etternavn"
      }
   """.trimIndent()
}

internal class FeilFraPDL(error: JSONObject) : TolketSvar() {
   val httpCode: Int = error.getJSONObject("extensions").getString("code").httpKode
}

private val String.httpKode
   get() = when (this) {
      "unauthenticated" -> 401
      "unauthorized" -> 403
      "not_found" -> 404
      "bad_request" -> 400
      "server_error" -> 500
      else -> 500
   }
