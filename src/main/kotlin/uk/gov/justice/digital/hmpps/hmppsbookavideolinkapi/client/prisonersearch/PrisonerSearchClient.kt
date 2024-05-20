package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch

import jakarta.validation.ValidationException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Component
class PrisonerSearchClient(private val prisonerSearchApiWebClient: WebClient) {

  fun getPrisoner(prisonerNumber: String): Prisoner? =
    prisonerSearchApiWebClient
      .get()
      .uri("/prisoner/{prisonerNumber}", prisonerNumber)
      .retrieve()
      .bodyToMono(Prisoner::class.java)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()
}

@Component
class PrisonerValidator(val prisonerSearchClient: PrisonerSearchClient) {
  fun validatePrisonerAtPrison(prisonerNumber: String, prisonerCode: String): Prisoner =
    prisonerSearchClient.getPrisoner(prisonerNumber)?.takeUnless { prisoner -> prisoner.prisonId != prisonerCode }
      ?: throw ValidationException("Prisoner $prisonerNumber not found at prison $prisonerCode")
}

// Ideally this model would be generated and not hard coded, however at time of writing the Open API generator did not
// play nicely with the JSON api spec for this service
// TODO add additional fields as and when needed e.g. ACTIVE/NOT ACTIVE in prison
data class Prisoner(
  val prisonerNumber: String,
  val prisonId: String?,
)
