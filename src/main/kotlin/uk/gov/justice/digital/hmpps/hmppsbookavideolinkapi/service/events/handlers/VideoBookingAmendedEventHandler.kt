package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingHistoryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingAmendedEvent
import kotlin.math.abs

@Component
class VideoBookingAmendedEventHandler(
  private val videoBookingRepository: VideoBookingRepository,
  private val bookingHistoryService: BookingHistoryService,
  private val outboundEventsService: OutboundEventsService,
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService,
) : DomainEventHandler<VideoBookingAmendedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: VideoBookingAmendedEvent) {
    videoBookingRepository
      .findById(abs(event.additionalInformation.videoBookingId))
      .ifPresentOrElse(
        { booking ->
          // Get the previous history row - the one before the one just added (which should always exist)
          val history = bookingHistoryService.getByVideoBookingId(booking.videoBookingId)
            .sortedByDescending { history -> history.createdTime }
            .let {
              if (event.additionalInformation.videoBookingId < 0) {
                require(booking.isBookingType(BookingType.PROBATION)) { "Booking type must be probation" }
                log.info("Processing negative videoBookingId ${event.additionalInformation.videoBookingId}")

                it[0]
              } else {
                log.info("Processing positive videoBookingId ${event.additionalInformation.videoBookingId}")

                it[1]
              }
            }

          // Cancel the appointments related to the previous state from history rows
          history.appointments().forEach {
            manageExternalAppointmentsService.cancelPreviousAppointment(it)
          }

          // Recreate appointments for the current state by sending APPOINTMENT_CREATED for each ID.
          booking.appointments().forEach {
            outboundEventsService.send(DomainEventType.APPOINTMENT_CREATED, it.prisonAppointmentId)
          }

          log.info("Processed BOOKING_AMENDED event for videoBookingId ${booking.videoBookingId}")
        },
        {
          // Ignore, there is nothing we can do if we do not find the booking
          log.warn("Video booking with ID ${event.additionalInformation.videoBookingId} not found")
        },
      )
  }
}
