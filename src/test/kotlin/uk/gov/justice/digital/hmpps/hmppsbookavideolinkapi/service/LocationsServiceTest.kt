package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.videoRoomAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.DayOfWeek
import java.time.LocalTime

class LocationsServiceTest {

  private val prisonRepository: PrisonRepository = mock()
  private val locationsClient: LocationsInsidePrisonClient = mock()
  private val locationAttributeRepository: LocationAttributeRepository = mock()

  private val service = LocationsService(prisonRepository, locationsClient, locationAttributeRepository)

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

  @Test
  fun `should return video link locations with extra attributes whether active or not`() {
    val locationA = location(WANDSWORTH, "A", active = true)
    val locationB = location(WANDSWORTH, "B", active = false)

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB)

    val roomAttributesA = videoRoomAttributes(
      prisonCode = WANDSWORTH,
      attributeId = 1,
      dpsLocationId = locationA.id,
    )

    val roomAttributesB = videoRoomAttributes(
      prisonCode = WANDSWORTH,
      attributeId = 1,
      dpsLocationId = locationB.id,
    )

    whenever(locationAttributeRepository.findByPrisonCode(WANDSWORTH)) doReturn roomAttributesA + roomAttributesB

    val result = service.getDecoratedVideoLocations(WANDSWORTH, enabledOnly = false)

    assertThat(result).hasSize(2)
    with(result[0]) {
      assertThat(key).isEqualTo(locationA.key)
      assertThat(dpsLocationId).isEqualTo(locationA.id)
      assertThat(description).isEqualTo(locationA.localName)
      assertThat(enabled).isTrue()
      assertThat(extraAttributes).isNotNull
      with(extraAttributes!!) {
        assertThat(locationStatus).isEqualTo(LocationStatus.ACTIVE)
        assertThat(locationUsage).isEqualTo(LocationUsage.SHARED)
        assertThat(schedule).hasSize(1)
        with(schedule[0]) {
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.MONDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(1, 0))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 0))
          assertThat(locationUsage).isEqualTo(LocationUsage.SHARED)
        }
      }
    }

    with(result[1]) {
      assertThat(key).isEqualTo(locationB.key)
      assertThat(dpsLocationId).isEqualTo(locationB.id)
      assertThat(description).isEqualTo(locationB.localName)
      assertThat(enabled).isFalse()
      assertThat(extraAttributes).isNotNull
      with(extraAttributes!!) {
        assertThat(locationStatus).isEqualTo(LocationStatus.ACTIVE)
        assertThat(locationUsage).isEqualTo(LocationUsage.SHARED)
        assertThat(schedule).hasSize(1)
        with(schedule[0]) {
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.MONDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(1, 0))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 0))
          assertThat(locationUsage).isEqualTo(LocationUsage.SHARED)
        }
      }
    }
  }

  @Test
  fun `should return only enabled video link locations with extra attributes`() {
    val locationA = location(WANDSWORTH, "A", active = true)
    val locationB = location(WANDSWORTH, "B", active = false)

    val roomAttributes = videoRoomAttributes(
      prisonCode = WANDSWORTH,
      attributeId = 1,
      dpsLocationId = locationA.id,
    )

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB)
    whenever(locationAttributeRepository.findByPrisonCode(WANDSWORTH)) doReturn roomAttributes

    val result = service.getDecoratedVideoLocations(WANDSWORTH, enabledOnly = true)

    assertThat(result).hasSize(1)
    with(result[0]) {
      assertThat(key).isEqualTo(locationA.key)
      assertThat(dpsLocationId).isEqualTo(locationA.id)
      assertThat(description).isEqualTo(locationA.localName)
      assertThat(enabled).isTrue()
      assertThat(extraAttributes).isNotNull
      with(extraAttributes!!) {
        assertThat(locationStatus).isEqualTo(LocationStatus.ACTIVE)
        assertThat(locationUsage).isEqualTo(LocationUsage.SHARED)
        assertThat(schedule).hasSize(1)
        with(schedule[0]) {
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.MONDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(1, 0))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 0))
          assertThat(locationUsage).isEqualTo(LocationUsage.SHARED)
        }
      }
    }
  }

  @Test
  fun `should return a list of enabled video link locations with no extra attributes if none exist`() {
    val locationA = location(WANDSWORTH, "A", active = true)
    val locationB = location(WANDSWORTH, "B", active = false)

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB)
    whenever(locationAttributeRepository.findByPrisonCode(WANDSWORTH)) doReturn emptyList()

    val result = service.getDecoratedVideoLocations(WANDSWORTH, enabledOnly = true)

    result containsExactlyInAnyOrder listOf(locationA.toModel())
    assertThat(result[0].extraAttributes).isNull()
  }
}
