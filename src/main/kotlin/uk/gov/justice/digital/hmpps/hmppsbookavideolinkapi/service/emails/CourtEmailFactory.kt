package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReleasedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReleasedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ReleasedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TransferredCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TransferredCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TransferredCourtBookingPrisonNoCourtEmail

object CourtEmailFactory {

  fun user(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    main: PrisonAppointment,
    pre: PrisonAppointment?,
    post: PrisonAppointment?,
    locations: Map<String, Location>,
    action: BookingAction,
  ): Email? {
    booking.requireIsCourtBooking()

    require(contact.contactType == ContactType.USER) {
      "Incorrect contact type ${contact.contactType} for user email"
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
        date = main.appointmentDate,
        preAppointmentInfo = pre?.appointmentInformation(locations),
        mainAppointmentInfo = main.appointmentInformation(locations),
        postAppointmentInfo = post?.appointmentInformation(locations),
        comments = booking.comments,
      )
      BookingAction.AMEND -> AmendedCourtBookingUserEmail(
        address = contact.email!!,
        userName = contact.name ?: "Book Video",
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prisonerNumber = prisoner.prisonerNumber,
        court = booking.court!!.description,
        date = main.appointmentDate,
        preAppointmentInfo = pre?.appointmentInformation(locations),
        mainAppointmentInfo = main.appointmentInformation(locations),
        postAppointmentInfo = post?.appointmentInformation(locations),
        comments = booking.comments,
      )
      BookingAction.CANCEL -> CancelledCourtBookingUserEmail(
        address = contact.email!!,
        userName = contact.name ?: "Book Video",
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prisonerNumber = prisoner.prisonerNumber,
        court = booking.court!!.description,
        prison = prison.name,
        date = main.appointmentDate,
        preAppointmentInfo = pre?.appointmentInformation(locations),
        mainAppointmentInfo = main.appointmentInformation(locations),
        postAppointmentInfo = post?.appointmentInformation(locations),
        comments = booking.comments,
      )

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
    locations: Map<String, Location>,
    action: BookingAction,
  ): Email? {
    booking.requireIsCourtBooking()

    require(contact.contactType == ContactType.COURT) {
      "Incorrect contact type ${contact.contactType} for court email"
    }

    return when (action) {
      BookingAction.CREATE -> {
        if (booking.createdByPrison) {
          NewCourtBookingCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            prison = prison.name,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
          )
        } else {
          null
        }
      }

      BookingAction.AMEND -> null // TODO: Inform court when the prison amends a booking

      BookingAction.CANCEL -> null // TODO: Inform court when the prison cancels a booking

      BookingAction.RELEASED -> ReleasedCourtBookingCourtEmail(
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
        comments = booking.comments,
      )

      BookingAction.TRANSFERRED -> TransferredCourtBookingCourtEmail(
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
        comments = booking.comments,
      )
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
    locations: Map<String, Location>,
    action: BookingAction,
  ): Email {
    booking.requireIsCourtBooking()

    require(contact.contactType == ContactType.PRISON) {
      "Incorrect contact type ${contact.contactType} for prison email"
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
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
          )
        } else {
          NewCourtBookingPrisonNoCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            prison = prison.name,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
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
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
          )
        } else {
          AmendedCourtBookingPrisonNoCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            prison = prison.name,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
          )
        }
      }

      BookingAction.CANCEL -> {
        if (primaryCourtContact != null) {
          CancelledCourtBookingPrisonCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            courtEmailAddress = primaryCourtContact.email!!,
            prison = prison.name,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
          )
        } else {
          CancelledCourtBookingPrisonNoCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            prison = prison.name,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
          )
        }
      }

      BookingAction.RELEASED ->
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
            comments = booking.comments,
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
            comments = booking.comments,
          )
        }

      BookingAction.TRANSFERRED ->
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
            comments = booking.comments,
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
            comments = booking.comments,
          )
        }
    }
  }

  private fun PrisonAppointment.appointmentInformation(locations: Map<String, Location>) =
    "${locations.room(prisonLocKey)} - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"

  private fun Map<String, Location>.room(key: String) = this[key]?.localName ?: ""

  private fun Collection<BookingContact>.primaryCourtContact() = singleOrNull { it.contactType == ContactType.COURT && it.primaryContact }

  private fun VideoBooking.requireIsCourtBooking() {
    require(isCourtBooking()) { "Booking ID $videoBookingId is not a court booking" }
  }
}
