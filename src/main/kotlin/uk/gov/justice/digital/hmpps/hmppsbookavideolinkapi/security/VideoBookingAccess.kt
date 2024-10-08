package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserType

/**
 * Video booking access checks are only relevant for external users.
 *
 * If the current user is not an external user then it will just be ignored.
 */
fun checkVideoBookingAccess(externalUser: User, booking: VideoBooking) {
  if (externalUser.isUserType(UserType.EXTERNAL)) {
    if (!booking.prisonIsEnabledForSelfService()) {
      throw VideoBookingAccessException("Prison with code ${booking.prisonCode()} for booking with id ${booking.videoBookingId} is not self service")
    }

    externalUser as ExternalUser

    if (externalUser.isCourtUser && booking.isCourtBooking() && booking.isAccessibleBy(externalUser)) return
    if (externalUser.isProbationUser && booking.isProbationBooking() && booking.isAccessibleBy(externalUser)) return

    throw VideoBookingAccessException("Required role to view a ${booking.bookingType} booking is missing")
  }
}

// Unknown courts or probation teams cannot be accessed by external users, they will only ever be set up by prison users
private fun VideoBooking.isAccessibleBy(user: ExternalUser) = (user.isCourtUser && court?.isUnknown() == false) || (user.isProbationUser && probationTeam?.isUnknown() == false)

class VideoBookingAccessException(message: String) : RuntimeException(message)
