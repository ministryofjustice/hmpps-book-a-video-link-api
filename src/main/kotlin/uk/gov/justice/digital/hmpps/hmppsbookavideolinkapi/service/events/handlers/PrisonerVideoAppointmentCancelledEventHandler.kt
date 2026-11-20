package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerVideoAppointmentCancelledEvent

/**
 * The nature of this event handler is to do our best to cancel (or delete an appointment) providing we can match the
 * details in the event to a single appointment.
 *
 * A booking is cancelled if the appointment removed in NOMIS is a main court hearing or a probation meeting. If the
 * cancelled appointment is a pre-meeting or post-meeting then appointment is removed from the court booking.
 *
 * If zero or more than one matching appointment is found then we log and ignore this event entirely.
 *
 * For A&A rolled-out prisons we can safely ignore this event. Appointments cannot be changed in NOMIS for rolled-out
 * prisons, the screens are disabled. However, the event still gets raised on the back of syncing from A&A when we delete
 * appointments in BVLS and then in A&A.
 */
@Component
class PrisonerVideoAppointmentCancelledEventHandler : DomainEventHandler<PrisonerVideoAppointmentCancelledEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: PrisonerVideoAppointmentCancelledEvent) {
    log.info("VIDEO APPOINTMENT CANCELLED EVENT HANDLER: Ignoring event $event all prisons are now rolled out")
    // TODO need to remove event from cloud platform config.
  }
}
