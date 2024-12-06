package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * This entity represents a view projection for v_video_appointments, and contains the prison appointments
 * for the criteria specified in the repository methods (different purposes). It is read-only (immutable).
 */
@Entity
@Immutable
@Table(name = "v_video_appointments")
data class VideoAppointment(
  @Id
  val prisonAppointmentId: Long,

  val videoBookingId: Long,

  val bookingType: String,

  val statusCode: String,

  val courtCode: String?,

  val probationTeamCode: String?,

  val prisonCode: String,

  val prisonerNumber: String,

  val appointmentType: String,

  override val prisonLocationId: UUID,

  val appointmentDate: LocalDate,

  override val startTime: LocalTime,

  override val endTime: LocalTime,
) : AppointmentSlot

interface AppointmentSlot {
  val prisonLocationId: UUID

  val startTime: LocalTime

  val endTime: LocalTime
}
