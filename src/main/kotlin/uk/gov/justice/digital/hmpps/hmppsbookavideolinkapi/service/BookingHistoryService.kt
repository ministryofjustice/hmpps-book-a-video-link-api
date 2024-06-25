package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository

@Service
class BookingHistoryService(private val bookingHistoryRepository: BookingHistoryRepository) {

  fun getHistoryByVideoBookingId(videoBookingId: Long): List<BookingHistory> =
    bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(videoBookingId)

  fun getHistoryByBookingHistoryId(bookingHistoryId: Long): BookingHistory =
    bookingHistoryRepository.findById(bookingHistoryId)
      .orElseThrow { EntityNotFoundException("Video booking history with ID $bookingHistoryId not found") }

  @Transactional
  fun createBookingHistoryForCourt(historyType: String, booking: VideoBooking, prisonerDetails: List<PrisonerDetails>) =
    BookingHistory(
      videoBookingId = booking.videoBookingId,
      historyType = historyType,
      courtId = booking.court?.courtId,
      hearingType = booking.hearingType,
      videoUrl = booking.videoUrl,
      comments = booking.comments,
      createdBy = booking.createdBy,
    ).also { history ->
      history.addBookingHistoryAppointments(getAppointmentsForHistory(history, prisonerDetails))
    }.also { history ->
      bookingHistoryRepository.saveAndFlush(history)
    }

  @Transactional
  fun createBookingHistoryForProbation(historyType: String, booking: VideoBooking, prisonerDetails: List<PrisonerDetails>) =
    BookingHistory(
      videoBookingId = booking.videoBookingId,
      historyType = historyType,
      probationTeamId = booking.probationTeam?.probationTeamId,
      probationMeetingType = booking.probationMeetingType,
      videoUrl = booking.videoUrl,
      comments = booking.comments,
      createdBy = booking.createdBy,
    ).also { history ->
      history.addBookingHistoryAppointments(getAppointmentsForHistory(history, prisonerDetails))
    }.also { history ->
      bookingHistoryRepository.saveAndFlush(history)
    }

  private fun getAppointmentsForHistory(
    history: BookingHistory,
    prisonerDetails: List<PrisonerDetails>,
  ): MutableList<BookingHistoryAppointment> {
    val appointments = mutableListOf<BookingHistoryAppointment>()

    prisonerDetails.forEach { detail ->
      detail.appointments.forEach { app ->
        appointments.add(
          BookingHistoryAppointment(
            bookingHistory = history,
            prisonCode = detail.prisonCode!!,
            prisonerNumber = detail.prisonerNumber!!,
            appointmentDate = app.date!!,
            appointmentType = app.type?.let { app.type.name } ?: "UNKNOWN",
            prisonLocKey = app.locationKey!!,
            startTime = app.startTime!!,
            endTime = app.endTime!!,
          ),
        )
      }
    }

    return appointments
  }
}
