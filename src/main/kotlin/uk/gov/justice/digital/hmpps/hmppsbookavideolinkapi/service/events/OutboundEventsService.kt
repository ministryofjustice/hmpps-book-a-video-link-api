package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.APPOINTMENT_CREATED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.VIDEO_BOOKING_AMENDED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.VIDEO_BOOKING_CANCELLED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.VIDEO_BOOKING_CREATED

@Service
class OutboundEventsService(private val outboundEventsPublisher: OutboundEventsPublisher) {

  fun send(domainEventType: DomainEventType, identifier: Long) {
    when (domainEventType) {
      APPOINTMENT_CREATED -> send(AppointmentCreatedEvent(identifier))
      VIDEO_BOOKING_CREATED -> send(VideoBookingCreatedEvent(identifier))
      VIDEO_BOOKING_CANCELLED -> send(VideoBookingCancelledEvent(identifier))
      VIDEO_BOOKING_AMENDED -> send(VideoBookingAmendedEvent(identifier))
      else -> throw IllegalArgumentException("Unsupported domain event $domainEventType")
    }
  }

  private fun send(event: DomainEvent<*>) {
    outboundEventsPublisher.send(event)
  }
}
