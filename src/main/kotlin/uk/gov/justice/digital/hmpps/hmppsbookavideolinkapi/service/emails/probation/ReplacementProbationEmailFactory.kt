package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.BookingDetails

object ReplacementProbationEmailFactory {
  fun user(bookingDetails: BookingDetails, userContact: BookingContact): VideoBookingEmail? {
    require(bookingDetails.bookingType == BookingType.PROBATION) {
      "Incorrect booking type ${bookingDetails.bookingType} for probation user email"
    }

    require(userContact.contactType == ContactType.USER) {
      "Incorrect contact type ${userContact.contactType} for probation user email"
    }

    val mainMeeting = bookingDetails.mainMeeting!!

    return when (bookingDetails) {
      is BookingDetails.Create -> NewProbationBookingUserEmail(
        address = userContact.email!!,
        prisonerNumber = bookingDetails.prisonerNumber,
        userName = userContact.name ?: "Book Video",
        probationTeam = bookingDetails.probationTeamDescription!!,
        appointmentDate = mainMeeting.appointmentDate,
        appointmentInfo = mainMeeting.appointmentInformation(),
        comments = bookingDetails.notesForStaff,
        prisonerFirstName = bookingDetails.prisonerFirstName,
        prisonerLastName = bookingDetails.prisonerLastName,
        prison = bookingDetails.prisonName,
        prisonVideoUrl = mainMeeting.prisonVideoUrl,
        probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
        probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
        probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
      )

      is BookingDetails.Amended -> {
        if (bookingDetails.isRescheduled()) {
          RescheduledProbationBookingUserEmail(
            address = userContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            userName = userContact.name ?: "Book Video",
            probationTeam = bookingDetails.probationTeamDescription!!,
            appointmentDate = mainMeeting.appointmentDate,
            appointmentInfo = mainMeeting.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prison = bookingDetails.prisonName,
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
            oldAppointmentDate = bookingDetails.oldMainMeeting!!.appointmentDate,
            oldAppointmentInfo = bookingDetails.oldMainMeeting.appointmentInformation(),
          )
        } else {
          AmendedProbationBookingUserEmail(
            address = userContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            userName = userContact.name ?: "Book Video",
            probationTeam = bookingDetails.probationTeamDescription!!,
            appointmentDate = mainMeeting.appointmentDate,
            appointmentInfo = mainMeeting.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prison = bookingDetails.prisonName,
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
          )
        }
      }

      is BookingDetails.Cancelled -> {
        CancelledProbationBookingUserEmail(
          address = userContact.email!!,
          userName = userContact.name!!,
          prisonerNumber = bookingDetails.prisonerNumber,
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          prison = bookingDetails.prisonName,
          probationTeam = bookingDetails.probationTeamDescription!!,
          appointmentDate = mainMeeting.appointmentDate,
          appointmentInfo = mainMeeting.appointmentInformation(),
          prisonVideoUrl = mainMeeting.prisonVideoUrl,
          probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
          probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
          probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
          comments = bookingDetails.notesForStaff,
        )
      }

      else -> null
    }
  }

