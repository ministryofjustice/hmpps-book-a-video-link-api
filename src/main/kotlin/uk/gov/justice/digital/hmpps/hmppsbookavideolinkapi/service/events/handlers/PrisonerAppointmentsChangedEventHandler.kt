package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Movement.MovementType.REL
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Movement.MovementType.TRN
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerAppointmentsChangedEvent

/**
 * This event is raised on the back of a prison user deciding what to do with a prisoners appointments on the back of a
 * movement (via NOMIS) e.g. transfer, release. They are given the option to cancel any existing appointments.
 */
@Component
class PrisonerAppointmentsChangedEventHandler(
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val prisonApiClient: PrisonApiClient,
  private val bookingFacade: BookingFacade,
  private val timeSource: TimeSource,
) : DomainEventHandler<PrisonerAppointmentsChangedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: PrisonerAppointmentsChangedEvent) {
    if (event.isCancelAppointments()) {
      val now = timeSource.now()

      val bookings = prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter(event.prisonerNumber(), now.toLocalDate(), now.toLocalTime())
        .map { it.videoBooking.videoBookingId }
        .distinct()

      if (bookings.isNotEmpty()) {
        val movementType = prisonApiClient.getLatestPrisonerMovementOnDate(event.prisonerNumber(), now.toLocalDate())

        when (movementType) {
          REL -> {
            log.info("PRISONER_APPOINTMENTS_CHANGED_EVENT: processing release for prisoner ${event.prisonerNumber()} - $event")
            bookings.forEach { bookingFacade.prisonerReleased(it, UserService.getServiceAsUser()) }
          }
          TRN -> {
            log.info("PRISONER_APPOINTMENTS_CHANGED_EVENT: processing transfer for prisoner ${event.prisonerNumber()} - $event")
            bookings.forEach { bookingFacade.prisonerTransferred(it, UserService.getServiceAsUser()) }
          }
          null -> log.info("PRISONER_APPOINTMENTS_CHANGED_EVENT: no-op - last movement type is null for prisoner ${event.prisonerNumber()} - $event")
          else -> log.info("PRISONER_APPOINTMENTS_CHANGED_EVENT: no-op - last movement type ${movementType.value} for prisoner ${event.prisonerNumber()} - $event")
        }

        return
      }
    }

    log.info("PRISONER_APPOINTMENTS_CHANGED_EVENT: no action taken for prisoner ${event.prisonerNumber()} - $event.")
  }
}
