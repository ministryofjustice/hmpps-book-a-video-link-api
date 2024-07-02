package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ServiceName
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
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
      // Doing the temporary check first to avoid potentially unnecessary database calls.
      event.isTemporary() -> log.info("RELEASE EVENT HANDLER: Ignoring temporary release event $event")
      event.isPermanent() -> cancelFutureBookingsFor(event.prisonerNumber())
      else -> log.warn("RELEASE EVENT HANDLER: Ignoring unknown release event $event")
    }
  }

  private fun cancelFutureBookingsFor(prisonerNumber: String) {
    prisonAppointmentRepository.findPrisonerPrisonAppointmentsAfter(prisonerNumber, LocalDate.now(), LocalTime.now())
      .map { it.videoBooking.videoBookingId }
      .distinct()
      .forEach { booking ->
        log.info("RELEASE EVENT HANDLER: cancelling video booking $booking for prisoner number $prisonerNumber")
        bookingFacade.cancel(booking, ServiceName.BOOK_A_VIDEO_LINK_SERVICE.name)
      }
  }
}
