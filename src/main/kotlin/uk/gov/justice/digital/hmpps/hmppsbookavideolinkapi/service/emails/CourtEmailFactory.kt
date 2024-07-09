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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingOwnerEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingOwnerEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingOwnerEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonNoCourtEmail

object CourtEmailFactory {

  fun owner(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    main: PrisonAppointment,
    pre: PrisonAppointment?,
    post: PrisonAppointment?,
    locations: Map<String, Location>,
    action: BookingAction,
  ): Email {
    return when (action) {
      BookingAction.CREATE -> NewCourtBookingOwnerEmail(
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
      BookingAction.AMEND -> AmendedCourtBookingOwnerEmail(
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
      BookingAction.CANCEL -> CancelledCourtBookingOwnerEmail(
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

      BookingAction.RELEASED -> TODO()
      BookingAction.TRANSFERRED -> TODO()
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

      BookingAction.RELEASED -> TODO()
      BookingAction.TRANSFERRED -> TODO()
    }
  }

  private fun PrisonAppointment.appointmentInformation(locations: Map<String, Location>) =
    "${locations.room(prisonLocKey)} - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"

  private fun Map<String, Location>.room(key: String) = this[key]?.localName ?: ""

  private fun Collection<BookingContact>.primaryCourtContact() = singleOrNull { it.contactType == ContactType.COURT && it.primaryContact }
}
