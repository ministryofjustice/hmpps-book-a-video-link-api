import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "7.0.0"
  id("org.openapi.generator") version "7.11.0"
  kotlin("plugin.spring") version "2.1.10"
  kotlin("plugin.jpa") version "2.1.10"
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
  // Kotlin dependencies
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")

  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.1.1")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.2.2")

  // CSV dependencies
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.18.2")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.5")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

  // Gov Notify client
  implementation("uk.gov.service.notify:notifications-java-client:5.2.1-RELEASE")

  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.12.0")

  implementation("com.googlecode.libphonenumber:libphonenumber:8.13.54")

  // Test dependencies
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("net.javacrumbs.json-unit:json-unit:4.1.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-json-path:4.1.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:localstack:1.20.4")
  testImplementation("org.testcontainers:postgresql:1.20.4")
  testImplementation("org.wiremock:wiremock-standalone:3.11.0")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    dependsOn("buildLocationsInsidePrisonApiModel", "buildActivitiesAppointmentsApiModel", "buildPrisonApiModel", "buildManageUsersApiModel")
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
  withType<KtLintCheckTask> {
    // Under gradle 8 we must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildLocationsInsidePrisonApiModel", "buildActivitiesAppointmentsApiModel", "buildPrisonApiModel", "buildManageUsersApiModel")
  }
  withType<KtLintFormatTask> {
    // Under gradle 8 we must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildLocationsInsidePrisonApiModel", "buildActivitiesAppointmentsApiModel", "buildPrisonApiModel", "buildManageUsersApiModel")
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
tasks.register("buildPrisonApiModel", GenerateTask::class) {
  generatorName.set("kotlin")
  inputSpec.set("openapi-specs/prison-api.json")
  outputDir.set("$buildDirectory/generated/prisonapi")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}
tasks.register("buildManageUsersApiModel", GenerateTask::class) {
  generatorName.set("kotlin")
  inputSpec.set("openapi-specs/manage-users-api.json")
  outputDir.set("$buildDirectory/generated/manageusersapi")
  modelPackage.set("uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model")
  configOptions.set(configValues)
  globalProperties.set(mapOf("models" to ""))
}

val generatedProjectDirs = listOf("locationsinsideprisonapi", "activitiesappointmentsapi", "prisonapi", "manageusersapi")

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
