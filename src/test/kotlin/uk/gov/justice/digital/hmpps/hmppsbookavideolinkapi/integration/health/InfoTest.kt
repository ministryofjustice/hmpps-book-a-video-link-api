package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@TestPropertySource(properties = ["FEATURE_GREY_RELEASE_PRISONS=PVI", "FEATURE_MASTER_PUBLIC_PRIVATE_NOTES=true"])
class InfoTest : IntegrationTestBase() {

  @Test
  fun `Info page is accessible`() {
    webTestClient.get()
      .uri("/info")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("build.name").isEqualTo("hmpps-book-a-video-link-api")
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("build.version").value<String> {
        assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
      }
  }

  @Test
  fun `Info page reports feature toggles`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("featureToggles.FEATURE_MASTER_PUBLIC_PRIVATE_NOTES").value<String> {
        assertThat(it).isEqualTo("true")
      }
      .jsonPath("featureToggles.FEATURE_GREY_RELEASE_PRISONS").value<String> {
        assertThat(it).isEqualTo("PVI")
      }
  }
}
