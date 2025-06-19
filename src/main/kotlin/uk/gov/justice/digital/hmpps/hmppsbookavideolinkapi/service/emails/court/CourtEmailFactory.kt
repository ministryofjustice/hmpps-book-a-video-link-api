package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode.CANCELLED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction
import java.util.UUID

@Component
class CourtEmailFactory(@Value("\${default.court.video.url:}") private val defaultCourtVideoUrl: String) {
  fun user(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    main: PrisonAppointment,
    pre: PrisonAppointment?,
    post: PrisonAppointment?,
    locations: Map<UUID, Location>,
    action: BookingAction,
  ): Email? {
    booking.requireIsCourtBooking()

    require(contact.contactType == ContactType.USER) {
      "Incorrect contact type ${contact.contactType} for court user email"
    }

    return when (action) {
      BookingAction.CREATE -> NewCourtBookingUserEmail(
        address = contact.email!!,
        userName = contact.name ?: "Book Video",
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prisonerNumber = prisoner.prisonerNumber,
        court = booking.court!!.description,
        prison = prison.name,
        appointmentDate = main.appointmentDate,
        preAppointmentDetails = pre?.appointmentDetails(locations),
        mainAppointmentDetails = main.appointmentDetails(locations),
        postAppointmentDetails = post?.appointmentDetails(locations),
        comments = booking.staffNotesOrElseComments(),
        courtHearingLink = booking.fullCvpVideoUrl(),
      )
      BookingAction.AMEND -> AmendedCourtBookingUserEmail(
        address = contact.email!!,
        userName = contact.name ?: "Book Video",
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prisonerNumber = prisoner.prisonerNumber,
        court = booking.court!!.description,
        prison = prison.name,
        appointmentDate = main.appointmentDate,
        preAppointmentDetails = pre?.appointmentDetails(locations),
        mainAppointmentDetails = main.appointmentDetails(locations),
        postAppointmentDetails = post?.appointmentDetails(locations),
        comments = booking.staffNotesOrElseComments(),
        courtHearingLink = booking.fullCvpVideoUrl(),
      )
      BookingAction.CANCEL -> {
        booking.requireIsCancelled()

        CancelledCourtBookingUserEmail(
          address = contact.email!!,
          userName = contact.name ?: "Book Video",
          prisonerFirstName = prisoner.firstName,
          prisonerLastName = prisoner.lastName,
          prisonerNumber = prisoner.prisonerNumber,
          court = booking.court!!.description,
          prison = prison.name,
          appointmentDate = main.appointmentDate,
          preAppointmentInfo = pre?.appointmentInformation(locations),
          mainAppointmentInfo = main.appointmentInformation(locations),
          postAppointmentInfo = post?.appointmentInformation(locations),
          comments = booking.staffNotesOrElseComments(),
          courtHearingLink = booking.fullCvpVideoUrl(),
        )
      }

      else -> null
    }
  }

