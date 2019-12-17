package no.nav.su.person

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


fun testServer(test: ApplicationEngine.() -> Unit) = embeddedServer(Netty, 8088) {
   app()
}.apply {
   val stopper = GlobalScope.launch {
      delay(10000)
      stop(0, 0, TimeUnit.SECONDS)
   }
   start(wait = false)
   try {
      test()
   } finally {
      stopper.cancel()
      stop(0, 0, TimeUnit.SECONDS)
   }
}
