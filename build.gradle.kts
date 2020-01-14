import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junitJupiterVersion = "5.6.0-M1"
val ktorVersion = "1.2.5"
val fuelVersion = "2.2.1"
val orgJsonVersion = "20180813"
val wireMockVersion = "2.23.2"
val micrometerRegistryPrometheusVersion = "1.3.2"
val mockkVersion = "1.9.3"
val kotlinJacksonVersion = "2.9.10"

plugins {
   id("org.jetbrains.kotlin.jvm") version "1.3.61"
}

repositories {
   jcenter()
   maven("https://dl.bintray.com/kotlin/ktor")
}

dependencies {
   implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
   implementation("io.ktor:ktor-server-netty:$ktorVersion")
   implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
   implementation("com.github.kittinunf.fuel:fuel-json:$fuelVersion")
   implementation("com.github.kittinunf.fuel:fuel-jackson:$fuelVersion")
   implementation("org.json:json:$orgJsonVersion")


   implementation("ch.qos.logback:logback-classic:1.2.3")
   implementation("net.logstash.logback:logstash-logback-encoder:5.2")
   implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
      exclude(group = "junit")
   }
   implementation("io.ktor:ktor-jackson:$ktorVersion")
   implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$kotlinJacksonVersion")

   implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
   implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")

   testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
      exclude(group = "junit")
      exclude(group = "org.eclipse.jetty") // conflicts with WireMock
   }
   testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
   testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
   testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
   testImplementation("com.github.tomakehurst:wiremock:$wireMockVersion") {
      exclude(group = "junit")
   }
   testImplementation("io.mockk:mockk:$mockkVersion")

}

tasks.withType<KotlinCompile> {
   kotlinOptions.jvmTarget = "12"
}

tasks.withType<Test> {
   useJUnitPlatform()
   testLogging {
      events("passed", "skipped", "failed")
   }
}

tasks.withType<Wrapper> {
   gradleVersion = "6.0.1"
}

tasks.named<Jar>("jar") {
   baseName = "app"

   manifest {
      attributes["Main-Class"] = "no.nav.su.person.ApplicationKt"
      attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
         it.name
      }
   }

   doLast {
      configurations.runtimeClasspath.get().forEach {
         val file = File("$buildDir/libs/${it.name}")
         if (!file.exists())
            it.copyTo(file)
      }
   }
}
