package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.health

import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HealthCheckTest : IntegrationTestBase() {

  @Test
  fun `Health page reports ok`() {
    stubPingWithResponse(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
      .jsonPath("components.activitiesAppointmentsApi.status").isEqualTo("UP")
      .jsonPath("components.hmppsAuth.status").isEqualTo("UP")
      .jsonPath("components.locationsInsidePrisonApi.status").isEqualTo("UP")
      .jsonPath("components.manageUsersApi.status").isEqualTo("UP")
      .jsonPath("components.prisonerSearchApi.status").isEqualTo("UP")
      .jsonPath("components.nomisMappingApi.status").isEqualTo("UP")
      .jsonPath("components.db.status").isEqualTo("UP")
      .jsonPath("components.db.details.database").isEqualTo("PostgreSQL")
  }

  @Test
  fun `Health page reports down`() {
    stubPingWithResponse(404)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .jsonPath("components.activitiesAppointmentsApi.status").isEqualTo("DOWN")
      .jsonPath("components.hmppsAuth.status").isEqualTo("DOWN")
      .jsonPath("components.locationsInsidePrisonApi.status").isEqualTo("DOWN")
      .jsonPath("components.manageUsersApi.status").isEqualTo("DOWN")
      .jsonPath("components.prisonerSearchApi.status").isEqualTo("DOWN")
      .jsonPath("components.nomisMappingApi.status").isEqualTo("DOWN")
  }

  @Test
  fun `Health info reports version`() {
    stubPingWithResponse(200)

    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Matchers.startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)),
      )
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }
}
