package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.videoRoomAttributesWithSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.videoRoomAttributesWithoutSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toRoomAttributes
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
  fun `should return active and inactive video link locations with extra attributes and schedules`() {
    val locationA = location(WANDSWORTH, "A", active = true)
    val locationB = location(WANDSWORTH, "B", active = false)

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB)

    val roomAttributesA = videoRoomAttributesWithSchedule(
      prisonCode = WANDSWORTH,
      dpsLocationId = locationA.id,
    )

    val roomAttributesB = videoRoomAttributesWithSchedule(
      prisonCode = WANDSWORTH,
      dpsLocationId = locationB.id,
    )

    whenever(locationAttributeRepository.findByPrisonCode(WANDSWORTH)) doReturn roomAttributesA + roomAttributesB

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = false)

    assertThat(result).hasSize(2)
    with(result[0]) {
      assertThat(key).isEqualTo(locationA.key)
      assertThat(dpsLocationId).isEqualTo(locationA.id)
      assertThat(description).isEqualTo(locationA.localName)
      assertThat(enabled).isTrue()
      assertThat(extraAttributes).isNotNull
      with(extraAttributes!!) {
        assertThat(locationStatus).isEqualTo(LocationStatus.ACTIVE)
        assertThat(locationUsage).isEqualTo(LocationUsage.SCHEDULE)
        assertThat(schedule).hasSize(1)
        with(schedule[0]) {
          assertThat(scheduleId).isEqualTo(0)
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
        assertThat(locationUsage).isEqualTo(LocationUsage.SCHEDULE)
        assertThat(schedule).hasSize(1)
        with(schedule[0]) {
          assertThat(scheduleId).isEqualTo(0)
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
  fun `should return only active video link locations with extra attributes and schedules`() {
    val locationA = location(WANDSWORTH, "A", active = true)
    val locationB = location(WANDSWORTH, "B", active = false)

    val roomAttributes = videoRoomAttributesWithSchedule(
      prisonCode = WANDSWORTH,
      dpsLocationId = locationA.id,
    )

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB)
    whenever(locationAttributeRepository.findByPrisonCode(WANDSWORTH)) doReturn roomAttributes

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    assertThat(result).hasSize(1)
    with(result[0]) {
      assertThat(key).isEqualTo(locationA.key)
      assertThat(dpsLocationId).isEqualTo(locationA.id)
      assertThat(description).isEqualTo(locationA.localName)
      assertThat(enabled).isTrue()
      assertThat(extraAttributes).isNotNull
      with(extraAttributes!!) {
        assertThat(locationStatus).isEqualTo(LocationStatus.ACTIVE)
        assertThat(locationUsage).isEqualTo(LocationUsage.SCHEDULE)
        assertThat(schedule).hasSize(1)
        with(schedule[0]) {
          assertThat(scheduleId).isEqualTo(0)
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.MONDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(1, 0))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 0))
          assertThat(locationUsage).isEqualTo(LocationUsage.SHARED)
          assertThat(allowedParties).isEmpty()
        }
      }
    }
  }

  @Test
  fun `should return only active video link locations with extra attributes but no schedule`() {
    val locationA = location(WANDSWORTH, "A", active = true)
    val locationB = location(WANDSWORTH, "B", active = false)

    val roomAttributes = videoRoomAttributesWithoutSchedule(
      prisonCode = WANDSWORTH,
      dpsLocationId = locationA.id,
    )

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB)
    whenever(locationAttributeRepository.findByPrisonCode(WANDSWORTH)) doReturn roomAttributes

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = true)

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
        assertThat(allowedParties).isEmpty()
        assertThat(schedule).isNullOrEmpty()
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

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = true)

    result containsExactlyInAnyOrder listOf(locationA.toModel())
    assertThat(result[0].extraAttributes).isNull()
  }

  @Test
  fun `should return a mixture of decorated and undecorated locations even when decorated with INACTIVE`() {
    val locationA = location(WANDSWORTH, "A", active = true)
    val locationB = location(WANDSWORTH, "B", active = true)
    val locationC = location(WANDSWORTH, "C", active = true)

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(locationA, locationB, locationC)

    // Location A is ACTIVE - in decoration data
    val roomAttributesA = videoRoomAttributesWithoutSchedule(
      prisonCode = WANDSWORTH,
      dpsLocationId = locationA.id,
      locationStatus = LocationStatus.ACTIVE,
    )

    // Location B is INACTIVE - in decoration data
    val roomAttributesB = videoRoomAttributesWithoutSchedule(
      prisonCode = WANDSWORTH,
      dpsLocationId = locationB.id,
      locationStatus = LocationStatus.INACTIVE,
    )

    // Location C has no decorating data

    whenever(locationAttributeRepository.findByPrisonCode(WANDSWORTH)) doReturn roomAttributesA + roomAttributesB

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = false)

    assertThat(result).hasSize(3)

    with(result[0]) {
      assertThat(key).isEqualTo(locationA.key)
      assertThat(enabled).isTrue()
      assertThat(extraAttributes).isNotNull
      with(extraAttributes!!) {
        assertThat(locationStatus).isEqualTo(LocationStatus.ACTIVE)
      }
    }

    with(result[1]) {
      assertThat(key).isEqualTo(locationB.key)
      assertThat(enabled).isTrue()
      with(extraAttributes!!) {
        assertThat(locationStatus).isEqualTo(LocationStatus.INACTIVE)
      }
    }

    with(result[2]) {
      assertThat(key).isEqualTo(locationC.key)
      assertThat(enabled).isTrue()
      assertThat(extraAttributes).isNull()
    }
  }

  @Test
  fun `should return decorated locations and preserving the ordering of prison locations`() {
    val locationA = location(WANDSWORTH, "A", active = true)
    val locationB = location(WANDSWORTH, "B", active = true)
    val locationC = location(WANDSWORTH, "C", active = true)
    val locationD = location(WANDSWORTH, "D", active = true)
    val locationE = location(WANDSWORTH, "E", active = true)

    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)

    whenever(locationsClient.getVideoLinkLocationsAtPrison(WANDSWORTH)) doReturn listOf(
      locationA,
      locationB,
      locationC,
      locationD,
      locationE,
    )

    // Location D decorations
    val roomAttributesD = videoRoomAttributesWithoutSchedule(
      prisonCode = WANDSWORTH,
      dpsLocationId = locationD.id,
      locationStatus = LocationStatus.ACTIVE,
    )

    // Location E decorations
    val roomAttributesE = videoRoomAttributesWithoutSchedule(
      prisonCode = WANDSWORTH,
      dpsLocationId = locationE.id,
      locationStatus = LocationStatus.ACTIVE,
    )

    // Reverse the order of room decorations returned
    whenever(locationAttributeRepository.findByPrisonCode(WANDSWORTH)) doReturn roomAttributesE + roomAttributesD

    val result = service.getVideoLinkLocationsAtPrison(WANDSWORTH, enabledOnly = false)

    assertThat(result).hasSize(5)

    assertThat(result[0].key).isEqualTo(locationA.key)
    assertThat(result[1].key).isEqualTo(locationB.key)
    assertThat(result[2].key).isEqualTo(locationC.key)
    assertThat(result[3].key).isEqualTo(locationD.key)
    assertThat(result[4].key).isEqualTo(locationE.key)
  }

  @Test
  fun `should return undecorated location by id`() {
    whenever(locationsClient.getLocationById(wandsworthLocation.id)) doReturn wandsworthLocation
    whenever(locationAttributeRepository.findByDpsLocationId(wandsworthLocation.id)) doReturn null

    service.getLocationById(wandsworthLocation.id) isEqualTo wandsworthLocation.toModel()

    inOrder(locationsClient, locationAttributeRepository) {
      verify(locationsClient).getLocationById(wandsworthLocation.id)
      verify(locationAttributeRepository).findByDpsLocationId(wandsworthLocation.id)
    }
  }

  @Test
  fun `should return decorated location by id`() {
    whenever(locationsClient.getLocationById(wandsworthLocation.id)) doReturn wandsworthLocation

    val roomAttributes = LocationAttribute.decoratedRoom(
      dpsLocationId = wandsworthLocation.id,
      prison = wandsworthPrison,
      locationStatus = LocationStatus.ACTIVE,
      locationUsage = LocationUsage.PROBATION,
      createdBy = PROBATION_USER,
      prisonVideoUrl = "video-link",
      allowedParties = emptySet(),
    )

    whenever(locationAttributeRepository.findByDpsLocationId(wandsworthLocation.id)) doReturn roomAttributes

    service.getLocationById(wandsworthLocation.id) isEqualTo wandsworthLocation.toModel(roomAttributes.toRoomAttributes())

    inOrder(locationsClient, locationAttributeRepository) {
      verify(locationsClient).getLocationById(wandsworthLocation.id)
      verify(locationAttributeRepository).findByDpsLocationId(wandsworthLocation.id)
    }
  }

  @Test
  fun `should return undecorated location by key`() {
    whenever(locationsClient.getLocationByKey(wandsworthLocation.key)) doReturn wandsworthLocation
    whenever(locationAttributeRepository.findByDpsLocationId(wandsworthLocation.id)) doReturn null

    service.getLocationByKey(wandsworthLocation.key) isEqualTo wandsworthLocation.toModel()

    inOrder(locationsClient, locationAttributeRepository) {
      verify(locationsClient).getLocationByKey(wandsworthLocation.key)
      verify(locationAttributeRepository).findByDpsLocationId(wandsworthLocation.id)
    }
  }

  @Test
  fun `should return decorated location by key`() {
    whenever(locationsClient.getLocationByKey(wandsworthLocation.key)) doReturn wandsworthLocation

    val roomAttributes = LocationAttribute.decoratedRoom(
      dpsLocationId = wandsworthLocation.id,
      prison = wandsworthPrison,
      locationStatus = LocationStatus.ACTIVE,
      locationUsage = LocationUsage.PROBATION,
      createdBy = PROBATION_USER,
      prisonVideoUrl = "video-link",
      allowedParties = emptySet(),
    )

    whenever(locationAttributeRepository.findByDpsLocationId(wandsworthLocation.id)) doReturn roomAttributes

    service.getLocationByKey(wandsworthLocation.key) isEqualTo wandsworthLocation.toModel(roomAttributes.toRoomAttributes())

    inOrder(locationsClient, locationAttributeRepository) {
      verify(locationsClient).getLocationByKey(wandsworthLocation.key)
      verify(locationAttributeRepository).findByDpsLocationId(wandsworthLocation.id)
    }
  }
}
