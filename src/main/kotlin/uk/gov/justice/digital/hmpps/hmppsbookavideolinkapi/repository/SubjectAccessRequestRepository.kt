package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Component
class SubjectAccessRequestRepository(private val repository: BookingHistoryRepository) {

  fun findBy(prn: String, fromDate: LocalDate, toDate: LocalDate): List<SubjectAccessRequestDto> = run {
    repository.findBy(
      prisonerNumber = prn,
      from = LocalDateTime.of(fromDate, LocalTime.MIN),
      to = LocalDateTime.of(toDate, LocalTime.MAX),
    ).flatMap { bookingHistory -> bookingHistory.appointments().sortedBy { it.startTime } }.map(::SubjectAccessRequestDto)
  }
}

data class SubjectAccessRequestDto(
  val bookingHistoryId: Long,
  val videoBookingId: Long,
  val prisonCode: String,
  val prisonerNumber: String,
  val historyType: HistoryType,
  val courtId: Long?,
  val hearingType: String?,
  val probationTeamId: Long?,
  val probationMeetingType: String?,
  val videoUrl: String?,
  val appointmentType: String,
  val appointmentDate: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
  val prisonLocationId: UUID,
  val comments: String?,
  val staffNotes: String?,
  val prisonerNotes: String?,
  val createdBy: String,
  val createdTime: LocalDateTime,
) {
  constructor(bha: BookingHistoryAppointment) : this(
    bookingHistoryId = bha.bookingHistory.bookingHistoryId,
    videoBookingId = bha.bookingHistory.videoBookingId,
    prisonCode = bha.prisonCode,
    prisonerNumber = bha.prisonerNumber,
    historyType = bha.bookingHistory.historyType,
    courtId = bha.bookingHistory.courtId,
    hearingType = bha.bookingHistory.hearingType,
    probationTeamId = bha.bookingHistory.probationTeamId,
    probationMeetingType = bha.bookingHistory.probationMeetingType,
    videoUrl = bha.bookingHistory.videoUrl,
    appointmentType = bha.appointmentType,
    appointmentDate = bha.appointmentDate,
    startTime = bha.startTime,
    endTime = bha.endTime,
    prisonLocationId = bha.prisonLocationId,
    comments = bha.bookingHistory.comments,
    staffNotes = bha.bookingHistory.notesForStaff,
    prisonerNotes = bha.bookingHistory.notesForPrisoners,
    createdBy = bha.bookingHistory.createdBy,
    createdTime = bha.bookingHistory.createdTime,
  )
}
