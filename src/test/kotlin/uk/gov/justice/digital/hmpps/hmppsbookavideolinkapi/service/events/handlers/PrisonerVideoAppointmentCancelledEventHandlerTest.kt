package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.videoAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingHistoryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentScheduleInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.Identifier
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerVideoAppointmentCancelledEvent
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class PrisonerVideoAppointmentCancelledEventHandlerTest {

  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient = mock()
  private val videoAppointmentRepository: VideoAppointmentRepository = mock()
  private val bookingFacade: BookingFacade = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val bookingHistoryService: BookingHistoryService = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val supportedAppointmentTypes = SupportedAppointmentTypes(featureSwitches)
  private val handler = PrisonerVideoAppointmentCancelledEventHandler(
    activitiesAppointmentsClient,
    videoAppointmentRepository,
    bookingFacade,
    videoBookingRepository,
    prisonAppointmentRepository,
    bookingHistoryService,
    supportedAppointmentTypes,
  )

  @BeforeEach
  fun before() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(any())) doReturn true
  }

  @Test
  fun `should attempt to cancel a court booking when main appointment removed in NOMIS`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(any())) doReturn false
    val courtBooking = courtBooking().withMainCourtPrisonAppointment(prisonerNumber = "ABC345", prisonCode = WANDSWORTH)
    val courtAppointment = videoAppointment(courtBooking, courtBooking.appointments().single())

    whenever(
      videoAppointmentRepository.findActiveVideoAppointments(
        prisonCode = WANDSWORTH,
        prisonerNumber = "ABC345",
        appointmentDate = today(),
        startTime = LocalTime.MIDNIGHT,
      ),
    ) doReturn listOf(courtAppointment)

    handler.handle(
      cancellationEvent(
        prisonCode = WANDSWORTH,
        prisonerNumber = "ABC345",
        start = today().atStartOfDay(),
      ),
    )

    inOrder(videoAppointmentRepository, bookingFacade) {
      verify(videoAppointmentRepository).findActiveVideoAppointments(
        WANDSWORTH,
        "ABC345",
        appointmentDate = today(),
        startTime = LocalTime.MIDNIGHT,
      )
      verify(bookingFacade).cancel(courtBooking.videoBookingId, SERVICE_USER)
    }
  }

  @Test
  fun `should attempt to remove a court booking appointment when pre-appointment removed in NOMIS`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(any())) doReturn false
    val courtBooking = courtBooking()

    val courtAppointment = videoAppointment(
      courtBooking,
      PrisonAppointment.newAppointment(
        videoBooking = courtBooking,
        prison = prison(),
        prisonerNumber = "ABC345",
        appointmentType = "VLB_COURT_PRE",
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT,
        locationId = UUID.randomUUID(),
      ),
    )

    whenever(
      videoAppointmentRepository.findActiveVideoAppointments(
        prisonCode = WANDSWORTH,
        prisonerNumber = "ABC345",
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
      ),
    ) doReturn listOf(courtAppointment)

    whenever(videoBookingRepository.findById(courtBooking.videoBookingId)) doReturn Optional.of(courtBooking)

    handler.handle(
      cancellationEvent(
        prisonCode = WANDSWORTH,
        prisonerNumber = "ABC345",
        start = tomorrow().atStartOfDay(),
      ),
    )

    inOrder(videoAppointmentRepository, prisonAppointmentRepository, videoBookingRepository, bookingHistoryService) {
      verify(videoAppointmentRepository).findActiveVideoAppointments(
        WANDSWORTH,
        "ABC345",
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
      )

      verify(prisonAppointmentRepository).deleteById(courtAppointment.prisonAppointmentId)
      verify(prisonAppointmentRepository).flush()
      verify(videoBookingRepository).findById(courtBooking.videoBookingId)
      verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, courtBooking)
    }

    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should attempt to remove a court booking appointment when post-appointment removed in NOMIS`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(any())) doReturn false
    val courtBooking = courtBooking()

    val courtAppointment = videoAppointment(
      courtBooking,
      PrisonAppointment.newAppointment(
        videoBooking = courtBooking,
        prison = prison(),
        prisonerNumber = "ABC345",
        appointmentType = "VLB_COURT_POST",
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT,
        locationId = UUID.randomUUID(),
      ),
    )

    whenever(
      videoAppointmentRepository.findActiveVideoAppointments(
        prisonCode = WANDSWORTH,
        prisonerNumber = "ABC345",
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
      ),
    ) doReturn listOf(courtAppointment)

    whenever(videoBookingRepository.findById(courtBooking.videoBookingId)) doReturn Optional.of(courtBooking)

    handler.handle(
      cancellationEvent(
        prisonCode = WANDSWORTH,
        prisonerNumber = "ABC345",
        start = tomorrow().atStartOfDay(),
      ),
    )

    inOrder(videoAppointmentRepository, prisonAppointmentRepository, videoBookingRepository, bookingHistoryService) {
      verify(videoAppointmentRepository).findActiveVideoAppointments(
        WANDSWORTH,
        "ABC345",
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
      )

      verify(prisonAppointmentRepository).deleteById(courtAppointment.prisonAppointmentId)
      verify(prisonAppointmentRepository).flush()
      verify(videoBookingRepository).findById(courtBooking.videoBookingId)
      verify(bookingHistoryService).createBookingHistory(HistoryType.AMEND, courtBooking)
    }

    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should attempt to cancel a probation booking removed in NOMIS`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(any())) doReturn false
    val probationBooking = probationBooking().withProbationPrisonAppointment(prisonerNumber = "DEF345", prisonCode = PENTONVILLE)
    val probationAppointment = videoAppointment(probationBooking, probationBooking.appointments().single())

    whenever(
      videoAppointmentRepository.findActiveVideoAppointments(
        prisonCode = PENTONVILLE,
        prisonerNumber = "DEF345",
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
      ),
    ) doReturn listOf(probationAppointment)

    handler.handle(
      cancellationEvent(
        prisonCode = PENTONVILLE,
        prisonerNumber = "DEF345",
        start = tomorrow().atStartOfDay(),
      ),
    )

    inOrder(videoAppointmentRepository, bookingFacade) {
      verify(videoAppointmentRepository).findActiveVideoAppointments(
        PENTONVILLE,
        "DEF345",
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
      )
      verify(bookingFacade).cancel(probationBooking.videoBookingId, SERVICE_USER)
    }
  }

  @Test
  fun `should not attempt to cancel a booking when activities and appointments is active`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(PENTONVILLE)) doReturn true

    handler.handle(
      cancellationEvent(
        prisonCode = PENTONVILLE,
        prisonerNumber = "DEF345",
        start = tomorrow().atStartOfDay(),
      ),
    )

    verifyNoInteractions(videoAppointmentRepository)
    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should be a no-op when multiple appointments found`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(any())) doReturn false
    val courtBooking = courtBooking().withMainCourtPrisonAppointment(prisonerNumber = "ABC345", prisonCode = WANDSWORTH)
    val courtAppointment = videoAppointment(courtBooking, courtBooking.appointments().single())
    val probationBooking = probationBooking().withProbationPrisonAppointment(prisonerNumber = "ABC345", prisonCode = WANDSWORTH)
    val probationAppointment = videoAppointment(probationBooking, probationBooking.appointments().single())

    whenever(
      videoAppointmentRepository.findActiveVideoAppointments(
        prisonCode = WANDSWORTH,
        prisonerNumber = "ABC345",
        appointmentDate = today(),
        startTime = LocalTime.MIDNIGHT,
      ),
    ) doReturn listOf(courtAppointment, probationAppointment)

    handler.handle(
      cancellationEvent(
        prisonCode = WANDSWORTH,
        prisonerNumber = "ABC345",
        start = today().atStartOfDay(),
      ),
    )

    verify(videoAppointmentRepository).findActiveVideoAppointments(
      WANDSWORTH,
      "ABC345",
      appointmentDate = today(),
      startTime = LocalTime.MIDNIGHT,
    )

    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should be a no-op when not a video link appointment`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(any())) doReturn false
    handler.handle(
      cancellationEvent(
        prisonCode = WANDSWORTH,
        prisonerNumber = "ABC345",
        start = today().atStartOfDay(),
        eventType = "NOT_VLB",
      ),
    )

    verifyNoInteractions(videoAppointmentRepository)
    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should be a no-op when the appointment happened yesterday`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(any())) doReturn false
    val probationBooking = probationBooking().withProbationPrisonAppointment(prisonerNumber = "DEF345", prisonCode = PENTONVILLE, date = yesterday())
    val probationAppointment = videoAppointment(probationBooking, probationBooking.appointments().single())

    whenever(
      videoAppointmentRepository.findActiveVideoAppointments(
        prisonCode = PENTONVILLE,
        prisonerNumber = "DEF345",
        appointmentDate = yesterday(),
        startTime = LocalTime.MIDNIGHT,
      ),
    ) doReturn listOf(probationAppointment)

    handler.handle(
      cancellationEvent(
        prisonCode = PENTONVILLE,
        prisonerNumber = "DEF345",
        start = yesterday().atStartOfDay(),
      ),
    )

    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should be a no-op when the appointment happened earlier today`() {
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(any())) doReturn false
    val probationBooking = probationBooking().withProbationPrisonAppointment(prisonerNumber = "DEF345", prisonCode = PENTONVILLE, date = today())
    val probationAppointment = videoAppointment(probationBooking, probationBooking.appointments().single())

    val now = LocalTime.now()

    whenever(
      videoAppointmentRepository.findActiveVideoAppointments(
        prisonCode = PENTONVILLE,
        prisonerNumber = "DEF345",
        appointmentDate = today(),
        startTime = now,
      ),
    ) doReturn listOf(probationAppointment)

    handler.handle(
      cancellationEvent(
        prisonCode = PENTONVILLE,
        prisonerNumber = "DEF345",
        start = today().atTime(now),
      ),
    )

    verifyNoInteractions(bookingFacade)
  }

  private fun cancellationEvent(
    prisonCode: String,
    prisonerNumber: String,
    start: LocalDateTime,
    eventType: String = "VLB",
  ) = PrisonerVideoAppointmentCancelledEvent(
    personReference = PersonReference(identifiers = listOf(Identifier("NOMS", prisonerNumber))),
    additionalInformation = AppointmentScheduleInformation(
      scheduleEventId = 1,
      scheduledStartTime = start,
      scheduledEndTime = null,
      scheduleEventSubType = eventType,
      scheduleEventStatus = "",
      recordDeleted = true,
      agencyLocationId = prisonCode,
    ),
  )
}