  fun probation(bookingDetails: BookingDetails, probationContact: BookingContact): VideoBookingEmail? {
    require(probationContact.contactType == ContactType.PROBATION) {
      "Incorrect contact type ${probationContact.contactType} for probation probation email"
    }

    val mainMeeting = bookingDetails.mainMeeting!!

    return when (bookingDetails) {
      is BookingDetails.Create -> NewProbationBookingProbationEmail(
        address = probationContact.email!!,
        prisonerNumber = bookingDetails.prisonerNumber,
        probationTeam = bookingDetails.probationTeamDescription!!,
        appointmentDate = mainMeeting.appointmentDate,
        appointmentInfo = mainMeeting.appointmentInformation(),
        comments = bookingDetails.notesForStaff,
        prisonerFirstName = bookingDetails.prisonerFirstName,
        prisonerLastName = bookingDetails.prisonerLastName,
        prison = bookingDetails.prisonName,
        prisonVideoUrl = mainMeeting.prisonVideoUrl,
        probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
        probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
        probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
      )

      is BookingDetails.Amended -> {
        if (bookingDetails.isRescheduled()) {
          RescheduledProbationBookingProbationEmail(
            address = probationContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            appointmentDate = mainMeeting.appointmentDate,
            probationTeam = bookingDetails.probationTeamDescription!!,
            comments = bookingDetails.notesForStaff,
            appointmentInfo = mainMeeting.appointmentInformation(),
            prison = bookingDetails.prisonName,
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
            oldAppointmentDate = bookingDetails.oldMainMeeting!!.appointmentDate,
            oldAppointmentInfo = bookingDetails.oldMainMeeting.appointmentInformation(),
          )
        } else {
          AmendedProbationBookingProbationEmail(
            address = probationContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prison = bookingDetails.prisonName,
            appointmentDate = mainMeeting.appointmentDate,
            probationTeam = bookingDetails.probationTeamDescription!!,
            comments = bookingDetails.notesForStaff,
            appointmentInfo = mainMeeting.appointmentInformation(),
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
          )
        }
      }

      is BookingDetails.Cancelled -> {
        CancelledProbationBookingProbationEmail(
          address = probationContact.email!!,
          prisonerNumber = bookingDetails.prisonerNumber,
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          prison = bookingDetails.prisonName,
          appointmentDate = mainMeeting.appointmentDate,
          probationTeam = bookingDetails.probationTeamDescription!!,
          appointmentInfo = mainMeeting.appointmentInformation(),
          prisonVideoUrl = mainMeeting.prisonVideoUrl,
          probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
          probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
          probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
          comments = bookingDetails.notesForStaff,
        )
      }

      is BookingDetails.Released -> {
        ReleasedProbationBookingProbationEmail(
          address = probationContact.email!!,
          prisonerNumber = bookingDetails.prisonerNumber,
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          prison = bookingDetails.prisonName,
          dateOfBirth = bookingDetails.prisonerDateOfBirth,
          appointmentDate = mainMeeting.appointmentDate,
          probationTeam = bookingDetails.probationTeamDescription!!,
          appointmentInfo = mainMeeting.appointmentInformation(),
          prisonVideoUrl = mainMeeting.prisonVideoUrl,
          probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
          probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
          probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
          comments = bookingDetails.notesForStaff,
        )
      }

      is BookingDetails.Transferred -> {
        TransferredProbationBookingProbationEmail(
          address = probationContact.email!!,
          prisonerNumber = bookingDetails.prisonerNumber,
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          prison = bookingDetails.prisonName,
          dateOfBirth = bookingDetails.prisonerDateOfBirth,
          appointmentDate = mainMeeting.appointmentDate,
          probationTeam = bookingDetails.probationTeamDescription!!,
          appointmentInfo = mainMeeting.appointmentInformation(),
          prisonVideoUrl = mainMeeting.prisonVideoUrl,
          probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
          probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
          probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
          comments = bookingDetails.notesForStaff,
        )
      }

      is BookingDetails.ProbationOfficerEmailReminder -> {
        ProbationOfficerDetailsReminderEmail(
          address = probationContact.email!!,
          prisonerNumber = bookingDetails.prisonerNumber,
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          prison = bookingDetails.prisonName,
          appointmentDate = mainMeeting.appointmentDate,
          probationTeam = bookingDetails.probationTeamDescription!!,
          meetingType = bookingDetails.probationMeetingType!!,
          appointmentInfo = mainMeeting.appointmentInformation(),
          prisonVideoUrl = mainMeeting.prisonVideoUrl,
          comments = bookingDetails.notesForStaff,
          bookingId = bookingDetails.videoLinkBookingId.toString(),
        )
      }

      else -> null
    }
  }

