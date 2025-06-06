package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingCancelledEvent

@Component
class VideoBookingCancelledEventHandler(
  private val videoBookingRepository: VideoBookingRepository,
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService,
) : DomainEventHandler<VideoBookingCancelledEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: VideoBookingCancelledEvent) {
    videoBookingRepository.findById(event.additionalInformation.videoBookingId).ifPresentOrElse(
      { booking ->
        booking.appointments().forEach {
          manageExternalAppointmentsService.cancelCurrentAppointment(it)
        }
      },
      {
        // Ignore, there is nothing we can do if we do not find the booking
        log.warn("Video booking with ID ${event.additionalInformation.videoBookingId} not found")
      },
    )
  }
}
