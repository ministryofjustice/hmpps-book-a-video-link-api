package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

class LocationsServiceTest {

  private val prisonRepository: PrisonRepository = mock()
  private val locationsClient: LocationsInsidePrisonClient = mock()
  private val service = LocationsService(prisonRepository, locationsClient)

  @Test
  fun `should return a list of non-residential enabled locations`() {
    val locationA = location(MOORLAND, "A", active = true)
    val locationB = location(MOORLAND, "B", active = true)

    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getNonResidentialAppointmentLocationsAtPrison(MOORLAND)) doReturn listOf(locationA, locationB)

    val result = service.getNonResidentialLocationsAtPrison(MOORLAND, enabledOnly = true)

    result containsExactlyInAnyOrder listOf(locationA.toModel(), locationB.toModel())
  }

  @Test
  fun `should return a list of only enabled non-residential locations`() {
    val locationA = location(MOORLAND, "A", active = true)
    val locationB = location(MOORLAND, "B", active = false)

    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getNonResidentialAppointmentLocationsAtPrison(MOORLAND)) doReturn listOf(locationA, locationB)

    val result = service.getNonResidentialLocationsAtPrison(MOORLAND, enabledOnly = true)

    result containsExactlyInAnyOrder listOf(locationA.toModel())
  }

  @Test
  fun `should return no non-residential locations when none active`() {
    val locationA = location(MOORLAND, "A", active = false)
    val locationB = location(MOORLAND, "B", active = false)

    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getNonResidentialAppointmentLocationsAtPrison(MOORLAND)) doReturn listOf(locationA, locationB)

    val result = service.getNonResidentialLocationsAtPrison(MOORLAND, enabledOnly = true)

    result.isEmpty() isBool true
  }

  @Test
  fun `should return no non-residential locations when none match prison code`() {
    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getNonResidentialAppointmentLocationsAtPrison(MOORLAND)) doReturn emptyList()

    val result = service.getNonResidentialLocationsAtPrison(MOORLAND, enabledOnly = true)

    result.isEmpty() isBool true
  }

  @Test
  fun `should return no non-residential locations when prison code not found`() {
    whenever(prisonRepository.findByCode(MOORLAND)) doReturn null

    val result = service.getNonResidentialLocationsAtPrison(MOORLAND, enabledOnly = true)

    result.isEmpty() isBool true

    verifyNoInteractions(locationsClient)
  }

  @Test
  fun `should return a list of video link enabled locations`() {
    val locationA = location(MOORLAND, "A", active = true)
    val locationB = location(MOORLAND, "B", active = true)

    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(MOORLAND)) doReturn listOf(locationA, locationB)

    val result = service.getVideoLinkLocationsAtPrison(MOORLAND, enabledOnly = true)

    result containsExactlyInAnyOrder listOf(locationA.toModel(), locationB.toModel())
  }

  @Test
  fun `should return a list of only enabled video link locations`() {
    val locationA = location(MOORLAND, "A", active = true)
    val locationB = location(MOORLAND, "B", active = false)

    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(MOORLAND)) doReturn listOf(locationA, locationB)

    val result = service.getVideoLinkLocationsAtPrison(MOORLAND, enabledOnly = true)

    result containsExactlyInAnyOrder listOf(locationA.toModel())
  }

  @Test
  fun `should return no video link locations when none active`() {
    val locationA = location(MOORLAND, "A", active = false)
    val locationB = location(MOORLAND, "B", active = false)

    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(MOORLAND)) doReturn listOf(locationA, locationB)

    val result = service.getVideoLinkLocationsAtPrison(MOORLAND, enabledOnly = true)

    result.isEmpty() isBool true
  }

  @Test
  fun `should return no video link locations when none match prison code`() {
    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(MOORLAND)) doReturn emptyList()

    val result = service.getVideoLinkLocationsAtPrison(MOORLAND, enabledOnly = true)

    result.isEmpty() isBool true
  }

  @Test
  fun `should return no video link locations when prison code not found`() {
    whenever(prisonRepository.findByCode(MOORLAND)) doReturn null

    val result = service.getVideoLinkLocationsAtPrison(MOORLAND, enabledOnly = true)

    result.isEmpty() isBool true

    verifyNoInteractions(locationsClient)
  }
}
