package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerReleasedEvent
import java.time.LocalDateTime

@Component
class PrisonerReleasedEventHandler(
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val bookingFacade: BookingFacade,
  private val transactionHandler: TransactionHandler,
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
    val now = LocalDateTime.now()

    // A new transaction is needed so we can look at (lazily loaded) appointments but avoid wrapping the call to the
    // facade in the same transaction. Ideally, the facade needs to be excluded from transactions so the service layer
    // calls further down the call chain can be in their own transaction.
    transactionHandler.newSpringTransaction {
      prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter(
        event.prisonerNumber(),
        now.toLocalDate(),
        now.toLocalTime(),
      ).ifEmpty {
        log.info("RELEASE EVENT HANDLER: no bookings affected for release event $event")
        emptyList()
      }
        .map(PrisonAppointment::videoBooking)
        .distinctBy(VideoBooking::videoBookingId)
        .allBookingAppointmentsAreStillActive(now)
        .map(VideoBooking::videoBookingId)
    }.forEach { booking ->
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

  private fun List<VideoBooking>.allBookingAppointmentsAreStillActive(dateTime: LocalDateTime) = filter { booking ->
    booking.appointments().all { appointment -> appointment.start().isAfter(dateTime) }
  }
}
