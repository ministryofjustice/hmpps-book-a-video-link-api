package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.administration

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationScheduleUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.videoRoomAttributesWithoutSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
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
    blockedToTime = LocalTime.now(),
  )
  private val blockedLocation2 = videoRoomAttributesWithoutSchedule(
    prisonCode = RISLEY,
    dpsLocationId = UUID.randomUUID(),
    locationStatus = LocationStatus.TEMPORARILY_BLOCKED,
    blockedFrom = today(),
    blockedTo = today(),
    blockedToTime = LocalTime.now(),
  )
  private val activeLocation = videoRoomAttributesWithoutSchedule(
    prisonCode = RISLEY,
    dpsLocationId = UUID.randomUUID(),
    locationStatus = LocationStatus.ACTIVE,
    locationUsage = LocationUsage.SCHEDULE,
  )
  private val blockedLocations = mutableListOf(blockedLocation1, blockedLocation2)
  private val telemetryEventCaptor = argumentCaptor<TelemetryEvent>()

  @Test
  fun `should reactivate blocked locations`() {
    locationAttributeRepository.stub {
      on { findByLocationStatus(LocationStatus.TEMPORARILY_BLOCKED) } doReturn blockedLocations
      on { saveAllAndFlush(blockedLocations) } doReturn blockedLocations
    }

    blockedLocation1.locationStatus isEqualTo LocationStatus.TEMPORARILY_BLOCKED
    blockedLocation2.locationStatus isEqualTo LocationStatus.TEMPORARILY_BLOCKED

    service.reactivateBlockedLocationsBefore(LocalDateTime.now())

    blockedLocation1.locationStatus isEqualTo LocationStatus.ACTIVE
    blockedLocation2.locationStatus isEqualTo LocationStatus.ACTIVE

    inOrder(locationAttributeRepository, locationAttributeRepository, telemetryService) {
      verify(locationAttributeRepository).findByLocationStatus(LocationStatus.TEMPORARILY_BLOCKED)
      verify(locationAttributeRepository).saveAllAndFlush(blockedLocations)
      verify(telemetryService, times(2)).track(telemetryEventCaptor.capture())
    }

    telemetryEventCaptor.firstValue.properties()["dps_location_id"] isEqualTo blockedLocation1.dpsLocationId.toString()
    telemetryEventCaptor.secondValue.properties()["dps_location_id"] isEqualTo blockedLocation2.dpsLocationId.toString()
  }

  @Test
  fun `should delete schedule from location attributes`() {
    locationAttributeRepository.stub {
      on { findByDpsLocationId(activeLocation.dpsLocationId) } doReturn activeLocation
    }

    activeLocation.addSchedule(
      usage = LocationScheduleUsage.COURT,
      startDayOfWeek = 1,
      endDayOfWeek = 5,
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(17, 0),
      allowedParties = emptySet(),
      notes = null,
      createdBy = COURT_USER,
    )

    activeLocation.schedule().isEmpty() isBool false

    locationScheduleRepository.stub {
      on { findById(any()) } doReturn Optional.of(activeLocation.schedule().first())
    }

    service.deleteSchedule(activeLocation.dpsLocationId, 0, COURT_USER)

    activeLocation.schedule().isEmpty() isBool true
  }
}
