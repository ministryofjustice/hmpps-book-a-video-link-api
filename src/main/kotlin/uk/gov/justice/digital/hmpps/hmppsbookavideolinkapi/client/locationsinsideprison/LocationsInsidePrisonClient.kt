package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison

import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import java.util.UUID

class LocationsInsidePrisonClient(private val locationsInsidePrisonApiWebClient: WebClient) {

  fun getLocation(locationId: UUID): Location? = locationsInsidePrisonApiWebClient.get()
    .uri("/locations/{id}", locationId)
    .retrieve()
    .bodyToMono(Location::class.java)
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()
}
