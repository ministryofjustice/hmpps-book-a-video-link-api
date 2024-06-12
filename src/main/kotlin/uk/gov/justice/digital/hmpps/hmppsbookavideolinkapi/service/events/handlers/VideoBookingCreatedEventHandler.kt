package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingCreatedEvent

/**
 * This handler is responsible for spawning 1-n appointment creation events which we actually handle in the
 * [AppointmentCreatedEventHandler]
 */
@Component
class VideoBookingCreatedEventHandler(
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val outboundEventsService: OutboundEventsService,
) : EventHandler<VideoBookingCreatedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: VideoBookingCreatedEvent) {
    videoBookingRepository.findById(event.videoBookingId).ifPresentOrElse(
      { booking ->
        prisonAppointmentRepository.findByVideoBooking(booking).forEach {
          outboundEventsService.send(DomainEventType.APPOINTMENT_CREATED, it.prisonAppointmentId)
        }
      },
      {
        // Ignore, there is nothing we can do if we do not find the booking
        log.warn("Video booking with ID ${event.videoBookingId} not found")
      },
    )
  }
}
