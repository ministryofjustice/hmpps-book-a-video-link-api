package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository

@Service
class BookingHistoryService(private val bookingHistoryRepository: BookingHistoryRepository) {

  fun getByVideoBookingId(videoBookingId: Long): List<BookingHistory> = bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(videoBookingId)

  @Transactional
  fun createBookingHistory(historyType: HistoryType, booking: VideoBooking) {
    when (historyType) {
      HistoryType.CREATE -> require(booking.isStatus(StatusCode.ACTIVE) && booking.amendedBy == null) {
        "Booking ${booking.videoBookingId} must be new for $historyType booking history"
      }
      HistoryType.AMEND -> require(booking.isStatus(StatusCode.ACTIVE) && booking.amendedBy != null) {
        "Booking ${booking.videoBookingId} must be amended for $historyType booking history"
      }
      HistoryType.CANCEL -> require(booking.isStatus(StatusCode.CANCELLED)) {
        "Booking ${booking.videoBookingId} must be cancelled for $historyType booking history"
      }
    }

    BookingHistory(
      videoBookingId = booking.videoBookingId,
      historyType = historyType,
      courtId = booking.court?.courtId.takeIf { booking.isBookingType(COURT) },
      hearingType = booking.hearingType.takeIf { booking.isBookingType(COURT) },
      probationTeamId = booking.probationTeam?.probationTeamId.takeIf { booking.isBookingType(PROBATION) },
      probationMeetingType = booking.probationMeetingType.takeIf { booking.isBookingType(PROBATION) },
      videoUrl = booking.videoUrl,
      comments = booking.comments,
      createdBy = booking.amendedBy ?: booking.createdBy,
      createdTime = booking.amendedTime ?: booking.createdTime,
    ).apply {
      addBookingHistoryAppointments(getAppointmentsForHistory(this, booking))
    }.also(bookingHistoryRepository::saveAndFlush)
  }

  private fun getAppointmentsForHistory(history: BookingHistory, booking: VideoBooking) = booking.appointments().map { appointment ->
    BookingHistoryAppointment(
      bookingHistory = history,
      prisonCode = appointment.prisonCode(),
      prisonerNumber = appointment.prisonerNumber,
      appointmentDate = appointment.appointmentDate,
      appointmentType = appointment.appointmentType,
      prisonLocationId = appointment.prisonLocationId,
      startTime = appointment.startTime,
      endTime = appointment.endTime,
    )
  }
}
