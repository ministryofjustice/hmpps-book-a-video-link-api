package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * This entity represents a view projection for v_video_booking_history, and contains the history of a booking.
 *
 * It is read-only (immutable).
 */
@Entity
@Immutable
@Table(name = "v_video_booking_event")
data class VideoBookingEvent(
  @Id
  val eventId: Long,

  val videoBookingId: Long,

  val dateOfBooking: LocalDate,

  val timestamp: LocalDateTime,

  val eventType: String,

  val prisonCode: String,

  val courtDescription: String? = null,

  val courtCode: String? = null,

  val probationTeamDescription: String? = null,

  val probationTeamCode: String? = null,

  val createdByPrison: Boolean,

  val preLocationId: String?,

  val mainLocationId: String,

  val mainDate: LocalDate,

  val mainStartTime: LocalTime,

  val mainEndTime: LocalTime,

  val preDate: LocalDate? = null,

  val preStartTime: LocalTime? = null,

  val preEndTime: LocalTime? = null,

  val postLocationId: String? = null,

  val postDate: LocalDate? = null,

  val postStartTime: LocalTime? = null,

  val postEndTime: LocalTime? = null,

  val courtBooking: Boolean,
)
