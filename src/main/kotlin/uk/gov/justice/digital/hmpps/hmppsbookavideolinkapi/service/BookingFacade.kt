package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.BvlsRequestContext
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

  fun create(booking: CreateVideoBookingRequest, context: BvlsRequestContext): Long =
    createVideoBookingService.create(booking, context.username).let { newBooking ->
      sendNewCourtBookingEmail(newBooking)
      newBooking.videoBookingId
    }

  private fun sendNewCourtBookingEmail(booking: VideoBooking) {
    log.info("TODO - send new court booking email.")

    // Agreed with Tim will be using a SQL view to pull back the necessary contact information related to the booking just created.
    // Multiple contacts will result in multiple emails i.e. one email per contact.
  }
}
