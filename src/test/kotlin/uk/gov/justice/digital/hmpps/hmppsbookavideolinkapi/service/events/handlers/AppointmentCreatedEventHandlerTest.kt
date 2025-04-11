package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class AppointmentCreatedEventHandlerTest {

  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService = mock()
  private val handler = AppointmentCreatedEventHandler(prisonAppointmentRepository, manageExternalAppointmentsService)
  private val courtAppointment = appointment(
    booking = courtBooking(),
    prisonCode = BIRMINGHAM,
    prisonerNumber = "123456",
    appointmentType = "VLB_COURT_PRE",
    date = LocalDate.of(2100, 1, 1),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(11, 30),
    locationId = UUID.randomUUID(),
  )

  @Test
  fun `should call manage external appointments on receipt of existing appointment event`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)

    handler.handle(AppointmentCreatedEvent(1))

    verify(manageExternalAppointmentsService).createAppointment(courtAppointment)
    verifyNoMoreInteractions(manageExternalAppointmentsService)
  }

  @Test
  fun `should no-op on receipt of non-existing appointment event`() {
    whenever(prisonAppointmentRepository.existsById(1)) doReturn false

    handler.handle(AppointmentCreatedEvent(1))

    verifyNoInteractions(manageExternalAppointmentsService)
  }
}
