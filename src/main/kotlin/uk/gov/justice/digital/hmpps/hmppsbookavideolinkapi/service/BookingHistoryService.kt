package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository

@Service
class BookingHistoryService(private val bookingHistoryRepository: BookingHistoryRepository) {

  fun getByVideoBookingId(videoBookingId: Long): List<BookingHistory> =
    bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(videoBookingId)

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
    }.also(bookingHistoryRepository::saveAndFlush)

  private fun getAppointmentsForHistory(history: BookingHistory, booking: VideoBooking) =
    booking.appointments().map { appointment ->
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
