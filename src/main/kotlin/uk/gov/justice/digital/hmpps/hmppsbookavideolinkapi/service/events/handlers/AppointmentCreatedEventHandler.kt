package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService

/**
 * This handler is responsible for delegating the creation of appointments to external services e.g. Activities and
 * Appointments and/or prison-api (NOMIS).
 *
 * Having this handled in the event handler means we are decoupling it from the actual appointment creation in BVLS and
 * will be handled as a background process. It also means if there are any issues talking to those external services it
 * will be retried should an error occur.
 */
@Component
class AppointmentCreatedEventHandler(
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService,
) : DomainEventHandler<AppointmentCreatedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: AppointmentCreatedEvent) {
    prisonAppointmentRepository.findById(event.additionalInformation.appointmentId).ifPresentOrElse(
      { appointment -> manageExternalAppointmentsService.createAppointment(appointment) },
      {
        // Ignore, there is nothing we can do if we do not find the prison appointment
        log.warn("Prison appointment with ID ${event.additionalInformation.appointmentId} not found")
      },
    )
  }
}
