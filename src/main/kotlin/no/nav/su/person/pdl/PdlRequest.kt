package no.nav.su.person.pdl

data class PdlRequest(
   val query: String,
   val variables: Variables
) {
   fun toJson() = """
      {
         "query": "$query",
         "variables": "${variables.toJson()}"
      }
   """.trimIndent().replace('\n', ' ')
}

data class Variables(
   val ident: String,
   val navnHistorikk: Boolean = false
) {
   fun toJson() = """
      {
         "ident": "$ident",
         "navnHistorikk": "$navnHistorikk"
      }
   """.trimIndent()
}
