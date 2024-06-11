package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class InboundEventsService(
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun process(event: InboundEvent) {
    when (event) {
      is InboundVideoBookingCreatedEvent -> manageExternalAppointmentsService.createAppointments(event.videoBookingId)
      else -> log.warn("Unsupported event ${event.javaClass.name}")
    }
  }
}
