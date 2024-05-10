package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest

/**
 * This facade exists to ensure booking related transactions are fully committed prior to sending any emails.
 */
@Component
class BookingFacade(
  private val createVideoBookingService: CreateVideoBookingService,
  private val emailService: EmailService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun create(booking: CreateVideoBookingRequest): Long =
    createVideoBookingService.create(booking).let { newBooking ->
      sendNewCourtBookingEmail(newBooking)
      newBooking.videoBookingId
    }

  private fun sendNewCourtBookingEmail(newBooking: VideoBooking) {
    log.info("TODO - send new court booking email.")
  }
}