  fun court(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    main: PrisonAppointment,
    pre: PrisonAppointment?,
    post: PrisonAppointment?,
    locations: Map<UUID, Location>,
    action: BookingAction,
  ): Email? {
    booking.requireIsCourtBooking()

    require(contact.contactType == ContactType.COURT) {
      "Incorrect contact type ${contact.contactType} for court court email"
    }

    return when (action) {
      BookingAction.CREATE -> NewCourtBookingCourtEmail(
        address = contact.email!!,
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prisonerNumber = prisoner.prisonerNumber,
        court = booking.court!!.description,
        prison = prison.name,
        appointmentDate = main.appointmentDate,
        preAppointmentDetails = pre?.appointmentDetails(locations),
        mainAppointmentDetails = main.appointmentDetails(locations),
        postAppointmentDetails = post?.appointmentDetails(locations),
        comments = booking.staffNotesOrElseComments(),
        courtHearingLink = booking.fullCvpVideoUrl(),
      )

      BookingAction.AMEND -> AmendedCourtBookingCourtEmail(
        address = contact.email!!,
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prisonerNumber = prisoner.prisonerNumber,
        appointmentDate = main.appointmentDate,
        court = booking.court!!.description,
        prison = prison.name,
        preAppointmentDetails = pre?.appointmentDetails(locations),
        mainAppointmentDetails = main.appointmentDetails(locations),
        postAppointmentDetails = post?.appointmentDetails(locations),
        comments = booking.staffNotesOrElseComments(),
        courtHearingLink = booking.fullCvpVideoUrl(),
      )

      BookingAction.CANCEL -> {
        booking.requireIsCancelled()

        CancelledCourtBookingCourtEmail(
          address = contact.email!!,
          prisonerFirstName = prisoner.firstName,
          prisonerLastName = prisoner.lastName,
          prisonerNumber = prisoner.prisonerNumber,
          appointmentDate = main.appointmentDate,
          court = booking.court!!.description,
          prison = prison.name,
          preAppointmentInfo = pre?.appointmentInformation(locations),
          mainAppointmentInfo = main.appointmentInformation(locations),
          postAppointmentInfo = post?.appointmentInformation(locations),
          comments = booking.staffNotesOrElseComments(),
          courtHearingLink = booking.fullCvpVideoUrl(),
        )
      }

      BookingAction.RELEASED -> {
        booking.requireIsCancelled()

        ReleasedCourtBookingCourtEmail(
          address = contact.email!!,
          court = booking.court!!.description,
          prison = prison.name,
          prisonerFirstName = prisoner.firstName,
          prisonerLastName = prisoner.lastName,
          dateOfBirth = prisoner.dateOfBirth,
          prisonerNumber = prisoner.prisonerNumber,
          date = main.appointmentDate,
          preAppointmentInfo = pre?.appointmentInformation(locations),
          mainAppointmentInfo = main.appointmentInformation(locations),
          postAppointmentInfo = post?.appointmentInformation(locations),
          comments = booking.staffNotesOrElseComments(),
        )
      }

      BookingAction.TRANSFERRED -> {
        booking.requireIsCancelled()

        TransferredCourtBookingCourtEmail(
          address = contact.email!!,
          court = booking.court!!.description,
          prison = prison.name,
          prisonerFirstName = prisoner.firstName,
          prisonerLastName = prisoner.lastName,
          dateOfBirth = prisoner.dateOfBirth,
          prisonerNumber = prisoner.prisonerNumber,
          date = main.appointmentDate,
          preAppointmentInfo = pre?.appointmentInformation(locations),
          mainAppointmentInfo = main.appointmentInformation(locations),
          postAppointmentInfo = post?.appointmentInformation(locations),
          comments = booking.staffNotesOrElseComments(),
        )
      }

      BookingAction.COURT_HEARING_LINK_REMINDER -> CourtHearingLinkReminderEmail(
        address = contact.email!!,
        court = booking.court!!.description,
        prison = prison.name,
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prisonerNumber = prisoner.prisonerNumber,
        date = main.appointmentDate,
        preAppointmentInfo = pre?.appointmentInformation(locations),
        mainAppointmentInfo = main.appointmentInformation(locations),
        postAppointmentInfo = post?.appointmentInformation(locations),
        comments = booking.staffNotesOrElseComments(),
        bookingId = booking.videoBookingId.toString(),
      )

      else -> null
    }
  }

  fun prison(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    contacts: Collection<BookingContact>,
    main: PrisonAppointment,
    pre: PrisonAppointment?,
    post: PrisonAppointment?,
    locations: Map<UUID, Location>,
    action: BookingAction,
  ): Email? {
    booking.requireIsCourtBooking()

    require(contact.contactType == ContactType.PRISON) {
      "Incorrect contact type ${contact.contactType} for court prison email"
    }

    val primaryCourtContact = contacts.primaryCourtContact()

    return when (action) {
      BookingAction.CREATE -> {
        if (primaryCourtContact != null) {
          NewCourtBookingPrisonCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            courtEmailAddress = primaryCourtContact.email!!,
            prison = prison.name,
            appointmentDate = main.appointmentDate,
            preAppointmentDetails = pre?.appointmentDetails(locations),
            mainAppointmentDetails = main.appointmentDetails(locations),
            postAppointmentDetails = post?.appointmentDetails(locations),
            comments = booking.staffNotesOrElseComments(),
            courtHearingLink = booking.fullCvpVideoUrl(),
          )
        } else {
          NewCourtBookingPrisonNoCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            prison = prison.name,
            appointmentDate = main.appointmentDate,
            preAppointmentDetails = pre?.appointmentDetails(locations),
            mainAppointmentDetails = main.appointmentDetails(locations),
            postAppointmentDetails = post?.appointmentDetails(locations),
            comments = booking.staffNotesOrElseComments(),
            courtHearingLink = booking.fullCvpVideoUrl(),
          )
        }
      }

