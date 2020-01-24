package no.nav.su.person.pdl

data class PdlResponse<T>(
   val errors: List<PdlError>?,
   val data: T?
) {
   fun statusCode(): Int {
      if (errors == null) {
         return 200
      } else {
         return errors.first().extensions.let {
            when (it?.code) {
               "unauthenticated" -> 401
               "unauthorized" -> 403
               "not_found" -> 404
               "bad_request" -> 400
               "server_error" -> 500
               else -> 500
            }
         }
      }
   }
}

//Generic errors
data class PdlError(
   val message: String?,
   val locations: List<PdlErrorLocation>?,
   val path: List<String>?,
   val extensions: PdlErrorExtension?
)

data class PdlErrorLocation(
   val line: Int?,
   val column: Int?
)

data class PdlErrorExtension(
   val code: String?,
   val classification: String?
)

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


