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
   private val alleNavn = person.getJSONArray("navn").map {
      PDLNavn(it as JSONObject)
   }
   private val navnData: PDLNavn =
      alleNavn.find { it.kildeComparable == "FREG" } ?: alleNavn.find { it.kildeComparable == "PDL" } ?: alleNavn.first()

   val fornavn: String = navnData.fornavn
   val mellomnavn: String = navnData.mellomnavn
   val etternavn: String = navnData.etternavn

   fun toJson(): String = """
      {
         "fornavn": "$fornavn",
         "mellomnavn": "$mellomnavn",
         "etternavn": "$etternavn"
      }
   """.trimIndent()
}

private class PDLNavn(source: JSONObject) {
   val fornavn: String = source.getString("fornavn")
   val mellomnavn: String = source.optString("mellomnavn") ?: ""
   val etternavn: String = source.getString("etternavn")
   val kilde: String = source.getJSONObject("metadata").getString("master")
   val kildeComparable = kilde.toUpperCase()
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
