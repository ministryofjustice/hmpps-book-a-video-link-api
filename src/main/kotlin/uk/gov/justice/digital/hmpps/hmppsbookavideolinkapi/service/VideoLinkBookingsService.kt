package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class VideoLinkBookingsService(
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
) {

  fun getVideoLinkBookingById(videoBookingId: Long): VideoLinkBooking {
    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }

    val prisonAppointments = prisonAppointmentRepository.findByVideoBooking(booking)

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
      prisonAppointments = prisonAppointments,
      courtDescription = booking.court?.description,
      probationTeamDescription = booking.probationTeam?.description,
      courtHearingTypeDescription = hearingType?.description,
      probationMeetingTypeDescription = meetingType?.description,
    )
  }
}
