package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerReceivedEvent

/**
 * We have seen on a quite a few occasions prisoners moved to a different prison but their existing appointments are not
 * cancelled (in NOMIS) when moved. This has a knock on effect with any existing bookings at their previous prison, we
 * are left with bookings which are no longer valid. Under normal circumstances we would expect the prisoner to
 * return to the original prison, but they are not. Our only real option here is to cancel existing bookings at the
 * previous prison and flag as transferred. The courts, probation teams and prisons will be notified via transfer emails
 * of any cancellations so the appropriate action can be taken if needed e.g. rebook.
 */
@Component
class PrisonerReceivedEventHandler(
  private val timeSource: TimeSource,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val bookingFacade: BookingFacade,
) : DomainEventHandler<PrisonerReceivedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: PrisonerReceivedEvent) {
    findAllFutureBookingsAtDifferentPrisonTo(event).forEach { futureBooking ->
      // TODO consider if we want/need different email templates to the normal transfer ones.
      bookingFacade.prisonerTransferred(futureBooking.videoBookingId, UserService.getServiceAsUser())
    }
  }

  private fun findAllFutureBookingsAtDifferentPrisonTo(event: PrisonerReceivedEvent) = run {
    val now = timeSource.now()

    prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter(
      event.prisonerNumber(),
      now.toLocalDate(),
      now.toLocalTime(),
    ).filterNot { pa -> pa.prisonCode() == event.prisonCode() }.map(PrisonAppointment::videoBooking).distinct().also {
      if (it.isEmpty()) {
        log.info("PRISONER RECEIVED: no bookings affected by $event")
      } else {
        log.info("PRISONER RECEIVED: bookings ${it.map { b -> b.videoBookingId }} affected by $event")
      }
    }
  }
}
