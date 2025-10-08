package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.administration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.videoRoomAttributesWithoutSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService
import java.util.UUID

class DecoratedLocationsServiceTest {
  private val createDecoratedLocationService: CreateDecoratedLocationService = mock()
  private val amendDecoratedLocationService: AmendDecoratedLocationService = mock()
  private val createLocationScheduleService: CreateLocationScheduleService = mock()
  private val locationAttributeRepository: LocationAttributeRepository = mock()
  private val locationScheduleRepository: LocationScheduleRepository = mock()
  private val telemetryService: TelemetryService = mock()
  private val service = DecoratedLocationsService(
    createDecoratedLocationService,
    amendDecoratedLocationService,
    createLocationScheduleService,
    locationAttributeRepository,
    locationScheduleRepository,
    telemetryService,
  )
  private val blockedLocation1 = videoRoomAttributesWithoutSchedule(
    prisonCode = RISLEY,
    dpsLocationId = UUID.randomUUID(),
    locationStatus = LocationStatus.TEMPORARILY_BLOCKED,
    blockedFrom = today(),
    blockedTo = today(),
  )
  private val blockedLocation2 = videoRoomAttributesWithoutSchedule(
    prisonCode = RISLEY,
    dpsLocationId = UUID.randomUUID(),
    locationStatus = LocationStatus.TEMPORARILY_BLOCKED,
    blockedFrom = today(),
    blockedTo = today(),
  )
  private val activeLocation = videoRoomAttributesWithoutSchedule(
    prisonCode = RISLEY,
    dpsLocationId = UUID.randomUUID(),
    locationStatus = LocationStatus.ACTIVE,
  )
  private val blockedLocations = mutableListOf(blockedLocation1, blockedLocation2)
  private val telemetryEventCaptor = argumentCaptor<TelemetryEvent>()

  @Test
  fun `should reactivate blocked locations`() {
    locationAttributeRepository.stub {
      on { findByBlockedToNotNullAndBlockedToIsBefore(today()) } doReturn blockedLocations
      on { saveAllAndFlush(blockedLocations) } doReturn blockedLocations
    }

    blockedLocation1.locationStatus isEqualTo LocationStatus.TEMPORARILY_BLOCKED
    blockedLocation2.locationStatus isEqualTo LocationStatus.TEMPORARILY_BLOCKED

    service.reactivateBlockedLocationsBefore(today())

    blockedLocation1.locationStatus isEqualTo LocationStatus.ACTIVE
    blockedLocation2.locationStatus isEqualTo LocationStatus.ACTIVE

    inOrder(locationAttributeRepository, locationAttributeRepository, telemetryService) {
      verify(locationAttributeRepository).findByBlockedToNotNullAndBlockedToIsBefore(today())
      verify(locationAttributeRepository).saveAllAndFlush(blockedLocations)
      verify(telemetryService, times(2)).track(telemetryEventCaptor.capture())
    }

    telemetryEventCaptor.firstValue.properties()["dps_location_id"] isEqualTo blockedLocation1.dpsLocationId.toString()
    telemetryEventCaptor.secondValue.properties()["dps_location_id"] isEqualTo blockedLocation2.dpsLocationId.toString()
  }

  @Test
  fun `should fail to reactivate active location`() {
    locationAttributeRepository.stub {
      on { findByBlockedToNotNullAndBlockedToIsBefore(today()) } doReturn mutableListOf(activeLocation)
    }

    activeLocation.locationStatus isEqualTo LocationStatus.ACTIVE

    assertThrows<IllegalArgumentException> { service.reactivateBlockedLocationsBefore(today()) }

    activeLocation.locationStatus isEqualTo LocationStatus.ACTIVE

    verify(locationAttributeRepository).findByBlockedToNotNullAndBlockedToIsBefore(today())
    verifyNoMoreInteractions(locationAttributeRepository)
    verifyNoInteractions(telemetryService)
  }
}
