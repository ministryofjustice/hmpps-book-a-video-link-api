package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonerSearchApiMockServer

class PrisonerSearchClientTest {

  private val server = PrisonerSearchApiMockServer().also { it.start() }
  private val client = PrisonerSearchClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching prisoner`() {
    server.stubGetPrisoner("123456", "RSI")

    client.getPrisoner("123456") isEqualTo Prisoner("123456", "RSI")
  }

  @Test
  fun `should get matching prisoner at prison`() {
    server.stubGetPrisoner("123456", "RSI")

    client.getPrisonerAtPrison("123456", "RSI") isEqualTo Prisoner("123456", "RSI")
  }

  @Test
  fun `should throw entity not found for prisoner at prison when prison does not match`() {
    server.stubGetPrisoner("123456", "MDI")

    val error = assertThrows<EntityNotFoundException> { client.getPrisonerAtPrison("123456", "RSI") }

    error.message isEqualTo "Prisoner 123456 not found at prison RSI"
  }

  @Test
  fun `should throw entity not found for prisoner at prison when prisoner not found`() {
    val error = assertThrows<EntityNotFoundException> { client.getPrisonerAtPrison("123456", "RSI") }

    error.message isEqualTo "Prisoner 123456 not found at prison RSI"
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}
