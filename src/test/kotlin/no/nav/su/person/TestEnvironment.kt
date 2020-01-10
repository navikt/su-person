package no.nav.su.person

const val TEST_IDENT = "12345678910"
const val AZURE_ISSUER = "azure"
const val AZURE_REQUIRED_GROUP = "su-gruppa"
const val AZURE_CLIENT_ID = "clientId"
const val SRV_SUPSTONAD = "srv-supstonad"
const val SRV_SUPSTONAD_PWD = "srv-supstonad-pwd"
const val SUBJECT = "enSaksbehandler"

fun testEnvironment(baseUrl: String) = Environment(
   AZURE_WELLKNOWN_URL = "$baseUrl/wellknown",
   AZURE_CLIENT_ID = AZURE_CLIENT_ID,
   AZURE_REQUIRED_GROUP = AZURE_REQUIRED_GROUP,
   SRV_SUPSTONAD = SRV_SUPSTONAD,
   SRV_SUPSTONAD_PWD = SRV_SUPSTONAD_PWD,
   STS_URL = baseUrl,
   PDL_URL = baseUrl
)
