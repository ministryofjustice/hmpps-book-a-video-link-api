package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction.AMEND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction.CANCEL
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction.CREATE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction.RELEASED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction.TRANSFERRED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

object TelemetryEventFactory {

  fun event(action: BookingAction, booking: VideoBooking, user: User): TelemetryEvent? = run {
    when {
      booking.isBookingType(COURT) -> courtEvent(action, booking, user)
      booking.isBookingType(PROBATION) -> probationEvent(action, booking, user)
      else -> throw IllegalArgumentException("Unsupported booking type for telemetry.")
    }
  }

  private fun courtEvent(action: BookingAction, booking: VideoBooking, user: User) = run {
    when (action) {
      CREATE -> CourtBookingCreatedTelemetryEvent(booking)
      AMEND -> CourtBookingAmendedTelemetryEvent(booking, user)
      CANCEL -> CourtBookingCancelledTelemetryEvent.user(booking, user)
      TRANSFERRED -> CourtBookingCancelledTelemetryEvent.transferred(booking)
      RELEASED -> CourtBookingCancelledTelemetryEvent.released(booking)
      else -> null
    }
  }

  private fun probationEvent(action: BookingAction, booking: VideoBooking, user: User) = run {
    when (action) {
      CREATE -> ProbationBookingCreatedTelemetryEvent(booking)
      AMEND -> ProbationBookingAmendedTelemetryEvent(booking, user)
      CANCEL -> ProbationBookingCancelledTelemetryEvent.user(booking, user)
      TRANSFERRED -> ProbationBookingCancelledTelemetryEvent.transferred(booking)
      RELEASED -> ProbationBookingCancelledTelemetryEvent.released(booking)
      else -> null
    }
  }
}
