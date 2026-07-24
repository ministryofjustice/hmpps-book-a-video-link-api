package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.appointmentResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoEventRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.findByCourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.findByProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import java.time.LocalDate.now
import java.time.LocalTime
import java.util.UUID

class VideoEventsByLocationServiceTest {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val locationsService: LocationsService = mock()
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val referenceCodeRepository: ReferenceCodeRepository = mock()

  private val prisonCode = PENTONVILLE

  private val videoLocation1 = Location(
    key = "PVI-RM-1",
    prisonCode = prisonCode,
    description = "Video room 1",
    enabled = true,
    dpsLocationId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
  )

  private val videoLocation2 = Location(
    key = "PVI-RM-2",
    prisonCode = prisonCode,
    description = "Video room 2",
    enabled = true,
    dpsLocationId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
  )

  private val videoLocation3 = Location(
    key = "PVI-RM-3",
    prisonCode = prisonCode,
    description = "Video room 3",
    enabled = false,
    dpsLocationId = UUID.fromString("2041db93-9c87-4b71-b016-52ccb300f8df"),
  )

  private val service = VideoEventsByLocationService(
    locationsService,
    activitiesAppointmentsClient,
    prisonAppointmentRepository,
    referenceCodeRepository,
  )

  @BeforeEach
  fun setUp() {
    openMocks(this)

    whenever(referenceCodeRepository.findByCourtHearingType("TRIBUNAL")).thenReturn(courtHearingType("Tribunal"))
    whenever(referenceCodeRepository.findByProbationMeetingType("PSR")).thenReturn(probationMeetingType("Pre-sentence report"))
  }

  @AfterEach
  fun tearDown() {
    reset(locationsService, activitiesAppointmentsClient, prisonAppointmentRepository, referenceCodeRepository)
  }

  @Test
  fun `should return an empty list of locations if no video link locations are defined for a prison`() {
    whenever(locationsService.getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)).thenReturn(emptyList())

    val response = service.videoEventsByLocation(PENTONVILLE, VideoEventRequest(now(), now()))

    assertThat(response.prisonCode).isEqualTo(prisonCode)
    assertThat(response.startDate).isEqualTo(now())
    assertThat(response.endDate).isEqualTo(now())
    assertThat(response.locations).isEmpty()

    verify(locationsService).getVideoLinkLocationsAtPrison(prisonCode, false)

