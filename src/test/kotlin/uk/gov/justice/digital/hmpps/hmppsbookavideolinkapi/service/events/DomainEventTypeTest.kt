package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.APPOINTMENT_CREATED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.VIDEO_BOOKING_CANCELLED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.VIDEO_BOOKING_CREATED

class DomainEventTypeTest {

  private val mapper = jacksonObjectMapper().registerModules(JavaTimeModule())

  @Test
  fun `should map to correct inbound domain event `() {
    VIDEO_BOOKING_CREATED.toInboundEvent(mapper, rawMessage(VideoBookingCreatedEvent(1))) isInstanceOf VideoBookingCreatedEvent::class.java
    APPOINTMENT_CREATED.toInboundEvent(mapper, rawMessage(AppointmentCreatedEvent(1))) isInstanceOf AppointmentCreatedEvent::class.java
    VIDEO_BOOKING_CANCELLED.toInboundEvent(mapper, rawMessage(VideoBookingCancelledEvent(1))) isInstanceOf VideoBookingCancelledEvent::class.java
  }

  @Test
  fun `should be correct event type`() {
    VIDEO_BOOKING_CREATED.eventType isEqualTo "book-a-video-link.video-booking.created"
    APPOINTMENT_CREATED.eventType isEqualTo "book-a-video-link.appointment.created"
    VIDEO_BOOKING_CANCELLED.eventType isEqualTo "book-a-video-link.video-booking.cancelled"
  }

  private fun rawMessage(event: DomainEvent<*>) = mapper.writeValueAsString(event)
}
