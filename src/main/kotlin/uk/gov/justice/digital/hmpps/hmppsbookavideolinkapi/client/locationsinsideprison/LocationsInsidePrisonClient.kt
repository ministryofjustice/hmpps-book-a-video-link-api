package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison

import jakarta.validation.ValidationException
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.extensions.isActive
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.extensions.isAtPrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Component
class LocationsInsidePrisonClient(private val locationsInsidePrisonApiWebClient: WebClient) {

  fun getLocationsByKeys(keys: Set<String>): List<Location> = locationsInsidePrisonApiWebClient.post()
    .uri("/locations/keys")
    .bodyValue(keys)
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()
}

@Component
class LocationValidator(private val locationsInsidePrisonClient: LocationsInsidePrisonClient) {

  fun validatePrisonLocation(prisonCode: String, locationKey: String) {
    validatePrisonLocations(prisonCode, setOf(locationKey))
  }

  fun validatePrisonLocations(prisonCode: String, locationKeys: Set<String>) {
    val maybeFoundLocations = locationsInsidePrisonClient.getLocationsByKeys(locationKeys).associateBy { it.key }

    if (maybeFoundLocations.isEmpty()) {
      validationError("The following ${if (locationKeys.size == 1) "location was" else "locations were"} not found $locationKeys")
    }

    val mayBeMissingLocations = maybeFoundLocations.keys.filterNot { locationKeys.contains(it) }

    if (mayBeMissingLocations.isNotEmpty()) {
      validationError("The following ${if (mayBeMissingLocations.size == 1) "location was" else "locations were"} not found $mayBeMissingLocations")
    }

    val maybeLocationsAtDifferentPrison = maybeFoundLocations.values.filterNot { it.isAtPrison(prisonCode) }.map { it.key }

    if (maybeLocationsAtDifferentPrison.isNotEmpty()) {
      validationError("The following ${if (maybeLocationsAtDifferentPrison.size == 1) "location is" else "locations are"} not at prison code $prisonCode $maybeLocationsAtDifferentPrison")
    }

    val maybeInactiveLocations = maybeFoundLocations.values.filterNot { it.isActive() }.map { it.key }

    if (maybeInactiveLocations.isNotEmpty()) {
      validationError("The following ${if (maybeInactiveLocations.size == 1) "location is" else "locations are"} not active at prison code $prisonCode $maybeInactiveLocations")
    }
  }

  private fun validationError(message: String) {
    throw ValidationException(message)
  }
}
