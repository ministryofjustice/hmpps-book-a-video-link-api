package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison

import jakarta.validation.ValidationException
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.extensions.isActive
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.extensions.isAtPrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.CacheConfiguration
import java.util.UUID

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Component
class LocationsInsidePrisonClient(private val locationsInsidePrisonApiWebClient: WebClient) {

  fun getLocationById(id: UUID): Location? = locationsInsidePrisonApiWebClient.get()
    .uri("/locations/{id}", id)
    .retrieve()
    .bodyToMono(Location::class.java)
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  fun getLocationByKey(key: String): Location? = locationsInsidePrisonApiWebClient.get()
    .uri("/locations/key/{key}", key)
    .retrieve()
    .bodyToMono(Location::class.java)
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  fun getLocationsByKeys(keys: Set<String>): List<Location> = locationsInsidePrisonApiWebClient.post()
    .uri("/locations/keys")
    .bodyValue(keys)
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()

  @Cacheable(CacheConfiguration.NON_RESIDENTIAL_LOCATIONS_CACHE_NAME)
  fun getNonResidentialAppointmentLocationsAtPrison(prisonCode: String): List<Location> = locationsInsidePrisonApiWebClient.get()
    .uri("/locations/prison/{prisonCode}/non-residential-usage-type/APPOINTMENT", prisonCode)
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()?.filter(Location::leafLevel) ?: emptyList()

  @Cacheable(CacheConfiguration.VIDEO_LINK_LOCATIONS_CACHE_NAME)
  fun getVideoLinkLocationsAtPrison(prisonCode: String): List<Location> = locationsInsidePrisonApiWebClient.get()
    .uri("/locations/prison/{prisonCode}/location-type/VIDEO_LINK", prisonCode)
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()?.filter(Location::leafLevel) ?: emptyList()
}

@Component
class LocationValidator(private val locationsInsidePrisonClient: LocationsInsidePrisonClient) {

  fun validatePrisonLocation(prisonCode: String, locationKey: String): Location {
    val location = locationsInsidePrisonClient.getLocationByKey(locationKey)
    validate(prisonCode, setOf(locationKey), listOfNotNull(location))
    return location!!
  }

  fun validatePrisonLocations(prisonCode: String, locationKeys: Set<String>): List<Location> {
    val locations = locationsInsidePrisonClient.getLocationsByKeys(locationKeys)
    validate(prisonCode, locationKeys, locations)
    return locations
  }

  private fun validate(prisonCode: String, locationKeys: Set<String>, maybeLocations: List<Location>) {
    val maybeFoundLocations = maybeLocations.associateBy { it.key }

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