  fun prison(
    bookingDetails: BookingDetails,
    prisonContact: BookingContact,
    probationContacts: Collection<BookingContact>,
  ): VideoBookingEmail? {
    require(prisonContact.contactType == ContactType.PRISON) {
      "Incorrect contact type ${prisonContact.contactType} for prison probation email"
    }

    val primaryProbationContact = probationContacts.primaryProbationContact()

    val mainMeeting = bookingDetails.mainMeeting!!

    return when (bookingDetails) {
      is BookingDetails.Create -> {
        if (primaryProbationContact != null) {
          NewProbationBookingPrisonProbationEmail(
            address = prisonContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            probationTeam = bookingDetails.probationTeamDescription!!,
            appointmentDate = mainMeeting.appointmentDate,
            appointmentInfo = mainMeeting.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prison = bookingDetails.prisonName,
            probationEmailAddress = primaryProbationContact.email!!,
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
          )
        } else {
          NewProbationBookingPrisonNoProbationEmail(
            address = prisonContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            probationTeam = bookingDetails.probationTeamDescription!!,
            appointmentDate = mainMeeting.appointmentDate,
            appointmentInfo = mainMeeting.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prison = bookingDetails.prisonName,
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
          )
        }
      }

      is BookingDetails.Amended -> {
        if (bookingDetails.isRescheduled()) {
          probationContacts.primaryProbationContact()?.let { primaryProbationContact ->
            RescheduledProbationBookingPrisonProbationEmail(
              address = prisonContact.email!!,
              prisonerNumber = bookingDetails.prisonerNumber,
              probationTeam = bookingDetails.probationTeamDescription!!,
              appointmentDate = mainMeeting.appointmentDate,
              appointmentInfo = mainMeeting.appointmentInformation(),
              comments = bookingDetails.notesForStaff,
              prisonerFirstName = bookingDetails.prisonerFirstName,
              prisonerLastName = bookingDetails.prisonerLastName,
              prison = bookingDetails.prisonName,
              probationEmailAddress = primaryProbationContact.email!!,
              prisonVideoUrl = mainMeeting.prisonVideoUrl,
              probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
              probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
              probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
              oldAppointmentDate = bookingDetails.oldMainMeeting!!.appointmentDate,
              oldAppointmentInfo = bookingDetails.oldMainMeeting.appointmentInformation(),
            )
          } ?: RescheduledProbationBookingPrisonNoProbationEmail(
            address = prisonContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            probationTeam = bookingDetails.probationTeamDescription!!,
            appointmentDate = mainMeeting.appointmentDate,
            appointmentInfo = mainMeeting.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prison = bookingDetails.prisonName,
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
            oldAppointmentDate = bookingDetails.oldMainMeeting!!.appointmentDate,
            oldAppointmentInfo = bookingDetails.oldMainMeeting.appointmentInformation(),
          )
        } else {
          if (primaryProbationContact != null) {
            AmendedProbationBookingPrisonProbationEmail(
              address = prisonContact.email!!,
              prisonerNumber = bookingDetails.prisonerNumber,
              probationTeam = bookingDetails.probationTeamDescription!!,
              appointmentDate = mainMeeting.appointmentDate,
              appointmentInfo = mainMeeting.appointmentInformation(),
              comments = bookingDetails.notesForStaff,
              prisonerFirstName = bookingDetails.prisonerFirstName,
              prisonerLastName = bookingDetails.prisonerLastName,
              prison = bookingDetails.prisonName,
              probationEmailAddress = primaryProbationContact.email!!,
              prisonVideoUrl = mainMeeting.prisonVideoUrl,
              probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
              probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
              probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
            )
          } else {
            AmendedProbationBookingPrisonNoProbationEmail(
              address = prisonContact.email!!,
              prisonerNumber = bookingDetails.prisonerNumber,
              probationTeam = bookingDetails.probationTeamDescription!!,
              appointmentDate = mainMeeting.appointmentDate,
              appointmentInfo = mainMeeting.appointmentInformation(),
              comments = bookingDetails.notesForStaff,
              prisonerFirstName = bookingDetails.prisonerFirstName,
              prisonerLastName = bookingDetails.prisonerLastName,
              prison = bookingDetails.prisonName,
              prisonVideoUrl = mainMeeting.prisonVideoUrl,
              probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
              probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
              probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
            )
          }
        }
      }

      is BookingDetails.Cancelled -> {
        if (primaryProbationContact != null) {
          CancelledProbationBookingPrisonProbationEmail(
            address = prisonContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            probationTeam = bookingDetails.probationTeamDescription!!,
            appointmentDate = mainMeeting.appointmentDate,
            appointmentInfo = mainMeeting.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prison = bookingDetails.prisonName,
            probationEmailAddress = primaryProbationContact.email!!,
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
          )
        } else {
          CancelledProbationBookingPrisonNoProbationEmail(
            address = prisonContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            probationTeam = bookingDetails.probationTeamDescription!!,
            appointmentDate = mainMeeting.appointmentDate,
            appointmentInfo = mainMeeting.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prison = bookingDetails.prisonName,
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
          )
        }
      }

      is BookingDetails.Released -> {
        if (primaryProbationContact != null) {
          ReleasedProbationBookingPrisonProbationEmail(
            address = prisonContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            dateOfBirth = bookingDetails.prisonerDateOfBirth,
            appointmentDate = mainMeeting.appointmentDate,
            probationTeam = bookingDetails.probationTeamDescription!!,
            probationEmailAddress = primaryProbationContact.email!!,
            prison = bookingDetails.prisonName,
            appointmentInfo = mainMeeting.appointmentInformation(),
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
            comments = bookingDetails.notesForStaff,
          )
        } else {
          ReleasedProbationBookingPrisonNoProbationEmail(
            address = prisonContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            dateOfBirth = bookingDetails.prisonerDateOfBirth,
            appointmentDate = mainMeeting.appointmentDate,
            probationTeam = bookingDetails.probationTeamDescription!!,
            prison = bookingDetails.prisonName,
            appointmentInfo = mainMeeting.appointmentInformation(),
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
            comments = bookingDetails.notesForStaff,
          )
        }
      }

      is BookingDetails.Transferred -> {
        if (primaryProbationContact != null) {
          TransferredProbationBookingPrisonProbationEmail(
            address = prisonContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            dateOfBirth = bookingDetails.prisonerDateOfBirth,
            appointmentDate = mainMeeting.appointmentDate,
            probationTeam = bookingDetails.probationTeamDescription!!,
            probationEmailAddress = primaryProbationContact.email!!,
            prison = bookingDetails.prisonName,
            appointmentInfo = mainMeeting.appointmentInformation(),
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
            comments = bookingDetails.notesForStaff,
          )
        } else {
          TransferredProbationBookingPrisonNoProbationEmail(
            address = prisonContact.email!!,
            prisonerNumber = bookingDetails.prisonerNumber,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            dateOfBirth = bookingDetails.prisonerDateOfBirth,
            appointmentDate = mainMeeting.appointmentDate,
            probationTeam = bookingDetails.probationTeamDescription!!,
            prison = bookingDetails.prisonName,
            appointmentInfo = mainMeeting.appointmentInformation(),
            prisonVideoUrl = mainMeeting.prisonVideoUrl,
            probationOfficerName = bookingDetails.additionalBookingDetails?.contactName,
            probationOfficerEmailAddress = bookingDetails.additionalBookingDetails?.contactEmail,
            probationOfficerContactNumber = bookingDetails.additionalBookingDetails?.contactNumber,
            comments = bookingDetails.notesForStaff,
          )
        }
      }

      else -> null
    }
  }

  private fun PrisonAppointment.appointmentInformation() = "$dpsLocationDescription - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"

  private fun Collection<BookingContact>.primaryProbationContact() = singleOrNull { it.contactType == ContactType.PROBATION && it.primaryContact }
}
