package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.officialvisits

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.officialvisits.model.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.officialvisits.model.OfficialVisitSummarySearchResponse
import java.time.LocalDate

@Component
class OfficialVisitsClient(private val officialVisitsApiWebClient: WebClient) {
  fun findOfficialVisits(prisonCode: String, startDate: LocalDate, endDate: LocalDate): List<OfficialVisitSummarySearchResponse> = run {
    officialVisitsApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/official-visit/prison/{prisonCode}/find-by-criteria")
          .queryParam("page", "0")
          .queryParam("size", "100")
          .build(prisonCode)
      }
      .bodyValue(
        OfficialVisitSummarySearchRequest(
          startDate = startDate,
          endDate = endDate,
        ),
      )
      .retrieve()
      .bodyToMono<PagedResponse<OfficialVisitSummarySearchResponse>>()
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()?.content ?: emptyList()
  }

  data class PagedResponse<T>(val content: List<T>)
}
