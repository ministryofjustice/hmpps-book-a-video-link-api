package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository

@Service
class BookingHistoryService(private val bookingHistoryRepository: BookingHistoryRepository) {

  // TODO: For an endpoint to retrieve the full history for a booking - coming soon
  fun getHistoryByVideoBookingId(videoBookingId: Long): List<BookingHistory> =
    bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(videoBookingId)

  // TODO: For use in retrieving a specific history row - coming soon
  fun getHistoryByBookingHistoryId(bookingHistoryId: Long): BookingHistory =
    bookingHistoryRepository.findById(bookingHistoryId)
      .orElseThrow { EntityNotFoundException("Video booking history with ID $bookingHistoryId not found") }

  @Transactional
  fun createBookingHistory(historyType: HistoryType, booking: VideoBooking) =
    BookingHistory(
      videoBookingId = booking.videoBookingId,
      historyType = historyType,
      courtId = booking.court?.courtId.takeIf { booking.isCourtBooking() },
      hearingType = booking.hearingType.takeIf { booking.isCourtBooking() },
      probationTeamId = booking.probationTeam?.probationTeamId.takeUnless { booking.isCourtBooking() },
      probationMeetingType = booking.probationMeetingType.takeUnless { booking.isCourtBooking() },
      videoUrl = booking.videoUrl,
      comments = booking.comments,
      createdBy = booking.createdBy,
    ).apply {
      addBookingHistoryAppointments(getAppointmentsForHistory(this, booking))
    }.apply {
      bookingHistoryRepository.saveAndFlush(this)
    }

  private fun getAppointmentsForHistory(history: BookingHistory, booking: VideoBooking) =
    booking.appointments().map { appointment ->
      BookingHistoryAppointment(
        bookingHistory = history,
        prisonCode = appointment.prisonCode,
        prisonerNumber = appointment.prisonerNumber,
        appointmentDate = appointment.appointmentDate,
        appointmentType = appointment.appointmentType,
        prisonLocKey = appointment.prisonLocKey,
        startTime = appointment.startTime,
        endTime = appointment.endTime,
      )
    }
}
