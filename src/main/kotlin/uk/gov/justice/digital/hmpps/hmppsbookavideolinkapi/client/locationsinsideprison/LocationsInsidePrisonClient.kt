package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison

import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location

class LocationsInsidePrisonClient(private val locationsInsidePrisonApiWebClient: WebClient) {

  fun getLocationsByKeys(keys: List<String>): List<Location> = keys.mapNotNull(::getLocationByKey)

  @Deprecated(message = "We are waiting on a post endpoint to avoid multiple location API calls")
  private fun getLocationByKey(key: String): Location? = locationsInsidePrisonApiWebClient.get()
    .uri("/locations/key/{key}", key)
    .retrieve()
    .bodyToMono(Location::class.java)
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()
}
