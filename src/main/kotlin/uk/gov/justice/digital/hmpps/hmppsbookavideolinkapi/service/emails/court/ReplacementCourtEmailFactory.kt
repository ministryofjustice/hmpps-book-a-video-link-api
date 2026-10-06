package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.BookingDetails

object ReplacementCourtEmailFactory {
  fun user(bookingDetails: BookingDetails, userContact: BookingContact): VideoBookingEmail? {
    require(bookingDetails.bookingType == BookingType.COURT) {
      "Incorrect booking type ${bookingDetails.bookingType} for court user email"
    }

    require(userContact.contactType == ContactType.USER) {
      "Incorrect contact type ${userContact.contactType} for court user email"
    }

    val (pre, main, post) = Triple(bookingDetails.preHearing, bookingDetails.mainHearing!!, bookingDetails.postHearing)

    return when (bookingDetails) {
      is BookingDetails.Create -> {
        NewCourtBookingUserEmail(
          address = userContact.email!!,
          userName = userContact.name ?: "Book Video",
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          prisonerNumber = bookingDetails.prisonerNumber,
          court = bookingDetails.courtDescription!!,
          prison = bookingDetails.prisonName,
          appointmentDate = main.appointmentDate,
          preAppointmentDetails = pre?.appointmentDetails(),
          mainAppointmentDetails = main.appointmentDetails(),
          postAppointmentDetails = post?.appointmentDetails(),
          comments = bookingDetails.notesForStaff,
          courtHearingLink = bookingDetails.fullCvpVideoUrl(),
        )
      }

      is BookingDetails.Amended -> {
        if (bookingDetails.isRescheduled()) {
          RescheduledCourtBookingUserEmail(
            address = userContact.email!!,
            userName = userContact.name ?: "Book Video",
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prisonerNumber = bookingDetails.prisonerNumber,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            appointmentDate = main.appointmentDate,
            preAppointmentDetails = pre?.appointmentDetails(),
            mainAppointmentDetails = main.appointmentDetails(),
            postAppointmentDetails = post?.appointmentDetails(),
            comments = bookingDetails.notesForStaff,
            courtHearingLink = bookingDetails.fullCvpVideoUrl(),
            oldAppointmentDate = bookingDetails.oldMainHearing!!.appointmentDate,
            oldPreAppointmentDetails = bookingDetails.oldPreHearing?.appointmentDetails(),
            oldMainAppointmentDetails = bookingDetails.oldMainHearing.appointmentDetails(),
            oldPostAppointmentDetails = bookingDetails.oldPostHearing?.appointmentDetails(),
          )
        } else {
          AmendedCourtBookingUserEmail(
            address = userContact.email!!,
            userName = userContact.name ?: "Book Video",
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prisonerNumber = bookingDetails.prisonerNumber,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            appointmentDate = main.appointmentDate,
            preAppointmentDetails = pre?.appointmentDetails(),
            mainAppointmentDetails = main.appointmentDetails(),
            postAppointmentDetails = post?.appointmentDetails(),
            comments = bookingDetails.notesForStaff,
            courtHearingLink = bookingDetails.fullCvpVideoUrl(),
          )
        }
      }

      is BookingDetails.Cancelled -> {
        CancelledCourtBookingUserEmail(
          address = userContact.email!!,
          userName = userContact.name ?: "Book Video",
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          prisonerNumber = bookingDetails.prisonerNumber,
          court = bookingDetails.courtDescription!!,
          prison = bookingDetails.prisonName,
          appointmentDate = main.appointmentDate,
          preAppointmentInfo = pre?.appointmentInformation(),
          mainAppointmentInfo = main.appointmentInformation(),
          postAppointmentInfo = post?.appointmentInformation(),
          comments = bookingDetails.notesForStaff,
          courtHearingLink = bookingDetails.fullCvpVideoUrl(),
        )
      }

      else -> null
    }
  }

