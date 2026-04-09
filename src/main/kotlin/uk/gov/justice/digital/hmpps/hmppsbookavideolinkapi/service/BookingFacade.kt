package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest

interface BookingFacade {
  fun create(bookingRequest: CreateVideoBookingRequest, createdBy: User): Long
  fun amend(videoBookingId: Long, bookingRequest: AmendVideoBookingRequest, amendedBy: User): Long
  fun cancel(videoBookingId: Long, cancelledBy: PrisonUser)
  fun cancel(videoBookingId: Long, cancelledBy: ExternalUser)
  fun request(booking: RequestVideoBookingRequest, user: ExternalUser)
  fun cancel(videoBookingId: Long, cancelledBy: ServiceUser)
  fun courtHearingLinkReminder(videoBooking: VideoBooking, user: ServiceUser)
  fun sendProbationOfficerDetailsReminder(videoBooking: VideoBooking, user: ServiceUser)
  fun prisonerTransferred(videoBookingId: Long, user: ServiceUser)
  fun prisonerReleased(videoBookingId: Long, user: ServiceUser)
}

enum class BookingAction {
  CREATE,
  AMEND,
  CANCEL,
  COURT_HEARING_LINK_REMINDER,
  PROBATION_OFFICER_DETAILS_REMINDER,
  RELEASED,
  TRANSFERRED,
}
