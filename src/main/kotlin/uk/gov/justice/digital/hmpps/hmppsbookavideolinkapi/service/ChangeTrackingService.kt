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
@Transactional(readOnly = true)
class ChangeTrackingService(
  private val videoBookingRepository: VideoBookingRepository,
  private val locationsService: LocationsService,
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository,
) {
  /**
   * Will return the change type NONE if there are no actual changes.
   */
  fun determineChangeType(videoBookingId: Long, requestedBookingChanges: AmendVideoBookingRequest, amendedBy: User): ChangeType {
    require(amendedBy is PrisonUser || amendedBy is ExternalUser) {
      "Only prison users and external users are supported. ${amendedBy::class.simpleName} is not supported."
    }

    val (cb1, cb2) = getComparableBookings(videoBookingId, requestedBookingChanges, amendedBy)

    when {
      cb1 == cb2 -> return ChangeType.NONE
      // All changes by external users are currently considered global.
      amendedBy is ExternalUser -> return ChangeType.GLOBAL
    }

    // This is considered a change by a prison user at this point.
    return when (cb1) {
      is CourtBooking -> {
        val isGlobalChange = CourtAttribute.entries.filter { it.changeType == ChangeType.GLOBAL }.any { it.hasChanged(cb1, cb2 as CourtBooking) }

        // At this point we know it has changed, and if there are no global changes, then it must be prison-specific.
        if (isGlobalChange) ChangeType.GLOBAL else ChangeType.PRISON
      }
      is ProbationBooking -> {
        val isGlobalChange = ProbationAttribute.entries.filter { it.changeType == ChangeType.GLOBAL }.any { it.hasChanged(cb1, cb2 as ProbationBooking) }

        // At this point we know it has changed, and if there are no global changes, then it must be prison-specific.
        if (isGlobalChange) ChangeType.GLOBAL else ChangeType.PRISON
      }
    } ?: ChangeType.GLOBAL
  }

  private fun getComparableBookings(videoBookingId: Long, requestedBookingChanges: AmendVideoBookingRequest, amendedBy: User) = run {
    val existingBooking = videoBookingRepository
      .findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found.") }
      .also {
        require(requestedBookingChanges.bookingType!!.name == it.bookingType.name) {
          "Request type and existing booking type must be the same. Request type is ${requestedBookingChanges.bookingType} and booking type is ${it.bookingType}."
        }
      }
      .toComparableBooking(additionalBookingDetailRepository, amendedBy)

    existingBooking to requestedBookingChanges.toComparableBooking(amendedBy, locationsService)
  }
}

enum class ChangeType(val description: String) {
  GLOBAL("A change is considered global when something can be modified by any user."),
  NONE("No change has been detected between the existing and suggested values."),
  PRISON("A change is considered prison-specific when something can only modified by a prison user."),
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

sealed interface ComparableBooking

enum class CourtAttribute(val hasChanged: (CourtBooking, CourtBooking) -> Boolean, val changeType: ChangeType = ChangeType.GLOBAL) {
  HEARING_TYPE({ b1, b2 -> b1.hearingType != b2.hearingType }),
  DATE({ b1, b2 -> b1.date != b2.date }),
  PRE_START_TIME({ b1, b2 -> b1.preStartTime != b2.preStartTime }),
  PRE_END_TIME({ b1, b2 -> b1.preEndTime != b2.preEndTime }),
  PRE_LOCATION({ b1, b2 -> b1.preLocation != b2.preLocation }),
  MAIN_START_TIME({ b1, b2 -> b1.mainStartTime != b2.mainStartTime }),
  MAIN_END_TIME({ b1, b2 -> b1.mainEndTime != b2.mainEndTime }),
  MAIN_LOCATION({ b1, b2 -> b1.mainLocation != b2.mainLocation }),
  POST_START_TIME({ b1, b2 -> b1.postStartTime != b2.postStartTime }),
  POST_END_TIME({ b1, b2 -> b1.postEndTime != b2.postEndTime }),
  POST_LOCATION({ b1, b2 -> b1.postLocation != b2.postLocation }),
  CVP_LINK({ b1, b2 -> b1.cvpLink != b2.cvpLink }),
  HMCTS_NUMBER({ b1, b2 -> b1.hmctsNumber != b2.hmctsNumber }),
  GUEST_PIN({ b1, b2 -> b1.guestPin != b2.guestPin }),
  NOTES_FOR_STAFF({ b1, b2 -> b1.notesForStaff != b2.notesForStaff }),
  NOTES_FOR_PRISONERS({ b1, b2 -> b1.notesForPrisoners != b2.notesForPrisoners }, ChangeType.PRISON),
}

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

enum class ProbationAttribute(val hasChanged: (ProbationBooking, ProbationBooking) -> Boolean, val changeType: ChangeType = ChangeType.GLOBAL) {
  MEETING_TYPE({ b1, b2 -> b1.meetingType != b2.meetingType }),
  DATE({ b1, b2 -> b1.date != b2.date }),
  START_TIME({ b1, b2 -> b1.startTime != b2.startTime }),
  END_TIME({ b1, b2 -> b1.endTime != b2.endTime }),
  LOCATION({ b1, b2 -> b1.location != b2.location }),
  NOTES_FOR_STAFF({ b1, b2 -> b1.notesForStaff != b2.notesForStaff }),
  NOTES_FOR_PRISONERS({ b1, b2 -> b1.notesForPrisoners != b2.notesForPrisoners }, ChangeType.PRISON),
  CONTACT_NAME({ b1, b2 -> b1.contactName != b2.contactName }),
  CONTACT_EMAIL({ b1, b2 -> b1.contactEmail != b2.contactEmail }),
  CONTACT_PHONE_NUMBER({ b1, b2 -> b1.contactPhoneNumber != b2.contactPhoneNumber }),
}

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
