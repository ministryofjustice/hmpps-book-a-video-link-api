package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.requireNot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewProbationBookingUserEmail

object ProbationEmailFactory {
  fun user(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    appointment: PrisonAppointment,
    location: Location,
    action: BookingAction,
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
        date = appointment.appointmentDate,
        appointmentInfo = appointment.appointmentInformation(location),
        comments = booking.comments,
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prison = prison.name,
      )
      BookingAction.AMEND -> null
      BookingAction.CANCEL -> null
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
      else -> null
    }
  }

  private fun VideoBooking.requireIsProbationBooking() {
    requireNot(isCourtBooking()) { "Booking ID $videoBookingId is not a probation booking" }
  }

  private fun PrisonAppointment.appointmentInformation(location: Location) =
    "${location.localName} - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"

  private fun Collection<BookingContact>.primaryProbationContact() = singleOrNull { it.contactType == ContactType.PROBATION && it.primaryContact }
}
