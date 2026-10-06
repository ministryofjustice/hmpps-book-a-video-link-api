package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade.BookingAction
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking

/**
 * Represents the details of a video link booking and its associated actions for a prisoner.
 *
 * This sealed class provides a base structure and specific implementations for various actions
 * that can be performed on a booking, such as creation, amendments, cancellations, or reminders.
 *
 * Each booking is associated with a prisoner and additional details that describe the booking
 * context, such as court descriptions, hearing times, and associated meeting details.
 *
 * @property booking The video link booking associated with this instance.
 * @property prisoner The prisoner associated with this booking.
 * @property action The action performed on this booking.
 */
sealed class BookingDetails private constructor(
  protected val booking: VideoLinkBooking,
  protected val prisoner: Prisoner,
  val action: BookingAction,
) {
  val videoLinkBookingId = booking.videoLinkBookingId
  val bookingType = booking.bookingType
  val courtDescription = booking.courtDescription
  val preHearing = booking.prisonAppointments.singleOrNull { it.appointmentType == "VLB_COURT_PRE" }
  val mainHearing = booking.prisonAppointments.singleOrNull { it.appointmentType == "VLB_COURT_MAIN" }
  val postHearing = booking.prisonAppointments.singleOrNull { it.appointmentType == "VLB_COURT_POST" }
  val hmctsNumber = booking.hmctsNumber
  val probationTeamDescription = booking.probationTeamDescription
  val probationMeetingType = booking.probationMeetingType?.description
  val mainMeeting = booking.prisonAppointments.singleOrNull { it.appointmentType == "VLB_PROBATION" }
  val additionalBookingDetails = booking.additionalBookingDetails
  val notesForStaff = booking.notesForStaff
  val videoLinkUrl = booking.videoLinkUrl
  val prisonName = (mainHearing ?: mainMeeting)!!.prisonName
  val prisonerNumber = prisoner.prisonerNumber
  val prisonerFirstName = prisoner.firstName
  val prisonerLastName = prisoner.lastName
  val prisonerDateOfBirth = prisoner.dateOfBirth

  companion object {
    fun create(newBooking: VideoLinkBooking, prisoner: Prisoner) = Create(newBooking, prisoner)
    fun amended(oldBooking: VideoLinkBooking, amendedBooking: VideoLinkBooking, prisoner: Prisoner) = Amended(oldBooking, amendedBooking, prisoner)
    fun cancelled(cancelledBooking: VideoLinkBooking, prisoner: Prisoner) = Cancelled(cancelledBooking, prisoner)
    fun released(cancelledBooking: VideoLinkBooking, prisoner: Prisoner) = Released(cancelledBooking, prisoner)
    fun transferred(cancelledBooking: VideoLinkBooking, prisoner: Prisoner) = Transferred(cancelledBooking, prisoner)
    fun courtHearingLinkReminder(booking: VideoLinkBooking, prisoner: Prisoner) = CourtHearingLinkReminder(booking, prisoner)
    fun probationOfficerEmailReminder(booking: VideoLinkBooking, prisoner: Prisoner) = ProbationOfficerEmailReminder(booking, prisoner)
  }

  class Create(booking: VideoLinkBooking, prisoner: Prisoner) : BookingDetails(booking, prisoner, BookingAction.CREATE)

  class Amended(
    private val oldBooking: VideoLinkBooking,
    amendedBooking: VideoLinkBooking,
    prisoner: Prisoner,
  ) : BookingDetails(amendedBooking, prisoner, BookingAction.AMEND) {
    init {
      require(oldBooking.videoLinkBookingId == amendedBooking.videoLinkBookingId) {
        "Old booking ID ${oldBooking.videoLinkBookingId} does not match amended booking ID ${amendedBooking.videoLinkBookingId}"
      }
    }

    val oldPreHearing = oldBooking.prisonAppointments.singleOrNull { it.appointmentType == "VLB_COURT_PRE" }
    val oldMainHearing = oldBooking.prisonAppointments.singleOrNull { it.appointmentType == "VLB_COURT_MAIN" }
    val oldPostHearing = oldBooking.prisonAppointments.singleOrNull { it.appointmentType == "VLB_COURT_POST" }
    val oldMainMeeting = oldBooking.prisonAppointments.singleOrNull { it.appointmentType == "VLB_PROBATION" }

    fun isRescheduled() = run {
      oldBooking.overallStartDateTime() != booking.overallStartDateTime() || oldBooking.overallEndDateTime() != booking.overallEndDateTime()
    }
  }

  class Cancelled(booking: VideoLinkBooking, prisoner: Prisoner) : BookingDetails(booking, prisoner, BookingAction.CANCEL) {
    init {
      require(booking.statusCode == BookingStatus.CANCELLED) {
        "Booking $videoLinkBookingId is not cancelled"
      }
    }
  }

  class Released(booking: VideoLinkBooking, prisoner: Prisoner) : BookingDetails(booking, prisoner, BookingAction.RELEASED) {
    init {
      require(booking.statusCode == BookingStatus.CANCELLED) {
        "Booking $videoLinkBookingId is not cancelled"
      }
    }
  }

  class Transferred(booking: VideoLinkBooking, prisoner: Prisoner) : BookingDetails(booking, prisoner, BookingAction.TRANSFERRED) {
    init {
      require(booking.statusCode == BookingStatus.CANCELLED) {
        "Booking $videoLinkBookingId is not cancelled"
      }
    }
  }

  class CourtHearingLinkReminder(booking: VideoLinkBooking, prisoner: Prisoner) : BookingDetails(booking, prisoner, BookingAction.COURT_HEARING_LINK_REMINDER)

  class ProbationOfficerEmailReminder(booking: VideoLinkBooking, prisoner: Prisoner) : BookingDetails(booking, prisoner, BookingAction.PROBATION_OFFICER_DETAILS_REMINDER)
}
