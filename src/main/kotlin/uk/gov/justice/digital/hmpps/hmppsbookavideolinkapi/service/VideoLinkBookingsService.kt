package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoBookingSearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
@Transactional(readOnly = true)
class VideoLinkBookingsService(
  private val videoBookingRepository: VideoBookingRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
  private val videoAppointmentRepository: VideoAppointmentRepository,
) {
  fun getVideoLinkBookingById(videoBookingId: Long): VideoLinkBooking {
    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }

    // Get the optional court hearing type reference data (if a court booking)
    val hearingType = when (booking.bookingType) {
      BookingType.COURT.name -> {
        referenceCodeRepository.findByGroupCodeAndCode("COURT_HEARING_TYPE", booking?.hearingType!!)
      }

      else -> null
    }

    // Get the optional probation meeting type reference data (if a probation booking)
    val meetingType = when (booking.bookingType) {
      BookingType.PROBATION.name -> {
        referenceCodeRepository.findByGroupCodeAndCode("PROBATION_MEETING_TYPE", booking?.probationMeetingType!!)
      }

      else -> null
    }

    return booking.toModel(
      prisonAppointments = booking.appointments(),
      courtDescription = booking.court?.description,
      probationTeamDescription = booking.probationTeam?.description,
      courtHearingTypeDescription = hearingType?.description,
      probationMeetingTypeDescription = meetingType?.description,
    )
  }

  fun findMatchingVideoLinkBooking(searchRequest: VideoBookingSearchRequest): VideoLinkBooking =
    videoAppointmentRepository.findByPrisonerNumberAndAppointmentDateAndPrisonLocKeyAndStartTimeAndEndTime(
      prisonerNumber = searchRequest.prisonerNumber!!,
      appointmentDate = searchRequest.date!!,
      prisonLocKey = searchRequest.locationKey!!,
      startTime = searchRequest.startTime!!,
      endTime = searchRequest.endTime!!,
    )
      ?.let { getVideoLinkBookingById(it.videoBookingId) }
      ?: throw EntityNotFoundException("Video booking not found matching search criteria $searchRequest")
}
