package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.PrisonAppointment as ModelPrisonAppointment

@Component
class RescheduledCourtEmailFactory(private val locationService: LocationsService) {
  fun user(
    oldBooking: VideoLinkBooking,
    amendedBooking: VideoBooking,
    prison: Prison,
    prisoner: Prisoner,
    userContact: BookingContact,
  ): VideoBookingEmail? {
    require(userContact.contactType == ContactType.USER) {
      "Incorrect contact type ${userContact.contactType} for court user email"
    }

    val (pre, main, post) = Triple(amendedBooking.preHearing(), amendedBooking.mainHearing()!!, amendedBooking.postHearing())
    val (oldPre, oldMain, oldPost) = Triple(oldBooking.preHearing(), oldBooking.mainHearing(), oldBooking.postHearing())

    return RescheduledCourtBookingUserEmail(
      address = userContact.email!!,
      userName = userContact.name ?: "Book Video",
      prisonerFirstName = prisoner.firstName,
      prisonerLastName = prisoner.lastName,
      prisonerNumber = prisoner.prisonerNumber,
      court = amendedBooking.court!!.description,
      prison = prison.name,
      appointmentDate = main.appointmentDate,
      preAppointmentDetails = pre?.appointmentDetails(locationService.getLocationById(pre.prisonLocationId)!!),
      mainAppointmentDetails = main.appointmentDetails(locationService.getLocationById(main.prisonLocationId)!!),
      postAppointmentDetails = post?.appointmentDetails(locationService.getLocationById(post.prisonLocationId)!!),
      comments = amendedBooking.notesForStaff,
      courtHearingLink = amendedBooking.fullCvpVideoUrl(),
      oldAppointmentDate = oldMain.appointmentDate,
      oldPreAppointmentDetails = oldPre?.appointmentDetails(locationService.getLocationByKey(oldPre.prisonLocKey)!!),
      oldMainAppointmentDetails = oldMain.appointmentDetails(locationService.getLocationByKey(oldMain.prisonLocKey)!!),
      oldPostAppointmentDetails = oldPost?.appointmentDetails(locationService.getLocationByKey(oldPost.prisonLocKey)!!),
    )
  }

  fun court(
    oldBooking: VideoLinkBooking,
    amendedBooking: VideoBooking,
    prison: Prison,
    prisoner: Prisoner,
    courtContact: BookingContact,
  ): VideoBookingEmail? {
    require(courtContact.contactType == ContactType.COURT) {
      "Incorrect contact type ${courtContact.contactType} for court court email"
    }

    val (pre, main, post) = Triple(amendedBooking.preHearing(), amendedBooking.mainHearing()!!, amendedBooking.postHearing())
    val (oldPre, oldMain, oldPost) = Triple(oldBooking.preHearing(), oldBooking.mainHearing(), oldBooking.postHearing())

    return RescheduledCourtBookingCourtEmail(
      address = courtContact.email!!,
      prisonerFirstName = prisoner.firstName,
      prisonerLastName = prisoner.lastName,
      prisonerNumber = prisoner.prisonerNumber,
      appointmentDate = main.appointmentDate,
      court = amendedBooking.court!!.description,
      prison = prison.name,
      preAppointmentDetails = pre?.appointmentDetails(locationService.getLocationById(pre.prisonLocationId)!!),
      mainAppointmentDetails = main.appointmentDetails(locationService.getLocationById(main.prisonLocationId)!!),
      postAppointmentDetails = post?.appointmentDetails(locationService.getLocationById(post.prisonLocationId)!!),
      comments = amendedBooking.notesForStaff,
      courtHearingLink = amendedBooking.fullCvpVideoUrl(),
      oldAppointmentDate = oldMain.appointmentDate,
      oldPreAppointmentDetails = oldPre?.appointmentDetails(locationService.getLocationByKey(oldPre.prisonLocKey)!!),
      oldMainAppointmentDetails = oldMain.appointmentDetails(locationService.getLocationByKey(oldMain.prisonLocKey)!!),
      oldPostAppointmentDetails = oldPost?.appointmentDetails(locationService.getLocationByKey(oldPost.prisonLocKey)!!),
    )
  }

