import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.6"
  id("org.openapi.generator") version "7.5.0"
  kotlin("plugin.spring") version "1.9.23"
  kotlin("plugin.jpa") version "1.9.23"
}

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable",
  )
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:0.2.4")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:3.1.1")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.7.3")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

  // Gov Notify client
  implementation("uk.gov.service.notify:notifications-java-client:5.0.1-RELEASE")

  // Test dependencies
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
  testImplementation("com.h2database:h2")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("net.javacrumbs.json-unit:json-unit:3.2.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.2.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-json-path:3.2.2")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.wiremock:wiremock-standalone:3.5.4")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    dependsOn("buildLocationsInsidePrisonApiModel", "buildActivitiesAppointmentsApiModel")
    kotlinOptions {
      jvmTarget = "21"
    }
  }
  withType<KtLintCheckTask> {
    // Under gradle 8 we must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildLocationsInsidePrisonApiModel", "buildActivitiesAppointmentsApiModel")
  }
  withType<KtLintFormatTask> {
    // Under gradle 8 we must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildLocationsInsidePrisonApiModel", "buildActivitiesAppointmentsApiModel")
  }
}

val configValues = mapOf(
  "dateLibrary" to "java8-localdatetime",
  "serializationLibrary" to "jackson",
  "enumPropertyNaming" to "original",
  "useSpringBoot3" to "true",
)

val buildDirectory: Directory = layout.buildDirectory.get()

tasks.register("buildLocationsInsidePrisonApiModel", GenerateTask::class) {
  generatorName.set("kotlin")
  inputSpec.set("openapi-specs/locations-inside-prison-api.json")
  outputDir.set("$buildDirectory/generated/locationsinsideprisonapi")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

tasks.register("buildActivitiesAppointmentsApiModel", GenerateTask::class) {
  generatorName.set("kotlin")
  inputSpec.set("openapi-specs/activities-appointments-api.json")
  outputDir.set("$buildDirectory/generated/activitiesappointmentsapi")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

val generatedProjectDirs = listOf("locationsinsideprisonapi", "activitiesappointmentsapi")

kotlin {
  generatedProjectDirs.forEach { generatedProject ->
    sourceSets["main"].apply {
      kotlin.srcDir("$buildDirectory/generated/$generatedProject/src/main/kotlin")
    }
  }
}

configure<KtlintExtension> {
  filter {
    generatedProjectDirs.forEach { generatedProject ->
      exclude { element ->
        element.file.path.contains("build/generated/$generatedProject/src/main/")
      }
    }
  }
}
