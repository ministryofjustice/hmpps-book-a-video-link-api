package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import java.time.LocalDate
import java.time.LocalTime

/**
 * This entity represents a view projection for v_video_booking_history, and contains the history of a booking.
 *
 * It is read-only (immutable).
 */
@Entity
@Immutable
@Table(name = "v_video_booking_history")
data class VideoBookingHistory(
  @Id
  val videoBookingId: Long,

  val dateOfBooking: LocalDate,

  val historyType: String,

  val prisonCode: String,

  val courtDescription: String?,

  val courtCode: String?,

  val probationTeamDescription: String?,

  val probationTeamCode: String?,

  val createdByPrison: Boolean,

  val preLocationKey: String?,

  val mainLocationKey: String,

  val mainDate: LocalDate,

  val mainStartTime: LocalTime,

  val mainEndTime: LocalTime,

  val preDate: LocalDate? = null,

  val preStartTime: LocalTime? = null,

  val preEndTime: LocalTime? = null,

  val postLocationKey: String? = null,

  val postDate: LocalDate? = null,

  val postStartTime: LocalTime? = null,

  val postEndTime: LocalTime? = null,
) {
  fun isCourtBooking() = courtCode != null
}
