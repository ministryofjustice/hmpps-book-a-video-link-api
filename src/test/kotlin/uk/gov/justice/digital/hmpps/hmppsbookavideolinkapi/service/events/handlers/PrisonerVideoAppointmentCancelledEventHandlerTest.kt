package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.videoAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentScheduleInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.Identifier
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerVideoAppointmentCancelledEvent
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonerVideoAppointmentCancelledEventHandlerTest {

  private val videoAppointmentRepository: VideoAppointmentRepository = mock()
  private val bookingFacade: BookingFacade = mock()
  private val handler = PrisonerVideoAppointmentCancelledEventHandler(videoAppointmentRepository, bookingFacade)

  @Test
  fun `should attempt to cancel a court booking when main appointment`() {
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
  fun `should attempt to cancel a probation booking`() {
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
  fun `should be a no-op when multiple appointments found`() {
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
