package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.APPOINTMENT_CREATED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.VIDEO_BOOKING_CREATED

@Service
class OutboundEventsService(
  private val outboundEventsPublisher: OutboundEventsPublisher,
) {
  fun send(domainEventType: DomainEventType, identifier: Long) {
    when (domainEventType) {
      APPOINTMENT_CREATED -> send(domainEventType.toOutboundEvent(AppointmentInformation(identifier)))
      VIDEO_BOOKING_CREATED -> send(domainEventType.toOutboundEvent(VideoBookingInformation(identifier)))
    }
  }

  private fun send(event: OutboundHMPPSDomainEvent) {
    outboundEventsPublisher.send(event)
  }
}
