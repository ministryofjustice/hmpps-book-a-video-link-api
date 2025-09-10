package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * This service is used to identify and prevent unnecessary entity updates if there are no actual changes.
 *
 * It must be used before any changes are made to the existing entities.
 */
@Service
class ChangeTrackingService(
  private val videoBookingRepository: VideoBookingRepository,
  private val locationsService: LocationsService,
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository,
) {
  @Transactional
  fun hasBookingChanged(videoBookingId: Long, request: AmendVideoBookingRequest, amendedBy: User): Boolean {
    val existingBooking = videoBookingRepository
      .findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found.") }
      .also {
        require(request.bookingType!!.name == it.bookingType.name) {
          "Request type and existing booking type must be the same. Request type is ${request.bookingType} and booking type is ${it.bookingType}."
        }
      }
      .toComparableBooking(additionalBookingDetailRepository, amendedBy)

    val suggestedBookingChanges = request.toComparableBooking(amendedBy, locationsService)

    return existingBooking != suggestedBookingChanges
  }
}

private fun AmendVideoBookingRequest.toComparableBooking(
  user: User,
  locationsService: LocationsService,
): ComparableBooking {
  if (this.probationMeetingType != null) {
    val appointment = this.prisoners.single().appointments.single()

    return ProbationBooking(
      meetingType = this.probationMeetingType.name,
      location = locationsService.getLocationByKey(appointment.locationKey!!)?.dpsLocationId!!,
      date = appointment.date!!,
      startTime = appointment.startTime,
      endTime = appointment.endTime,
      notesForStaff = notesForStaff,
      notesForPrisoners = notesForPrisoners.takeIf { user is PrisonUser },
      contactName = this.additionalBookingDetails?.contactName,
      contactEmail = this.additionalBookingDetails?.contactEmail,
      contactPhoneNumber = this.additionalBookingDetails?.contactNumber,
    )
  }

  val pre = this.prisoners.first().appointments.singleOrNull { it.type == AppointmentType.VLB_COURT_PRE }
  val main = this.prisoners.first().appointments.single { it.type == AppointmentType.VLB_COURT_MAIN }
  val post = this.prisoners.first().appointments.singleOrNull { it.type == AppointmentType.VLB_COURT_POST }

  return CourtBooking(
    hearingType = this.courtHearingType!!.name,
    date = main.date!!,
    preStartTime = pre?.startTime,
    preEndTime = pre?.endTime,
    preLocation = pre?.locationKey?.let { locationsService.getLocationByKey(it)?.dpsLocationId },
    mainStartTime = main.startTime!!,
    mainEndTime = main.endTime!!,
    mainLocation = main.locationKey?.let { locationsService.getLocationByKey(it)?.dpsLocationId },
    postStartTime = post?.startTime,
    postEndTime = post?.endTime,
    postLocation = post?.locationKey?.let { locationsService.getLocationByKey(it)?.dpsLocationId },
    cvpLink = videoLinkUrl,
    hmctsNumber = hmctsNumber,
    guestPin = guestPin,
    notesForStaff = notesForStaff,
    notesForPrisoners = notesForPrisoners.takeIf { user is PrisonUser },
  )
}

private fun VideoBooking.toComparableBooking(
  additionalBookingDetailRepository: AdditionalBookingDetailRepository,
  user: User,
): ComparableBooking {
  if (this.probationMeetingType != null) {
    val meeting = this.probationMeeting()!!
    val additionalBookingDetail = additionalBookingDetailRepository.findByVideoBooking(this)

    return ProbationBooking(
      meetingType = this.probationMeetingType!!,
      location = meeting.prisonLocationId,
      date = meeting.appointmentDate,
      startTime = meeting.startTime,
      endTime = meeting.endTime,
      notesForStaff = notesForStaff,
      notesForPrisoners = notesForPrisoners.takeIf { user is PrisonUser },
      contactName = additionalBookingDetail?.contactName,
      contactEmail = additionalBookingDetail?.contactEmail,
      contactPhoneNumber = additionalBookingDetail?.contactNumber,
    )
  }

  val pre = this.preHearing()
  val main = this.mainHearing()!!
  val post = this.postHearing()

  return CourtBooking(
    hearingType = hearingType!!,
    date = main.appointmentDate,
    preStartTime = pre?.startTime,
    preEndTime = pre?.endTime,
    preLocation = pre?.prisonLocationId,
    mainStartTime = main.startTime,
    mainEndTime = main.endTime,
    mainLocation = main.prisonLocationId,
    postStartTime = post?.startTime,
    postEndTime = post?.endTime,
    postLocation = post?.prisonLocationId,
    cvpLink = videoUrl,
    hmctsNumber = hmctsNumber,
    guestPin = guestPin,
    notesForStaff = notesForStaff,
    notesForPrisoners = notesForPrisoners.takeIf { user is PrisonUser },
  )
}

interface ComparableBooking

data class CourtBooking(
  val hearingType: String,
  val date: LocalDate,
  val preStartTime: LocalTime?,
  val preEndTime: LocalTime?,
  val preLocation: UUID?,
  val mainStartTime: LocalTime,
  val mainEndTime: LocalTime,
  val mainLocation: UUID?,
  val postStartTime: LocalTime?,
  val postEndTime: LocalTime?,
  val postLocation: UUID?,
  val cvpLink: String?,
  val hmctsNumber: String?,
  val guestPin: String?,
  val notesForStaff: String?,
  val notesForPrisoners: String?,
) : ComparableBooking

data class ProbationBooking(
  val meetingType: String,
  val location: UUID,
  val date: LocalDate,
  val startTime: LocalTime?,
  val endTime: LocalTime?,
  val notesForStaff: String?,
  val notesForPrisoners: String?,
  val contactName: String?,
  val contactEmail: String?,
  val contactPhoneNumber: String?,
) : ComparableBooking
