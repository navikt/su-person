package no.nav.su.person.pdl

data class PdlResponse<T>(
   val errors: List<PdlError>?,
   val data: T?
) {
   fun statusCode(): Int {
      return errors?.httpKode() ?: 200
   }
}

private fun List<PdlError>.httpKode() = when {
   isEmpty() -> 200
   else -> first().httpKode
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

//Generic errors
data class PdlError(
   val message: String?,
   val locations: List<PdlErrorLocation>?,
   val path: List<String>?,
   val extensions: PdlErrorExtension?
) {
   val httpKode
      get() = when (extensions) {
         null -> 200
         else -> extensions.httpKode
      }
}

data class PdlErrorLocation(
   val line: Int?,
   val column: Int?
)

data class PdlErrorExtension(
   val code: String?,
   val classification: String?
) {
   val httpKode
      get() = when (code) {
         null -> 200
         else -> code.httpKode
      }
}

//Query hentPerson
data class PdlHentPerson(
   val hentPerson: PdlPerson?
)

data class PdlPerson(
   val navn: List<PdlPersonNavn>
)

data class PdlPersonNavn(
   val fornavn: String,
   val etternavn: String
)


