package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService

class AppointmentCreatedEventHandlerTest {

  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService = mock()
  private val handler = AppointmentCreatedEventHandler(prisonAppointmentRepository, manageExternalAppointmentsService)

  @Test
  fun `should call manage external appointments on receipt of existing appointment event`() {
    whenever(prisonAppointmentRepository.existsById(1)) doReturn true

    handler.handle(AppointmentCreatedEvent(AppointmentInformation(1)))

    verify(manageExternalAppointmentsService).createAppointment(1)
  }

  @Test
  fun `should no-op on receipt of non-existing appointment event`() {
    whenever(prisonAppointmentRepository.existsById(1)) doReturn false

    handler.handle(AppointmentCreatedEvent(AppointmentInformation(1)))

    verifyNoInteractions(manageExternalAppointmentsService)
  }
}
