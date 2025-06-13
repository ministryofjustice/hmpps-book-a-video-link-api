package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * This entity represents a view projection for v_prison_schedule and contains the prison appointment times, locations
 * and booking details for the criteria specified in the repository methods (different purposes).
 * It is read-only (immutable). Used for obtaining the data for the data schedule view.
 */

@Entity
@Immutable
@Table(name = "v_prison_schedule")
data class ScheduleItem(
  val videoBookingId: Long,

  @Id
  val prisonAppointmentId: Long,

  val bookingType: String,

  val statusCode: String,

  val videoUrl: String?,

  @Deprecated("use notesoff the booking instead")
  val bookingComments: String?,

  val createdByPrison: Boolean,

  val courtId: Long?,

  val courtCode: String?,

  val courtDescription: String?,

  val hearingType: String?,

  val hearingTypeDescription: String?,

  val probationTeamId: Long?,

  val probationTeamCode: String?,

  val probationTeamDescription: String?,

  val probationMeetingType: String?,

  val probationMeetingTypeDescription: String?,

  val prisonCode: String,

  val prisonName: String,

  val prisonerNumber: String,

  val appointmentType: String,

  val appointmentTypeDescription: String?,

  @Deprecated("use notes/comments off the booking instead")
  val appointmentComments: String?,

  val prisonLocationId: UUID,

  val appointmentDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime,

  val createdTime: LocalDateTime,

  val createdBy: String,

  val updatedTime: LocalDateTime?,

  val updatedBy: String?,

  val probationOfficerName: String?,

  val probationOfficerEmailAddress: String?,

  val notesForStaff: String?,

  val notesForPrisoners: String?,

  val hmctsNumber: String?,

  val guestPin: String?,
)
