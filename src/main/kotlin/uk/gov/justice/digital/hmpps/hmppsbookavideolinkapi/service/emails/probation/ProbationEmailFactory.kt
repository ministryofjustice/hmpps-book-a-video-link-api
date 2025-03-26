package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AdditionalBookingDetail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode.CANCELLED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction

object ProbationEmailFactory {
  fun user(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    appointment: PrisonAppointment,
    location: Location,
    action: BookingAction,
    additionalBookingDetail: AdditionalBookingDetail?,
  ): Email? {
    booking.requireIsProbationBooking()

    require(contact.contactType == ContactType.USER) {
      "Incorrect contact type ${contact.contactType} for probation user email"
    }

    return when (action) {
      BookingAction.CREATE -> NewProbationBookingUserEmail(
        address = contact.email!!,
        prisonerNumber = prisoner.prisonerNumber,
        userName = contact.name ?: "Book Video",
        probationTeam = booking.probationTeam!!.description,
        appointmentDate = appointment.appointmentDate,
        appointmentInfo = appointment.appointmentInformation(location),
        comments = booking.comments,
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prison = prison.name,
        prisonVideoUrl = location.extraAttributes?.prisonVideoUrl,
        probationOfficerName = additionalBookingDetail?.contactName,
        probationOfficerEmailAddress = additionalBookingDetail?.contactEmail,
        probationOfficerContactNumber = additionalBookingDetail?.contactNumber,
      )

      BookingAction.AMEND -> AmendedProbationBookingUserEmail(
        address = contact.email!!,
        prisonerNumber = prisoner.prisonerNumber,
        userName = contact.name ?: "Book Video",
        probationTeam = booking.probationTeam!!.description,
        appointmentDate = appointment.appointmentDate,
        appointmentInfo = appointment.appointmentInformation(location),
        comments = booking.comments,
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prison = prison.name,
      )

      BookingAction.CANCEL -> {
        booking.requireIsCancelled()

        CancelledProbationBookingUserEmail(
          address = contact.email!!,
          userName = contact.name!!,
          prisonerNumber = prisoner.prisonerNumber,
          prisonerFirstName = prisoner.firstName,
          prisonerLastName = prisoner.lastName,
          prison = prison.name,
          probationTeam = booking.probationTeam!!.description,
          appointmentDate = appointment.appointmentDate,
          appointmentInfo = appointment.appointmentInformation(location),
          comments = booking.comments,
        )
      }

      else -> null
    }
  }

  fun probation(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    appointment: PrisonAppointment,
    location: Location,
    action: BookingAction,
  ): Email? {
    booking.requireIsProbationBooking()

    require(contact.contactType == ContactType.PROBATION) {
      "Incorrect contact type ${contact.contactType} for probation probation email"
    }

    return when (action) {
      BookingAction.AMEND -> AmendedProbationBookingProbationEmail(
        address = contact.email!!,
        prisonerNumber = prisoner.prisonerNumber,
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        appointmentDate = appointment.appointmentDate,
        probationTeam = booking.probationTeam!!.description,
        comments = booking.comments,
        appointmentInfo = appointment.appointmentInformation(location),
        prison = prison.name,
      )

      BookingAction.CANCEL -> {
        booking.requireIsCancelled()

        CancelledProbationBookingProbationEmail(
          address = contact.email!!,
          prisonerNumber = prisoner.prisonerNumber,
          prisonerFirstName = prisoner.firstName,
          prisonerLastName = prisoner.lastName,
          appointmentDate = appointment.appointmentDate,
          probationTeam = booking.probationTeam!!.description,
          prison = prison.name,
          appointmentInfo = appointment.appointmentInformation(location),
          comments = booking.comments,
        )
      }

      BookingAction.RELEASED -> {
        booking.requireIsCancelled()

        ReleasedProbationBookingProbationEmail(
          address = contact.email!!,
          prisonerNumber = prisoner.prisonerNumber,
          prisonerFirstName = prisoner.firstName,
          prisonerLastName = prisoner.lastName,
          dateOfBirth = prisoner.dateOfBirth,
          appointmentDate = appointment.appointmentDate,
          probationTeam = booking.probationTeam!!.description,
          prison = prison.name,
          appointmentInfo = appointment.appointmentInformation(location),
          comments = booking.comments,
        )
      }

      BookingAction.TRANSFERRED -> {
        booking.requireIsCancelled()

        TransferredProbationBookingProbationEmail(
          address = contact.email!!,
          prisonerNumber = prisoner.prisonerNumber,
          prisonerFirstName = prisoner.firstName,
          prisonerLastName = prisoner.lastName,
          dateOfBirth = prisoner.dateOfBirth,
          appointmentDate = appointment.appointmentDate,
          probationTeam = booking.probationTeam!!.description,
          prison = prison.name,
          appointmentInfo = appointment.appointmentInformation(location),
          comments = booking.comments,
        )
      }

      else -> null
    }
  }

