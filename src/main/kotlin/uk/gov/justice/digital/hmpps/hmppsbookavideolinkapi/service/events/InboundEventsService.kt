package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.AppointmentCreatedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingCancelledEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingCreatedEventHandler

@Service
class InboundEventsService(
  private val appointmentCreatedEventHandler: AppointmentCreatedEventHandler,
  private val videoBookingCreatedEventHandler: VideoBookingCreatedEventHandler,
  private val videoBookingCancelledEventHandler: VideoBookingCancelledEventHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun process(event: DomainEvent<*>) {
    when (event) {
      is VideoBookingCreatedEvent -> videoBookingCreatedEventHandler.handle(event)
      is AppointmentCreatedEvent -> appointmentCreatedEventHandler.handle(event)
      is VideoBookingCancelledEvent -> videoBookingCancelledEventHandler.handle(event)
      else -> log.warn("Unsupported domain event ${event.javaClass.name}")
    }
  }
}