    verifyNoInteractions(activitiesAppointmentsClient, prisonAppointmentRepository, referenceCodeRepository)
    verifyNoMoreInteractions(locationsService)
  }

  @Test
  fun `should return a list of video locations even when no video events exist in any of them`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)).thenReturn(true)
    whenever(activitiesAppointmentsClient.getScheduledAppointmentsBetween(prisonCode, now(), now())).thenReturn(emptyList())
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsBetweenDates(prisonCode, now(), now())).thenReturn(emptyList())
    whenever(locationsService.getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)).thenReturn(
      listOf(videoLocation1, videoLocation2, videoLocation3),
    )

    val response = service.videoEventsByLocation(PENTONVILLE, VideoEventRequest(now(), now()))

    assertThat(response.prisonCode).isEqualTo(prisonCode)
    assertThat(response.startDate).isEqualTo(now())
    assertThat(response.endDate).isEqualTo(now())

    assertThat(response.locations).hasSize(3)

    for (i in 0..2) {
      with(response.locations[i]) {
        assertThat(events).isEmpty()
      }
    }

    verify(locationsService).getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)
    verify(activitiesAppointmentsClient).isAppointmentsRolledOutAt(prisonCode)
    verify(activitiesAppointmentsClient).getScheduledAppointmentsBetween(prisonCode, now(), now())
    verify(prisonAppointmentRepository).findActivePrisonAppointmentsBetweenDates(prisonCode, now(), now())

    verifyNoMoreInteractions(locationsService, activitiesAppointmentsClient, prisonAppointmentRepository, referenceCodeRepository)
  }

  @Test
  fun `should not return appointments if A&A is switched off and only return BVLS video bookings`() {
    val bvlsAppointment1 = probationBooking()
      .withProbationPrisonAppointment(
        date = now(),
        prisonCode = prisonCode,
        location = pentonvilleLocation.copy(id = videoLocation1.dpsLocationId),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
      )
      .appointments()
      .first()

    val bvlsAppointment2 = courtBooking()
      .withMainCourtPrisonAppointment(
        date = now(),
        prisonCode = prisonCode,
        location = pentonvilleLocation.copy(id = videoLocation2.dpsLocationId),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(12, 0),
      )
      .appointments()
      .first()

    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)).thenReturn(false)
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsBetweenDates(prisonCode, now(), now())).thenReturn(
      listOf(bvlsAppointment1, bvlsAppointment2),
    )
    whenever(locationsService.getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)).thenReturn(
      listOf(videoLocation1, videoLocation2, videoLocation3),
    )

    val response = service.videoEventsByLocation(PENTONVILLE, VideoEventRequest(now(), now()))

    assertThat(response.locations).hasSize(3)

    assertThat(response.locations[0].events).hasSize(1)
    assertThat(response.locations[1].events).hasSize(1)
    assertThat(response.locations[2].events).isEmpty()

    verify(locationsService).getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)
    verify(activitiesAppointmentsClient).isAppointmentsRolledOutAt(prisonCode)
    verify(prisonAppointmentRepository).findActivePrisonAppointmentsBetweenDates(prisonCode, now(), now())
    verify(referenceCodeRepository).findByProbationMeetingType(ProbationMeetingType.PSR.name)
    verify(referenceCodeRepository).findByCourtHearingType(CourtHearingType.TRIBUNAL.name)

    verifyNoMoreInteractions(activitiesAppointmentsClient, prisonAppointmentRepository, locationsService, referenceCodeRepository)
  }

  @Test
  fun `should return locations and video appointments`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)).thenReturn(true)
    whenever(activitiesAppointmentsClient.getScheduledAppointmentsBetween(prisonCode, now(), now())).thenReturn(
      listOf(
        appointmentResult(
          prisonCode = prisonCode,
          dpsLocationId = videoLocation1.dpsLocationId,
          appointmentSeriesId = 1L,
          appointmentId = 1L,
          date = now(),
          startTime = "09:00",
          endTime = "10:00",
          categoryCode = "VLOO",
          categoryDescription = "Video link - official other",
        ),
        appointmentResult(
          prisonCode = prisonCode,
          dpsLocationId = videoLocation2.dpsLocationId,
          appointmentSeriesId = 2L,
          appointmentId = 2L,
          date = now(),
          startTime = "10:00",
          endTime = "11:00",
          categoryCode = "VLLA",
          categoryDescription = "Video link - legal appointment",
        ),
      ),
    )
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsBetweenDates(prisonCode, now(), now())).thenReturn(
      emptyList(),
    )
    whenever(locationsService.getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)).thenReturn(
      listOf(videoLocation1, videoLocation2, videoLocation3),
    )

    val response = service.videoEventsByLocation(PENTONVILLE, VideoEventRequest(now(), now()))

    assertThat(response.locations).hasSize(3)

    assertThat(response.locations[0].events).hasSize(1)
    assertThat(response.locations[1].events).hasSize(1)
    assertThat(response.locations[2].events).isEmpty()

    verify(locationsService).getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)
    verify(activitiesAppointmentsClient).isAppointmentsRolledOutAt(prisonCode)
    verify(activitiesAppointmentsClient).getScheduledAppointmentsBetween(prisonCode, now(), now())
    verify(prisonAppointmentRepository).findActivePrisonAppointmentsBetweenDates(prisonCode, now(), now())

    verifyNoMoreInteractions(locationsService, activitiesAppointmentsClient, prisonAppointmentRepository, referenceCodeRepository)
  }

  @Test
  fun `should return locations with mixed video bookings and A&A appointments interleaved and sorted by date and time`() {
    val bvlsAppointment1 = probationBooking()
      .withProbationPrisonAppointment(
        date = now(),
        prisonCode = prisonCode,
        location = pentonvilleLocation.copy(id = videoLocation1.dpsLocationId),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
      )
      .appointments()
      .first()

    val bvlsAppointment2 = courtBooking()
      .withMainCourtPrisonAppointment(
        date = now(),
        prisonCode = prisonCode,
        location = pentonvilleLocation.copy(id = videoLocation2.dpsLocationId),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(12, 0),
      )
      .appointments()
      .first()

    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)).thenReturn(true)
    whenever(activitiesAppointmentsClient.getScheduledAppointmentsBetween(prisonCode, now(), now())).thenReturn(
      listOf(
        appointmentResult(
          prisonCode = prisonCode,
          dpsLocationId = videoLocation1.dpsLocationId,
          appointmentSeriesId = 1L,
          appointmentId = 1L,
          date = now(),
          startTime = "09:00",
          endTime = "10:00",
          categoryCode = "VLLA",
          categoryDescription = "Video link - legal appointment",
        ),
        appointmentResult(
          prisonCode = prisonCode,
          dpsLocationId = videoLocation2.dpsLocationId,
          appointmentSeriesId = 2L,
          appointmentId = 2L,
          date = now(),
          startTime = "10:00",
          endTime = "11:00",
          categoryCode = "VLLA",
          categoryDescription = "Video link - legal appointment",
        ),
      ),
    )
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsBetweenDates(prisonCode, now(), now())).thenReturn(
      listOf(bvlsAppointment1, bvlsAppointment2),
    )
    whenever(locationsService.getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)).thenReturn(
      listOf(videoLocation1, videoLocation2),
    )

    val response = service.videoEventsByLocation(PENTONVILLE, VideoEventRequest(now(), now()))

    assertThat(response.locations).hasSize(2)

    assertThat(response.locations[0].events).hasSize(2)
    assertThat(response.locations[1].events).hasSize(2)

    assertThat(response.locations[0].events).extracting("eventType").containsExactly("APPOINTMENT", "PROBATION")
    assertThat(response.locations[1].events).extracting("eventType").containsExactly("APPOINTMENT", "COURT")

    verify(locationsService).getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)
    verify(activitiesAppointmentsClient).isAppointmentsRolledOutAt(prisonCode)
    verify(activitiesAppointmentsClient).getScheduledAppointmentsBetween(prisonCode, now(), now())
    verify(prisonAppointmentRepository).findActivePrisonAppointmentsBetweenDates(prisonCode, now(), now())
    verify(referenceCodeRepository).findByProbationMeetingType(ProbationMeetingType.PSR.name)
    verify(referenceCodeRepository).findByCourtHearingType(CourtHearingType.TRIBUNAL.name)

    verifyNoMoreInteractions(locationsService, activitiesAppointmentsClient, prisonAppointmentRepository, referenceCodeRepository)
  }

  @Test
  fun `should filter appointments to uncancelled, undeleted, non-BVLS appointment types only`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)).thenReturn(true)
    whenever(activitiesAppointmentsClient.getScheduledAppointmentsBetween(prisonCode, now(), now())).thenReturn(
      listOf(
        appointmentResult(
          prisonCode = prisonCode,
          dpsLocationId = videoLocation1.dpsLocationId,
          appointmentSeriesId = 1L,
          appointmentId = 1L,
          date = now(),
          startTime = "09:00",
          endTime = "10:00",
          categoryCode = "VLB",
          categoryDescription = "Video link - court hearing",
        ),
        appointmentResult(
          prisonCode = prisonCode,
          dpsLocationId = videoLocation2.dpsLocationId,
          appointmentSeriesId = 2L,
          appointmentId = 2L,
          date = now(),
          startTime = "10:00",
          endTime = "11:00",
          categoryCode = "VLPM",
          categoryDescription = "Video link - probation meeting",
        ),
        appointmentResult(
          prisonCode = prisonCode,
          dpsLocationId = videoLocation3.dpsLocationId,
          appointmentSeriesId = 3L,
          appointmentId = 3L,
          date = now(),
          startTime = "13:00",
          endTime = "14:00",
          categoryCode = "CHAP",
          categoryDescription = "Chaplaincy",
        ),
        appointmentResult(
          prisonCode = prisonCode,
          dpsLocationId = videoLocation3.dpsLocationId,
          appointmentSeriesId = 4L,
          appointmentId = 4L,
          date = now(),
          startTime = "14:00",
          endTime = "15:00",
          categoryCode = "VLPA",
          categoryDescription = "Video link - parole",
        ),
      ),
    )
    whenever(prisonAppointmentRepository.findActivePrisonAppointmentsBetweenDates(prisonCode, now(), now())).thenReturn(
      emptyList(),
    )
    whenever(locationsService.getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)).thenReturn(
      listOf(videoLocation1, videoLocation2, videoLocation3),
    )

    val response = service.videoEventsByLocation(PENTONVILLE, VideoEventRequest(now(), now()))

    assertThat(response.locations).hasSize(3)

    assertThat(response.locations[0].events).isEmpty()
    assertThat(response.locations[1].events).isEmpty()
    assertThat(response.locations[2].events).hasSize(1)

    verify(locationsService).getVideoLinkLocationsAtPrison(prisonCode, enabledOnly = false)
    verify(activitiesAppointmentsClient).isAppointmentsRolledOutAt(prisonCode)
    verify(activitiesAppointmentsClient).getScheduledAppointmentsBetween(prisonCode, now(), now())
    verify(prisonAppointmentRepository).findActivePrisonAppointmentsBetweenDates(prisonCode, now(), now())

    verifyNoMoreInteractions(locationsService, activitiesAppointmentsClient, prisonAppointmentRepository, referenceCodeRepository)
  }
}
