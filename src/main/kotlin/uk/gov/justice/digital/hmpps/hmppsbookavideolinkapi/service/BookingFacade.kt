package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
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

  fun create(bookingRequest: CreateVideoBookingRequest, username: String): Long {
    val (booking, prisoner) = createVideoBookingService.create(bookingRequest, username)

    when (bookingRequest.bookingType!!) {
      BookingType.COURT -> sendNewCourtBookingEmail(booking, prisoner)
      BookingType.PROBATION -> sendNewProbationBookingEmail(booking, prisoner)
    }

    return booking.videoBookingId
  }

  private fun sendNewCourtBookingEmail(booking: VideoBooking, prisoner: Prisoner) {
    log.info("TODO - send new court booking email.")

    // Agreed with Tim will be using a ContactService to pull back the necessary contact information related to the booking just created.
    // Multiple contacts will result in multiple emails i.e. one email per contact.
  }

  private fun sendNewProbationBookingEmail(booking: VideoBooking, prisoner: Prisoner) {
    log.info("TODO - send new probation booking email.")

    // Agreed with Tim will be using a ContactService to pull back the necessary contact information related to the booking just created.
    // Multiple contacts will result in multiple emails i.e. one email per contact.
  }
}
