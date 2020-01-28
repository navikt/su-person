package no.nav.su.person

internal sealed class Result
internal class Ok(val json: String) : Result()
internal class Feil(val httpCode: Int, val message: String) : Result()
