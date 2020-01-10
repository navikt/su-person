package no.nav.su.person.pdl

data class PdlResponse<T>(
   val errors: List<PdlError>?,
   val data: T?
)

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


