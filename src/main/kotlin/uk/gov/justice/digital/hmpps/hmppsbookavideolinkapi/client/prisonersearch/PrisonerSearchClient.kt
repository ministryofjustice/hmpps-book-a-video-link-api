package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch

import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

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

// TODO add additional fields as and when needed e.g. ACTIVE/NOT ACTIVE in prison
data class Prisoner(
  val prisonerNumber: String,
  val prisonId: String?,
)