  fun prison(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    appointment: PrisonAppointment,
    location: Location,
    action: BookingAction,
    contacts: Collection<BookingContact>,
    additionalBookingDetail: AdditionalBookingDetail?,
  ): Email? {
    booking.requireIsProbationBooking()

    require(contact.contactType == ContactType.PRISON) {
      "Incorrect contact type ${contact.contactType} for prison probation email"
    }

    val primaryProbationContact = contacts.primaryProbationContact()

    return when (action) {
      BookingAction.CREATE -> {
        if (primaryProbationContact != null) {
          NewProbationBookingPrisonProbationEmail(
            address = contact.email!!,
            prisonerNumber = prisoner.prisonerNumber,
            probationTeam = booking.probationTeam!!.description,
            appointmentDate = appointment.appointmentDate,
            appointmentInfo = appointment.appointmentInformation(location),
            comments = booking.comments,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prison = prison.name,
            probationEmailAddress = primaryProbationContact.email!!,
            prisonVideoUrl = location.extraAttributes?.prisonVideoUrl,
            probationOfficerName = additionalBookingDetail?.contactName,
            probationOfficerEmailAddress = additionalBookingDetail?.contactEmail,
            probationOfficerContactNumber = additionalBookingDetail?.contactNumber,
          )
        } else {
          NewProbationBookingPrisonNoProbationEmail(
            address = contact.email!!,
            prisonerNumber = prisoner.prisonerNumber,
            probationTeam = booking.probationTeam!!.description,
            appointmentDate = appointment.appointmentDate,
            appointmentInfo = appointment.appointmentInformation(location),
            comments = booking.comments,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prison = prison.name,
          )
        }
      }

      BookingAction.AMEND -> {
        if (primaryProbationContact != null) {
          AmendedProbationBookingPrisonProbationEmail(
            address = contact.email!!,
            prisonerNumber = prisoner.prisonerNumber,
            probationTeam = booking.probationTeam!!.description,
            appointmentDate = appointment.appointmentDate,
            appointmentInfo = appointment.appointmentInformation(location),
            comments = booking.comments,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prison = prison.name,
            probationEmailAddress = primaryProbationContact.email!!,
          )
        } else {
          AmendedProbationBookingPrisonNoProbationEmail(
            address = contact.email!!,
            prisonerNumber = prisoner.prisonerNumber,
            probationTeam = booking.probationTeam!!.description,
            appointmentDate = appointment.appointmentDate,
            appointmentInfo = appointment.appointmentInformation(location),
            comments = booking.comments,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prison = prison.name,
          )
        }
      }

      BookingAction.CANCEL -> {
        booking.requireIsCancelled()

        if (primaryProbationContact != null) {
          CancelledProbationBookingPrisonProbationEmail(
            address = contact.email!!,
            prisonerNumber = prisoner.prisonerNumber,
            probationTeam = booking.probationTeam!!.description,
            appointmentDate = appointment.appointmentDate,
            appointmentInfo = appointment.appointmentInformation(location),
            comments = booking.comments,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prison = prison.name,
            probationEmailAddress = primaryProbationContact.email!!,
          )
        } else {
          CancelledProbationBookingPrisonNoProbationEmail(
            address = contact.email!!,
            prisonerNumber = prisoner.prisonerNumber,
            probationTeam = booking.probationTeam!!.description,
            appointmentDate = appointment.appointmentDate,
            appointmentInfo = appointment.appointmentInformation(location),
            comments = booking.comments,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prison = prison.name,
          )
        }
      }

      BookingAction.RELEASED -> {
        booking.requireIsCancelled()

        if (primaryProbationContact != null) {
          ReleasedProbationBookingPrisonProbationEmail(
            address = contact.email!!,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            dateOfBirth = prisoner.dateOfBirth,
            appointmentDate = appointment.appointmentDate,
            probationTeam = booking.probationTeam!!.description,
            probationEmailAddress = primaryProbationContact.email!!,
            prison = prison.name,
            appointmentInfo = appointment.appointmentInformation(location),
            comments = booking.comments,
          )
        } else {
          ReleasedProbationBookingPrisonNoProbationEmail(
            address = contact.email!!,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            dateOfBirth = prisoner.dateOfBirth,
            appointmentDate = appointment.appointmentDate,
            probationTeam = booking.probationTeam!!.description,
            prison = prison.name,
            appointmentInfo = appointment.appointmentInformation(location),
            comments = booking.comments,
          )
        }
      }

      BookingAction.TRANSFERRED -> {
        booking.requireIsCancelled()

        if (primaryProbationContact != null) {
          TransferredProbationBookingPrisonProbationEmail(
            address = contact.email!!,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            dateOfBirth = prisoner.dateOfBirth,
            appointmentDate = appointment.appointmentDate,
            probationTeam = booking.probationTeam!!.description,
            probationEmailAddress = primaryProbationContact.email!!,
            prison = prison.name,
            appointmentInfo = appointment.appointmentInformation(location),
            comments = booking.comments,
          )
        } else {
          TransferredProbationBookingPrisonNoProbationEmail(
            address = contact.email!!,
            prisonerNumber = prisoner.prisonerNumber,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            dateOfBirth = prisoner.dateOfBirth,
            appointmentDate = appointment.appointmentDate,
            probationTeam = booking.probationTeam!!.description,
            prison = prison.name,
            appointmentInfo = appointment.appointmentInformation(location),
            comments = booking.comments,
          )
        }
      }

      else -> null
    }
  }

  private fun VideoBooking.requireIsProbationBooking() {
    require(isBookingType(PROBATION)) { "Booking ID $videoBookingId is not a probation booking" }
  }

  private fun VideoBooking.requireIsCancelled() {
    require(isStatus(CANCELLED)) { "Booking ID $videoBookingId is not a cancelled" }
  }

  private fun PrisonAppointment.appointmentInformation(location: Location) = "${location.description} - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"

  private fun Collection<BookingContact>.primaryProbationContact() = singleOrNull { it.contactType == ContactType.PROBATION && it.primaryContact }
}