  fun prison(
    oldBooking: VideoLinkBooking,
    amendedBooking: VideoBooking,
    prison: Prison,
    prisoner: Prisoner,
    prisonContact: BookingContact,
    contacts: Collection<BookingContact>,
  ): VideoBookingEmail? {
    require(prisonContact.contactType == ContactType.PRISON) {
      "Incorrect contact type ${prisonContact.contactType} for court prison email"
    }

    val (pre, main, post) = Triple(amendedBooking.preHearing(), amendedBooking.mainHearing()!!, amendedBooking.postHearing())
    val (oldPre, oldMain, oldPost) = Triple(oldBooking.preHearing(), oldBooking.mainHearing(), oldBooking.postHearing())

    return contacts.primaryCourtContact()?.let { primaryCourtContact ->
      RescheduledCourtBookingPrisonCourtEmail(
        address = prisonContact.email!!,
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prisonerNumber = prisoner.prisonerNumber,
        court = amendedBooking.court!!.description,
        courtEmailAddress = primaryCourtContact.email!!,
        prison = prison.name,
        appointmentDate = main.appointmentDate,
        preAppointmentDetails = pre?.appointmentDetails(locationService.getLocationById(pre.prisonLocationId)!!),
        mainAppointmentDetails = main.appointmentDetails(locationService.getLocationById(main.prisonLocationId)!!),
        postAppointmentDetails = post?.appointmentDetails(locationService.getLocationById(post.prisonLocationId)!!),
        comments = amendedBooking.notesForStaff,
        courtHearingLink = amendedBooking.fullCvpVideoUrl(),
        oldAppointmentDate = oldMain.appointmentDate,
        oldPreAppointmentDetails = oldPre?.appointmentDetails(locationService.getLocationByKey(oldPre.prisonLocKey)!!),
        oldMainAppointmentDetails = oldMain.appointmentDetails(locationService.getLocationByKey(oldMain.prisonLocKey)!!),
        oldPostAppointmentDetails = oldPost?.appointmentDetails(locationService.getLocationByKey(oldPost.prisonLocKey)!!),
      )
    } ?: RescheduledCourtBookingPrisonNoCourtEmail(
      address = prisonContact.email!!,
      prisonerFirstName = prisoner.firstName,
      prisonerLastName = prisoner.lastName,
      prisonerNumber = prisoner.prisonerNumber,
      court = amendedBooking.court!!.description,
      prison = prison.name,
      appointmentDate = main.appointmentDate,
      preAppointmentDetails = pre?.appointmentDetails(locationService.getLocationById(pre.prisonLocationId)!!),
      mainAppointmentDetails = main.appointmentDetails(locationService.getLocationById(main.prisonLocationId)!!),
      postAppointmentDetails = post?.appointmentDetails(locationService.getLocationById(post.prisonLocationId)!!),
      comments = amendedBooking.notesForStaff,
      courtHearingLink = amendedBooking.fullCvpVideoUrl(),
      oldAppointmentDate = oldMain.appointmentDate,
      oldPreAppointmentDetails = oldPre?.appointmentDetails(locationService.getLocationByKey(oldPre.prisonLocKey)!!),
      oldMainAppointmentDetails = oldMain.appointmentDetails(locationService.getLocationByKey(oldMain.prisonLocKey)!!),
      oldPostAppointmentDetails = oldPost?.appointmentDetails(locationService.getLocationByKey(oldPost.prisonLocKey)!!),
    )
  }

  private fun PrisonAppointment.appointmentDetails(location: Location) = AppointmentDetails(location.description ?: "", startTime, endTime, location.prisonVideoUrl(this))

  private fun Location.prisonVideoUrl(pa: PrisonAppointment) = this.extraAttributes?.prisonVideoUrl

  private fun Collection<BookingContact>.primaryCourtContact() = singleOrNull { it.contactType == ContactType.COURT && it.primaryContact }

  private fun VideoBooking.requireIsCourtBooking() {
    require(isBookingType(COURT)) { "Booking ID $videoBookingId is not a court booking" }
  }

  private fun VideoBooking.fullCvpVideoUrl() = hmctsNumber?.let { "${DEFAULT_COURT_URL_PREFIX}HMCTS$it@$DEFAULT_COURT_URL_SUFFIX" } ?: videoUrl

  private fun VideoLinkBooking.preHearing() = prisonAppointments.singleOrNull { it.appointmentType == "VLB_COURT_PRE" }

  private fun VideoLinkBooking.mainHearing() = prisonAppointments.single { it.appointmentType == "VLB_COURT_MAIN" }

  private fun VideoLinkBooking.postHearing() = prisonAppointments.singleOrNull { it.appointmentType == "VLB_COURT_POST" }

  private fun ModelPrisonAppointment.appointmentDetails(location: Location) = AppointmentDetails(location.description ?: "", startTime, endTime, null)
}
