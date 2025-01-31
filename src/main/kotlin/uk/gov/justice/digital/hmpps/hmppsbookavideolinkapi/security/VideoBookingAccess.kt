package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

/**
 * Video booking access checks are only relevant for external users.
 *
 * If the current user is not an external user then it will just be ignored.
 */
fun checkVideoBookingAccess(externalUser: User, booking: VideoBooking) {
  if (externalUser is ExternalUser) {
    if (!booking.prisonIsEnabledForSelfService()) {
      throw VideoBookingAccessException("Prison with code ${booking.prisonCode()} for booking with id ${booking.videoBookingId} is not self service")
    }

    if (externalUser.isCourtUser && booking.isBookingType(COURT) && booking.isAccessibleBy(externalUser)) return
    if (externalUser.isProbationUser && booking.isBookingType(PROBATION) && booking.isAccessibleBy(externalUser)) return

    throw VideoBookingAccessException("Video booking ${booking.videoBookingId} is not accessible by user")
  }
}

// Unknown courts or probation teams cannot be accessed by external users, they will only ever be set up by prison users
private fun VideoBooking.isAccessibleBy(user: ExternalUser) =
  (user.isCourtUser && court?.isUnknown() == false && user.hasAccessTo(court)) ||
    (user.isProbationUser && probationTeam?.isUnknown() == false && user.hasAccessTo(probationTeam))

class VideoBookingAccessException(message: String) : RuntimeException(message)