  fun court(bookingDetails: BookingDetails, courtContact: BookingContact): VideoBookingEmail? {
    require(bookingDetails.bookingType == BookingType.COURT) {
      "Incorrect booking type ${bookingDetails.bookingType} for court user email"
    }

    require(courtContact.contactType == ContactType.COURT) {
      "Incorrect contact type ${courtContact.contactType} for court court email"
    }

    val (pre, main, post) = Triple(bookingDetails.preHearing, bookingDetails.mainHearing!!, bookingDetails.postHearing)

    return when (bookingDetails) {
      is BookingDetails.Create -> {
        NewCourtBookingCourtEmail(
          address = courtContact.email!!,
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          prisonerNumber = bookingDetails.prisonerNumber,
          court = bookingDetails.courtDescription!!,
          prison = bookingDetails.prisonName,
          appointmentDate = main.appointmentDate,
          preAppointmentDetails = pre?.appointmentDetails(),
          mainAppointmentDetails = main.appointmentDetails(),
          postAppointmentDetails = post?.appointmentDetails(),
          comments = bookingDetails.notesForStaff,
          courtHearingLink = bookingDetails.fullCvpVideoUrl(),
        )
      }

      is BookingDetails.Amended -> {
        if (bookingDetails.isRescheduled()) {
          RescheduledCourtBookingCourtEmail(
            address = courtContact.email!!,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prisonerNumber = bookingDetails.prisonerNumber,
            appointmentDate = main.appointmentDate,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            preAppointmentDetails = pre?.appointmentDetails(),
            mainAppointmentDetails = main.appointmentDetails(),
            postAppointmentDetails = post?.appointmentDetails(),
            comments = bookingDetails.notesForStaff,
            courtHearingLink = bookingDetails.fullCvpVideoUrl(),
            oldAppointmentDate = bookingDetails.oldMainHearing!!.appointmentDate,
            oldPreAppointmentDetails = bookingDetails.oldPreHearing?.appointmentDetails(),
            oldMainAppointmentDetails = bookingDetails.oldMainHearing.appointmentDetails(),
            oldPostAppointmentDetails = bookingDetails.oldPostHearing?.appointmentDetails(),
          )
        } else {
          AmendedCourtBookingCourtEmail(
            address = courtContact.email!!,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prisonerNumber = bookingDetails.prisonerNumber,
            appointmentDate = main.appointmentDate,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            preAppointmentDetails = pre?.appointmentDetails(),
            mainAppointmentDetails = main.appointmentDetails(),
            postAppointmentDetails = post?.appointmentDetails(),
            comments = bookingDetails.notesForStaff,
            courtHearingLink = bookingDetails.fullCvpVideoUrl(),
          )
        }
      }

      is BookingDetails.Cancelled -> {
        CancelledCourtBookingCourtEmail(
          address = courtContact.email!!,
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          prisonerNumber = bookingDetails.prisonerNumber,
          appointmentDate = main.appointmentDate,
          court = bookingDetails.courtDescription!!,
          prison = bookingDetails.prisonName,
          preAppointmentInfo = pre?.appointmentInformation(),
          mainAppointmentInfo = main.appointmentInformation(),
          postAppointmentInfo = post?.appointmentInformation(),
          comments = bookingDetails.notesForStaff,
          courtHearingLink = bookingDetails.fullCvpVideoUrl(),
        )
      }

      is BookingDetails.Released -> {
        ReleasedCourtBookingCourtEmail(
          address = courtContact.email!!,
          court = bookingDetails.courtDescription!!,
          prison = bookingDetails.prisonName,
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          dateOfBirth = bookingDetails.prisonerDateOfBirth,
          prisonerNumber = bookingDetails.prisonerNumber,
          date = main.appointmentDate,
          preAppointmentInfo = pre?.appointmentInformation(),
          mainAppointmentInfo = main.appointmentInformation(),
          postAppointmentInfo = post?.appointmentInformation(),
          comments = bookingDetails.notesForStaff,
        )
      }

      is BookingDetails.Transferred -> {
        TransferredCourtBookingCourtEmail(
          address = courtContact.email!!,
          court = bookingDetails.courtDescription!!,
          prison = bookingDetails.prisonName,
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          dateOfBirth = bookingDetails.prisonerDateOfBirth,
          prisonerNumber = bookingDetails.prisonerNumber,
          date = main.appointmentDate,
          preAppointmentInfo = pre?.appointmentInformation(),
          mainAppointmentInfo = main.appointmentInformation(),
          postAppointmentInfo = post?.appointmentInformation(),
          comments = bookingDetails.notesForStaff,
        )
      }

      is BookingDetails.CourtHearingLinkReminder -> {
        CourtHearingLinkReminderEmail(
          address = courtContact.email!!,
          court = bookingDetails.courtDescription!!,
          prison = bookingDetails.prisonName,
          prisonerFirstName = bookingDetails.prisonerFirstName,
          prisonerLastName = bookingDetails.prisonerLastName,
          prisonerNumber = bookingDetails.prisonerNumber,
          date = main.appointmentDate,
          preAppointmentInfo = pre?.appointmentInformation(),
          mainAppointmentInfo = main.appointmentInformation(),
          postAppointmentInfo = post?.appointmentInformation(),
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
    contacts: Collection<BookingContact>,
  ): VideoBookingEmail? {
    require(bookingDetails.bookingType == BookingType.COURT) {
      "Incorrect booking type ${bookingDetails.bookingType} for court user email"
    }

    require(prisonContact.contactType == ContactType.PRISON) {
      "Incorrect contact type ${prisonContact.contactType} for court prison email"
    }

    val primaryCourtContact = contacts.primaryCourtContact()

    val (pre, main, post) = Triple(bookingDetails.preHearing, bookingDetails.mainHearing!!, bookingDetails.postHearing)

    return when (bookingDetails) {
      is BookingDetails.Create -> {
        if (primaryCourtContact != null) {
          NewCourtBookingPrisonCourtEmail(
            address = prisonContact.email!!,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prisonerNumber = bookingDetails.prisonerNumber,
            court = bookingDetails.courtDescription!!,
            courtEmailAddress = primaryCourtContact.email!!,
            prison = bookingDetails.prisonName,
            appointmentDate = main.appointmentDate,
            preAppointmentDetails = pre?.appointmentDetails(),
            mainAppointmentDetails = main.appointmentDetails(),
            postAppointmentDetails = post?.appointmentDetails(),
            comments = bookingDetails.notesForStaff,
            courtHearingLink = bookingDetails.fullCvpVideoUrl(),
          )
        } else {
          NewCourtBookingPrisonNoCourtEmail(
            address = prisonContact.email!!,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prisonerNumber = bookingDetails.prisonerNumber,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            appointmentDate = main.appointmentDate,
            preAppointmentDetails = pre?.appointmentDetails(),
            mainAppointmentDetails = main.appointmentDetails(),
            postAppointmentDetails = post?.appointmentDetails(),
            comments = bookingDetails.notesForStaff,
            courtHearingLink = bookingDetails.fullCvpVideoUrl(),
          )
        }
      }

      is BookingDetails.Amended -> {
        if (bookingDetails.isRescheduled()) {
          contacts.primaryCourtContact()?.let { primaryCourtContact ->
            RescheduledCourtBookingPrisonCourtEmail(
              address = prisonContact.email!!,
              prisonerFirstName = bookingDetails.prisonerFirstName,
              prisonerLastName = bookingDetails.prisonerLastName,
              prisonerNumber = bookingDetails.prisonerNumber,
              court = bookingDetails.courtDescription!!,
              prison = bookingDetails.prisonName,
              courtEmailAddress = primaryCourtContact.email!!,
              appointmentDate = main.appointmentDate,
              preAppointmentDetails = pre?.appointmentDetails(),
              mainAppointmentDetails = main.appointmentDetails(),
              postAppointmentDetails = post?.appointmentDetails(),
              comments = bookingDetails.notesForStaff,
              courtHearingLink = bookingDetails.fullCvpVideoUrl(),
              oldAppointmentDate = bookingDetails.oldMainHearing!!.appointmentDate,
              oldPreAppointmentDetails = bookingDetails.oldPreHearing?.appointmentDetails(),
              oldMainAppointmentDetails = bookingDetails.oldMainHearing.appointmentDetails(),
              oldPostAppointmentDetails = bookingDetails.oldPostHearing?.appointmentDetails(),
            )
          } ?: RescheduledCourtBookingPrisonNoCourtEmail(
            address = prisonContact.email!!,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prisonerNumber = bookingDetails.prisonerNumber,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            appointmentDate = main.appointmentDate,
            preAppointmentDetails = pre?.appointmentDetails(),
            mainAppointmentDetails = main.appointmentDetails(),
            postAppointmentDetails = post?.appointmentDetails(),
            comments = bookingDetails.notesForStaff,
            courtHearingLink = bookingDetails.fullCvpVideoUrl(),
            oldAppointmentDate = bookingDetails.oldMainHearing!!.appointmentDate,
            oldPreAppointmentDetails = bookingDetails.oldPreHearing?.appointmentDetails(),
            oldMainAppointmentDetails = bookingDetails.oldMainHearing.appointmentDetails(),
            oldPostAppointmentDetails = bookingDetails.oldPostHearing?.appointmentDetails(),
          )
        } else {
          if (primaryCourtContact != null) {
            AmendedCourtBookingPrisonCourtEmail(
              address = prisonContact.email!!,
              prisonerFirstName = bookingDetails.prisonerFirstName,
              prisonerLastName = bookingDetails.prisonerLastName,
              prisonerNumber = bookingDetails.prisonerNumber,
              court = bookingDetails.courtDescription!!,
              courtEmailAddress = primaryCourtContact.email!!,
              prison = bookingDetails.prisonName,
              appointmentDate = main.appointmentDate,
              preAppointmentDetails = pre?.appointmentDetails(),
              mainAppointmentDetails = main.appointmentDetails(),
              postAppointmentDetails = post?.appointmentDetails(),
              comments = bookingDetails.notesForStaff,
              courtHearingLink = bookingDetails.fullCvpVideoUrl(),
            )
          } else {
            AmendedCourtBookingPrisonNoCourtEmail(
              address = prisonContact.email!!,
              prisonerFirstName = bookingDetails.prisonerFirstName,
              prisonerLastName = bookingDetails.prisonerLastName,
              prisonerNumber = bookingDetails.prisonerNumber,
              court = bookingDetails.courtDescription!!,
              prison = bookingDetails.prisonName,
              appointmentDate = main.appointmentDate,
              preAppointmentDetails = pre?.appointmentDetails(),
              mainAppointmentDetails = main.appointmentDetails(),
              postAppointmentDetails = post?.appointmentDetails(),
              comments = bookingDetails.notesForStaff,
              courtHearingLink = bookingDetails.fullCvpVideoUrl(),
            )
          }
        }
      }

      is BookingDetails.Cancelled -> {
        if (primaryCourtContact != null) {
          CancelledCourtBookingPrisonCourtEmail(
            address = prisonContact.email!!,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prisonerNumber = bookingDetails.prisonerNumber,
            court = bookingDetails.courtDescription!!,
            courtEmailAddress = primaryCourtContact.email!!,
            prison = bookingDetails.prisonName,
            appointmentDate = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(),
            mainAppointmentInfo = main.appointmentInformation(),
            postAppointmentInfo = post?.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
            courtHearingLink = bookingDetails.fullCvpVideoUrl(),
          )
        } else {
          CancelledCourtBookingPrisonNoCourtEmail(
            address = prisonContact.email!!,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            prisonerNumber = bookingDetails.prisonerNumber,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            appointmentDate = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(),
            mainAppointmentInfo = main.appointmentInformation(),
            postAppointmentInfo = post?.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
            courtHearingLink = bookingDetails.fullCvpVideoUrl(),
          )
        }
      }

      is BookingDetails.Released -> {
        if (primaryCourtContact != null) {
          // Note: primary contact is only used to determine which template to use, it is not used in the template.
          ReleasedCourtBookingPrisonCourtEmail(
            address = prisonContact.email!!,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            dateOfBirth = bookingDetails.prisonerDateOfBirth,
            prisonerNumber = bookingDetails.prisonerNumber,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(),
            mainAppointmentInfo = main.appointmentInformation(),
            postAppointmentInfo = post?.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
          )
        } else {
          ReleasedCourtBookingPrisonNoCourtEmail(
            address = prisonContact.email!!,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            dateOfBirth = bookingDetails.prisonerDateOfBirth,
            prisonerNumber = bookingDetails.prisonerNumber,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(),
            mainAppointmentInfo = main.appointmentInformation(),
            postAppointmentInfo = post?.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
          )
        }
      }

      is BookingDetails.Transferred -> {
        // Note: primary contact is only used to determine which template to use, it is not used in the template.
        if (primaryCourtContact != null) {
          TransferredCourtBookingPrisonCourtEmail(
            address = prisonContact.email!!,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            dateOfBirth = bookingDetails.prisonerDateOfBirth,
            prisonerNumber = bookingDetails.prisonerNumber,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(),
            mainAppointmentInfo = main.appointmentInformation(),
            postAppointmentInfo = post?.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
          )
        } else {
          TransferredCourtBookingPrisonNoCourtEmail(
            address = prisonContact.email!!,
            court = bookingDetails.courtDescription!!,
            prison = bookingDetails.prisonName,
            prisonerFirstName = bookingDetails.prisonerFirstName,
            prisonerLastName = bookingDetails.prisonerLastName,
            dateOfBirth = bookingDetails.prisonerDateOfBirth,
            prisonerNumber = bookingDetails.prisonerNumber,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(),
            mainAppointmentInfo = main.appointmentInformation(),
            postAppointmentInfo = post?.appointmentInformation(),
            comments = bookingDetails.notesForStaff,
          )
        }
      }

      else -> null
    }
  }

  private fun PrisonAppointment.appointmentDetails() = AppointmentDetails(dpsLocationDescription, startTime, endTime, prisonVideoUrl)

  private fun PrisonAppointment.appointmentInformation() = "$dpsLocationDescription - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"

  private fun Collection<BookingContact>.primaryCourtContact() = singleOrNull { it.contactType == ContactType.COURT && it.primaryContact }

  private fun BookingDetails.fullCvpVideoUrl() = hmctsNumber?.let { "${DEFAULT_COURT_URL_PREFIX}HMCTS$it@$DEFAULT_COURT_URL_SUFFIX" } ?: videoLinkUrl
}
