package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerReleasedEvent
import java.time.LocalDate
import java.time.LocalTime

@Component
class PrisonerReleasedEventHandler(
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val bookingFacade: BookingFacade,
) : DomainEventHandler<PrisonerReleasedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: PrisonerReleasedEvent) {
    when {
      event.isTemporary() -> log.info("RELEASE EVENT HANDLER: Ignoring temporary release event $event")
      event.isTransferred() || event.isPermanent() -> processRelease(event)
      else -> log.warn("RELEASE EVENT HANDLER: Ignoring unknown release event $event")
    }
  }

  private fun processRelease(event: PrisonerReleasedEvent) {
    prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter(event.prisonerNumber(), LocalDate.now(), LocalTime.now())
      .ifEmpty {
        log.info("RELEASE EVENT HANDLER: no bookings affected for release event $event")
        emptyList()
      }
      .map { it.videoBooking.videoBookingId }
      .distinct()
      .forEach { booking ->
        if (event.isTransferred()) {
          log.info("RELEASE EVENT HANDLER: processing transfer event $event")
          bookingFacade.prisonerTransferred(booking, getServiceAsUser())
        } else if (event.isPermanent()) {
          log.info("RELEASE EVENT HANDLER: processing release event $event")
          bookingFacade.prisonerReleased(booking, getServiceAsUser())
        } else {
          log.info("RELEASE EVENT HANDLER: no action taken for release event $event")
        }
      }
  }
}
