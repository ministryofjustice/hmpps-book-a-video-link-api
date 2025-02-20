package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class NomisMappingClient(private val nomisMappingApiWebClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getNomisLocationMappingsBy(dpsLocationIds: Collection<UUID>): Collection<NomisDpsLocationMapping> = dpsLocationIds.mapNotNull { getNomisLocationMappingBy(it) }

  fun getNomisLocationMappingBy(dpsLocationId: UUID): NomisDpsLocationMapping? = nomisMappingApiWebClient
    .get()
    .uri("/api/locations/dps/{id}", dpsLocationId)
    .retrieve()
    .bodyToMono(NomisDpsLocationMapping::class.java)
    .doOnError { error -> log.info("Error looking up internal location mapping by dps location id $dpsLocationId in mapping service", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()
}

data class NomisDpsLocationMapping(
  val dpsLocationId: UUID,
  val nomisLocationId: Long,
)
