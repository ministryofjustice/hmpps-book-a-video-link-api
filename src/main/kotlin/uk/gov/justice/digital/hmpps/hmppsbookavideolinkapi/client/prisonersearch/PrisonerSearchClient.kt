package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.stream.Collectors
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.attributesearch.Request as AttributeSearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.attributesearch.Response as AttributeSearchResponse

@Component
class PrisonerSearchClient(private val prisonerSearchApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun filterPrisonerNumbersByName(
    prisonerNumbers: Set<String>,
    firstName: String?,
    lastName: String?,
    batchSize: Int = 1000,
  ): Set<String> {
    if (prisonerNumbers.isEmpty()) return emptySet()

    require(firstName != null || lastName != null) {
      "Either first name or last name must be set"
    }

    require(batchSize in 1..1000) {
      "Batch size must be between 1 and 1000"
    }

    return prisonerNumbers.chunked(batchSize)
      .stream()
      .map {
        prisonerSearchApiWebClient
          .post()
          .uri { builder ->
            builder.path("/attribute-search")
              .queryParam("size", batchSize)
              .queryParam("responseFields", "prisonerNumber")
              .build()
          }
          .bodyValue(
            AttributeSearchRequest.filterPrisonerNumbersByName(
              prisonerNumbers = it,
              firstName = firstName,
              lastName = lastName,
            ),
          )
          .retrieve()
          .bodyToMono<AttributeSearchResponse>()
          .block()
      }
      .flatMap { it?.content?.stream() }
      .map { it.prisonerNumber }
      .collect(Collectors.toSet())
  }

  fun getPrisoner(prisonerNumber: String): Prisoner? = prisonerSearchApiWebClient
    .get()
    .uri("/prisoner/{prisonerNumber}", prisonerNumber)
    .retrieve()
    .bodyToMono(Prisoner::class.java)
    .doOnError { error -> log.info("Error looking up prisoner by prisoner number $prisonerNumber in prisoner search client", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  fun findByPrisonerNumbers(prisonerNumbers: Set<String>, batchSize: Int = 1000): List<Prisoner> {
    if (prisonerNumbers.isEmpty()) return emptyList()

    require(batchSize in 1..1000) {
      "Batch size must be between 1 and 1000"
    }

    return prisonerNumbers.chunked(batchSize).flatMap {
      prisonerSearchApiWebClient.post()
        .uri("/prisoner-search/prisoner-numbers")
        .bodyValue(PrisonerNumbers(it))
        .retrieve()
        .bodyToMono<List<Prisoner>>()
        .block() ?: emptyList()
    }
  }
}

data class PrisonerNumbers(val prisonerNumbers: List<String>)

@Component
class PrisonerValidator(val prisonerSearchClient: PrisonerSearchClient) {
  fun validatePrisonerAtPrison(prisonerNumber: String, prisonCode: String): Prisoner = prisonerSearchClient.getPrisoner(prisonerNumber)?.takeUnless { prisoner -> prisoner.prisonId != prisonCode }
    ?: throw ValidationException("Prisoner $prisonerNumber not found at prison $prisonCode")
}

// Ideally this model would be generated and not hard coded, however at time of writing the Open API generator did not
// play nicely with the JSON api spec for this service
data class Prisoner(
  val prisonerNumber: String,
  val prisonId: String? = null,
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val bookingId: String? = null,
  val lastPrisonId: String? = null,
  val lastMovementTypeCode: String? = null,
)
