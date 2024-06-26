package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * This entity represents the history of a booking at a point in time, and is created/saved
 * whenever a video link booking is CREATED, AMENDED or CANCELLED. Upon each the details of the
 * related appointments should also be recorded as a list.
 */

@Entity
@Table(name = "booking_history")
class BookingHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val bookingHistoryId: Long = 0L,

  val videoBookingId: Long,

  @Enumerated(EnumType.STRING)
  val historyType: HistoryType,

  val courtId: Long? = null,

  val hearingType: String? = null,

  val probationTeamId: Long? = null,

  val probationMeetingType: String? = null,

  val videoUrl: String? = null,

  val comments: String? = null,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) {
  @OneToMany(mappedBy = "bookingHistory", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val appointments: MutableList<BookingHistoryAppointment> = mutableListOf()

  fun addBookingHistoryAppointments(bookingHistoryAppointments: List<BookingHistoryAppointment>) =
    this.appointments.addAll(bookingHistoryAppointments)

  fun appointments() = appointments.toList()
}

/**
 * This is a separate entity so that it can cater for different prisoners, at different prisons, being
 * related to a single video link booking - the co-defendant coses. At present, the user journeys for
 * create and amend cannot have more than one prisoner per booking, but this is future-proofing it.
 */

@Entity
@Table(name = "booking_history_appointment")
data class BookingHistoryAppointment(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val bookingHistoryAppointmentId: Long = 0L,

  @ManyToOne
  @JoinColumn(name = "booking_history_id", nullable = false)
  val bookingHistory: BookingHistory,

  val prisonCode: String,

  val prisonerNumber: String,

  val appointmentDate: LocalDate,

  val appointmentType: String,

  val prisonLocKey: String,

  val startTime: LocalTime,

  val endTime: LocalTime,
)

enum class HistoryType {
  CREATE,
  AMEND,
}