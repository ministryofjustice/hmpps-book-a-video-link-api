package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.CacheConfiguration
import java.util.UUID

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Component
class NomisMappingClient(private val nomisMappingApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Cacheable(CacheConfiguration.NOMIS_MAPPING_CACHE_NAME)
  fun getNomisLocationMappingsBy(dpsLocationIds: DpsLocationsIds) = nomisMappingApiWebClient
    .post()
    .uri("/api/locations/dps")
    .bodyValue(dpsLocationIds.locationIds)
    .retrieve()
    .bodyToMono(typeReference<Collection<NomisDpsLocationMapping>>())
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block() ?: emptyList()

  fun getNomisLocationMappingBy(dpsLocationId: UUID): NomisDpsLocationMapping? = nomisMappingApiWebClient
    .get()
    .uri("/api/locations/dps/{id}", dpsLocationId)
    .retrieve()
    .bodyToMono(NomisDpsLocationMapping::class.java)
    .doOnError { error -> log.info("Error looking up internal location mapping by dps location id $dpsLocationId in mapping service", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()
}

class DpsLocationsIds(locationIds: Collection<UUID>) {
  val locationIds = locationIds.sorted()
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DpsLocationsIds

    return locationIds == other.locationIds
  }

  override fun hashCode() = locationIds.hashCode()
}

data class NomisDpsLocationMapping(
  val dpsLocationId: UUID,
  val nomisLocationId: Long,
)
