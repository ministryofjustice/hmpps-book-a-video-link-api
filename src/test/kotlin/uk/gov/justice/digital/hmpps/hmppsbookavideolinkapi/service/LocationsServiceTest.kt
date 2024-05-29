package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

class LocationsServiceTest {

  private val prisonRepository: PrisonRepository = mock()
  private val locationsClient: LocationsInsidePrisonClient = mock()
  private val service = LocationsService(prisonRepository, locationsClient)

  @Test
  fun `should return a list of enabled locations`() {
    val locationA = location(MOORLAND, "A", active = true)
    val locationB = location(MOORLAND, "B", active = true)

    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getLocationsAtPrison(MOORLAND)) doReturn listOf(locationA, locationB)

    val result = service.getLocationsAtPrison(MOORLAND, enabledOnly = true)

    result containsExactlyInAnyOrder listOf(locationA.toModel(), locationB.toModel())
  }

  @Test
  fun `should return a list of only enabled locations`() {
    val locationA = location(MOORLAND, "A", active = true)
    val locationB = location(MOORLAND, "B", active = false)

    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getLocationsAtPrison(MOORLAND)) doReturn listOf(locationA, locationB)

    val result = service.getLocationsAtPrison(MOORLAND, enabledOnly = true)

    result containsExactlyInAnyOrder listOf(locationA.toModel())
  }

  @Test
  fun `should return no locations when none active`() {
    val locationA = location(MOORLAND, "A", active = false)
    val locationB = location(MOORLAND, "B", active = false)

    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getLocationsAtPrison(MOORLAND)) doReturn listOf(locationA, locationB)

    val result = service.getLocationsAtPrison(MOORLAND, enabledOnly = true)

    result containsExactlyInAnyOrder listOf()
  }

  @Test
  fun `should return no locations when none match prison code`() {
    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getLocationsAtPrison(MOORLAND)) doReturn emptyList()

    val result = service.getLocationsAtPrison(MOORLAND, enabledOnly = true)

    result containsExactlyInAnyOrder listOf()
  }

  @Test
  fun `should return no locations when prison code not found`() {
    whenever(prisonRepository.findByCode(MOORLAND)) doReturn null

    val result = service.getLocationsAtPrison(MOORLAND, enabledOnly = true)

    result containsExactlyInAnyOrder listOf()

    verifyNoInteractions(locationsClient)
  }
}
