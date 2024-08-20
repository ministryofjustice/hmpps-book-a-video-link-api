package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserType

/**
 * Video booking access checks are only relevant for external users.
 *
 * If the current user is not an external user then it will just be ignored.
 */
fun checkVideoBookingAccess(externalUser: User, booking: VideoBooking) {
  if (externalUser.isUserType(UserType.EXTERNAL)) {
    if (externalUser.isCourtUser && booking.isCourtBooking()) return
    if (externalUser.isProbationUser && booking.isProbationBooking()) return

    throw VideoBookingAccessException()
  }
}

class VideoBookingAccessException : RuntimeException()
