package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.DeliusUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

/**
 * Video booking access checks are only relevant to External and Delius self-service users.
 * For Prison users these checks will be silently ignored.
 */

fun checkVideoBookingAccess(user: User, booking: VideoBooking) {
  if (user !is ExternalUser && user !is DeliusUser) {
    return
  }

  if (!booking.prisonIsEnabledForSelfService()) {
    throw VideoBookingAccessException("Prison with code ${booking.prisonCode()} for booking with id ${booking.videoBookingId} is not self service")
  }

  val hasAccess = when (user) {
    is ExternalUser -> booking.isAccessibleBy(user)
    is DeliusUser -> booking.isAccessibleBy(user)
    else -> false
  }

  if (!hasAccess) {
    throw VideoBookingAccessException("Video booking ${booking.videoBookingId} is not accessible by user")
  }
}

// Unknown courts or probation teams cannot be accessed by external users (exclusive to prison users)
private fun VideoBooking.isAccessibleBy(user: ExternalUser) = when {
  user.isCourtUser && isBookingType(COURT) -> court?.let { !it.isUnknown() && user.hasAccessTo(it) } == true
  user.isProbationUser && isBookingType(PROBATION) -> probationTeam?.let { !it.isUnknown() && user.hasAccessTo(it) } == true
  else -> false
}

// Unknown probation teams cannot be accessed by Delius probation users (exclusive to prison users)
private fun VideoBooking.isAccessibleBy(user: DeliusUser) = user.isProbationUser &&
  isBookingType(PROBATION) &&
  probationTeam?.let { !it.isUnknown() && user.hasAccessTo(it) } == true

class VideoBookingAccessException(message: String) : RuntimeException(message)
