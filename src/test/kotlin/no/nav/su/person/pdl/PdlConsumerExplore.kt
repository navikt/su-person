package no.nav.su.person.pdl

import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response

fun main() {
   FuelManager.instance.client = object : Client {
      override fun executeRequest(request: Request): Response {
         println(String(request.body.toByteArray()))
         return Response.error()
      }

   }

   val pdl = PdlConsumer(
      "http://localhost:4321",
      object : TokenProvider {
         override fun token() = "a token"
      }
   )
   val person = pdl.person("en person", "en saksbehandler")
   println("I am $person")
}
