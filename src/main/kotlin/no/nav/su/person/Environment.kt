package no.nav.su.person

data class Environment(
   val SRV_SUPSTONAD: String = getEnvVar("username"),
   val SRV_SUPSTONAD_PWD: String = getEnvVar("password"),
   val STS_URL: String = "http://security-token-service",
   val AZURE_WELLKNOWN_URL: String = getEnvVar("AZURE_WELLKNOWN_URL"),
   val AZURE_CLIENT_ID: String = getEnvVar("AZURE_CLIENT_ID"),
   val AZURE_REQUIRED_GROUP: String = getEnvVar("AZURE_REQUIRED_GROUP"),
   val PDL_URL: String = "http://pdl-api"
)

private fun getEnvVar(varName: String) = getOptionalEnvVar(varName) ?: throw Exception("mangler verdi for $varName")

private fun getOptionalEnvVar(varName: String): String? = System.getenv(varName)