      BookingAction.AMEND -> {
        if (primaryCourtContact != null) {
          AmendedCourtBookingPrisonCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            courtEmailAddress = primaryCourtContact.email!!,
            prison = prison.name,
            appointmentDate = main.appointmentDate,
            preAppointmentDetails = pre?.appointmentDetails(locations),
            mainAppointmentDetails = main.appointmentDetails(locations),
            postAppointmentDetails = post?.appointmentDetails(locations),
            comments = booking.staffNotesOrElseComments(),
            courtHearingLink = booking.fullCvpVideoUrl(),
          )
        } else {
          AmendedCourtBookingPrisonNoCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            prison = prison.name,
            appointmentDate = main.appointmentDate,
            preAppointmentDetails = pre?.appointmentDetails(locations),
            mainAppointmentDetails = main.appointmentDetails(locations),
            postAppointmentDetails = post?.appointmentDetails(locations),
            comments = booking.staffNotesOrElseComments(),
            courtHearingLink = booking.fullCvpVideoUrl(),
          )
        }
      }

      BookingAction.CANCEL -> {
        booking.requireIsCancelled()

        if (primaryCourtContact != null) {
          CancelledCourtBookingPrisonCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            courtEmailAddress = primaryCourtContact.email!!,
            prison = prison.name,
            appointmentDate = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.staffNotesOrElseComments(),
            courtHearingLink = booking.fullCvpVideoUrl(),
          )
        } else {
          CancelledCourtBookingPrisonNoCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            prison = prison.name,
            appointmentDate = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.staffNotesOrElseComments(),
            courtHearingLink = booking.fullCvpVideoUrl(),
          )
        }
      }

      BookingAction.RELEASED -> {
        booking.requireIsCancelled()

        if (primaryCourtContact != null) {
          // Note: primary contact is only used to determine which template to use, it is not used in the template.
          ReleasedCourtBookingPrisonCourtEmail(
            address = contact.email!!,
            court = booking.court!!.description,
            prison = prison.name,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            dateOfBirth = prisoner.dateOfBirth,
            prisonerNumber = prisoner.prisonerNumber,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.staffNotesOrElseComments(),
          )
        } else {
          ReleasedCourtBookingPrisonNoCourtEmail(
            address = contact.email!!,
            court = booking.court!!.description,
            prison = prison.name,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            dateOfBirth = prisoner.dateOfBirth,
            prisonerNumber = prisoner.prisonerNumber,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.staffNotesOrElseComments(),
          )
        }
      }

      BookingAction.TRANSFERRED -> {
        booking.requireIsCancelled()

        // Note: primary contact is only used to determine which template to use, it is not used in the template.
        if (primaryCourtContact != null) {
          TransferredCourtBookingPrisonCourtEmail(
            address = contact.email!!,
            court = booking.court!!.description,
            prison = prison.name,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            dateOfBirth = prisoner.dateOfBirth,
            prisonerNumber = prisoner.prisonerNumber,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.staffNotesOrElseComments(),
          )
        } else {
          TransferredCourtBookingPrisonNoCourtEmail(
            address = contact.email!!,
            court = booking.court!!.description,
            prison = prison.name,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            dateOfBirth = prisoner.dateOfBirth,
            prisonerNumber = prisoner.prisonerNumber,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.staffNotesOrElseComments(),
          )
        }
      }

      else -> null
    }
  }

  private fun PrisonAppointment.appointmentDetails(locations: Map<UUID, Location>) = AppointmentDetails(locations.room(this), startTime, endTime, locations.prisonVideoUrl(this))

  private fun PrisonAppointment.appointmentInformation(locations: Map<UUID, Location>) = "${locations.room(prisonLocationId)} - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"

  private fun Map<UUID, Location>.room(id: UUID) = this[id]?.description ?: ""

  private fun Map<UUID, Location>.room(pa: PrisonAppointment) = this[pa.prisonLocationId]?.description ?: ""

  private fun Map<UUID, Location>.prisonVideoUrl(pa: PrisonAppointment) = this[pa.prisonLocationId]?.extraAttributes?.prisonVideoUrl

  private fun Collection<BookingContact>.primaryCourtContact() = singleOrNull { it.contactType == ContactType.COURT && it.primaryContact }

  private fun VideoBooking.requireIsCourtBooking() {
    require(isBookingType(COURT)) { "Booking ID $videoBookingId is not a court booking" }
  }

  private fun VideoBooking.requireIsCancelled() {
    require(isStatus(CANCELLED)) { "Booking ID $videoBookingId is not a cancelled" }
  }

  private fun VideoBooking.staffNotesOrElseComments() = notesForStaff ?: comments

  private fun VideoBooking.fullCvpVideoUrl() = hmctsNumber?.let { "HMCTS$it@$defaultCourtVideoUrl" } ?: videoUrl
}
