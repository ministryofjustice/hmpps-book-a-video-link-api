package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

class LocationsServiceTest {

  private val prisonRepository: PrisonRepository = mock()
  private val locationsClient: LocationsInsidePrisonClient = mock()
  private val service = LocationsService(prisonRepository, locationsClient)

  @Test
  fun `should return a list of non-residential enabled locations sorted by description`() {
    val locationA = location(WANDSWORTH, "A", active = true, localName = "AAAAA")
    val locationB = location(WANDSWORTH, "B", active = true, localName = "BBBBB")

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getNonResidentialAppointmentLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationB, locationA)

    val result = service.getNonResidentialLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result isEqualTo listOf(locationA.toModel(), locationB.toModel())
  }

  @Test
  fun `should return a list of only enabled non-residential locations`() {
    val locationA = location(WANDSWORTH, "A", active = true, localName = "AAAAA")
    val locationB = location(WANDSWORTH, "B", active = false, localName = "BBBBB")
    val locationC = location(WANDSWORTH, "C", active = true, localName = "CCCC")

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getNonResidentialAppointmentLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationC, locationA, locationB)

    val result = service.getNonResidentialLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result isEqualTo listOf(locationA.toModel(), locationC.toModel())
  }

  @Test
  fun `should return no non-residential locations when none active`() {
    val locationA = location(WANDSWORTH, "A", active = false)
    val locationB = location(WANDSWORTH, "B", active = false)

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getNonResidentialAppointmentLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB)

    val result = service.getNonResidentialLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result.isEmpty() isBool true
  }

  @Test
  fun `should return no non-residential locations when none match prison code`() {
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getNonResidentialAppointmentLocationsAtPrison(WANDSWORTH)) doReturn emptyList()

    val result = service.getNonResidentialLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result.isEmpty() isBool true
  }

  @Test
  fun `should return no non-residential locations when prison code not found`() {
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn null

    val result = service.getNonResidentialLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result.isEmpty() isBool true

    verifyNoInteractions(locationsClient)
  }

  @Test
  fun `should return a list of video link enabled locations`() {
    val locationA = location(WANDSWORTH, "A", active = true)
    val locationB = location(WANDSWORTH, "B", active = true)

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB)

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result containsExactlyInAnyOrder listOf(locationA.toModel(), locationB.toModel())
  }

  @Test
  fun `should return a list of only enabled video link locations`() {
    val locationA = location(WANDSWORTH, "A", active = true)
    val locationB = location(WANDSWORTH, "B", active = false)

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB)

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result containsExactlyInAnyOrder listOf(locationA.toModel())
  }

  @Test
  fun `should return no video link locations when none active`() {
    val locationA = location(WANDSWORTH, "A", active = false)
    val locationB = location(WANDSWORTH, "B", active = false)

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB)

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result.isEmpty() isBool true
  }

  @Test
  fun `should return no video link locations when none match prison code`() {
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn emptyList()

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result.isEmpty() isBool true
  }

  @Test
  fun `should return no video link locations when prison code not found`() {
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn null

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result.isEmpty() isBool true

    verifyNoInteractions(locationsClient)
  }
}
