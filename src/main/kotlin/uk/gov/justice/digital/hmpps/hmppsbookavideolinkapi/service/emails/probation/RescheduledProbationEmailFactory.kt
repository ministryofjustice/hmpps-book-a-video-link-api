package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.PrisonAppointment as ModelPrisonAppointment

@Component
class RescheduledProbationEmailFactory(
  private val locationService: LocationsService,
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository,
) {
  fun user(
    oldBooking: VideoLinkBooking,
    amendedBooking: VideoBooking,
    contact: BookingContact,
    prisoner: Prisoner,
    prison: Prison,
  ): VideoBookingEmail {
    amendedBooking.requireIsProbationBooking()

    require(contact.contactType == ContactType.USER) {
      "Incorrect contact type ${contact.contactType} for probation user email"
    }

    val meeting = amendedBooking.probationMeeting()!!
    val meetingLocation = locationService.getLocationById(meeting.prisonLocationId)!!
    val additionalBookingDetail = additionalBookingDetailRepository.findByVideoBooking(amendedBooking)

    return RescheduledProbationBookingUserEmail(
      address = contact.email!!,
      prisonerNumber = prisoner.prisonerNumber,
      userName = contact.name ?: "Book Video",
      probationTeam = amendedBooking.probationTeam!!.description,
      appointmentDate = meeting.appointmentDate,
      appointmentInfo = meeting.appointmentInformation(meetingLocation),
      comments = amendedBooking.notesForStaff,
      prisonerFirstName = prisoner.firstName,
      prisonerLastName = prisoner.lastName,
      prison = prison.name,
      prisonVideoUrl = meetingLocation.extraAttributes?.prisonVideoUrl,
      probationOfficerName = additionalBookingDetail?.contactName,
      probationOfficerEmailAddress = additionalBookingDetail?.contactEmail,
      probationOfficerContactNumber = additionalBookingDetail?.contactNumber,
      oldAppointmentDate = oldBooking.prisonAppointments.single().appointmentDate,
      oldAppointmentInfo = oldBooking.prisonAppointments.single().let { it.appointmentInformation(locationService.getLocationByKey(it.prisonLocKey)!!) },
    )
  }

  fun probation(
    oldBooking: VideoLinkBooking,
    amendedBooking: VideoBooking,
    contact: BookingContact,
    prisoner: Prisoner,
    prison: Prison,
  ): VideoBookingEmail {
    amendedBooking.requireIsProbationBooking()

    require(contact.contactType == ContactType.PROBATION) {
      "Incorrect contact type ${contact.contactType} for probation probation email"
    }

    val meeting = amendedBooking.probationMeeting()!!
    val meetingLocation = locationService.getLocationById(meeting.prisonLocationId)!!
    val additionalBookingDetail = additionalBookingDetailRepository.findByVideoBooking(amendedBooking)

    return RescheduledProbationBookingProbationEmail(
      address = contact.email!!,
      prisonerNumber = prisoner.prisonerNumber,
      prisonerFirstName = prisoner.firstName,
      prisonerLastName = prisoner.lastName,
      appointmentDate = meeting.appointmentDate,
      probationTeam = amendedBooking.probationTeam!!.description,
      comments = amendedBooking.notesForStaff,
      appointmentInfo = meeting.appointmentInformation(meetingLocation),
      prison = prison.name,
      prisonVideoUrl = meetingLocation.extraAttributes?.prisonVideoUrl,
      probationOfficerName = additionalBookingDetail?.contactName,
      probationOfficerEmailAddress = additionalBookingDetail?.contactEmail,
      probationOfficerContactNumber = additionalBookingDetail?.contactNumber,
      oldAppointmentDate = oldBooking.prisonAppointments.single().appointmentDate,
      oldAppointmentInfo = oldBooking.prisonAppointments.single().let { it.appointmentInformation(locationService.getLocationByKey(it.prisonLocKey)!!) },
    )
  }

  fun prison(
    oldBooking: VideoLinkBooking,
    amendedBooking: VideoBooking,
    contact: BookingContact,
    prisoner: Prisoner,
    prison: Prison,
    contacts: Collection<BookingContact>,
  ): VideoBookingEmail {
    amendedBooking.requireIsProbationBooking()

    require(contact.contactType == ContactType.PRISON) {
      "Incorrect contact type ${contact.contactType} for prison probation email"
    }

    val meeting = amendedBooking.probationMeeting()!!
    val meetingLocation = locationService.getLocationById(meeting.prisonLocationId)!!
    val additionalBookingDetail = additionalBookingDetailRepository.findByVideoBooking(amendedBooking)

    return contacts.primaryProbationContact()?.let { primaryProbationContact ->
      RescheduledProbationBookingPrisonProbationEmail(
        address = contact.email!!,
        prisonerNumber = prisoner.prisonerNumber,
        probationTeam = amendedBooking.probationTeam!!.description,
        appointmentDate = meeting.appointmentDate,
        appointmentInfo = meeting.appointmentInformation(meetingLocation),
        comments = amendedBooking.notesForStaff,
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prison = prison.name,
        probationEmailAddress = primaryProbationContact.email!!,
        prisonVideoUrl = meetingLocation.extraAttributes?.prisonVideoUrl,
        probationOfficerName = additionalBookingDetail?.contactName,
        probationOfficerEmailAddress = additionalBookingDetail?.contactEmail,
        probationOfficerContactNumber = additionalBookingDetail?.contactNumber,
        oldAppointmentDate = oldBooking.prisonAppointments.single().appointmentDate,
        oldAppointmentInfo = oldBooking.prisonAppointments.single().let { it.appointmentInformation(locationService.getLocationByKey(it.prisonLocKey)!!) },
      )
    } ?: RescheduledProbationBookingPrisonNoProbationEmail(
      address = contact.email!!,
      prisonerNumber = prisoner.prisonerNumber,
      probationTeam = amendedBooking.probationTeam!!.description,
      appointmentDate = meeting.appointmentDate,
      appointmentInfo = meeting.appointmentInformation(meetingLocation),
      comments = amendedBooking.notesForStaff,
      prisonerFirstName = prisoner.firstName,
      prisonerLastName = prisoner.lastName,
      prison = prison.name,
      prisonVideoUrl = meetingLocation.extraAttributes?.prisonVideoUrl,
      probationOfficerName = additionalBookingDetail?.contactName,
      probationOfficerEmailAddress = additionalBookingDetail?.contactEmail,
      probationOfficerContactNumber = additionalBookingDetail?.contactNumber,
      oldAppointmentDate = oldBooking.prisonAppointments.single().appointmentDate,
      oldAppointmentInfo = oldBooking.prisonAppointments.single().let { it.appointmentInformation(locationService.getLocationByKey(it.prisonLocKey)!!) },
    )
  }

  private fun VideoBooking.requireIsProbationBooking() {
    require(isBookingType(PROBATION)) { "Booking ID $videoBookingId is not a probation booking" }
  }

  private fun PrisonAppointment.appointmentInformation(location: Location) = "${location.description} - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"

  private fun Collection<BookingContact>.primaryProbationContact() = singleOrNull { it.contactType == ContactType.PROBATION && it.primaryContact }

  private fun ModelPrisonAppointment.appointmentInformation(location: Location) = "${location.description} - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"
}
