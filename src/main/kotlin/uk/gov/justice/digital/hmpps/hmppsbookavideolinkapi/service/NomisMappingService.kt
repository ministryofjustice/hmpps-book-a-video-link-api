package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import java.util.UUID

@Service
class NomisMappingService(private val nomisMappingClient: NomisMappingClient) {
  fun getNomisLocationId(dpsLocationId: UUID) = nomisMappingClient.getNomisLocationMappingBy(dpsLocationId)?.nomisLocationId
}
