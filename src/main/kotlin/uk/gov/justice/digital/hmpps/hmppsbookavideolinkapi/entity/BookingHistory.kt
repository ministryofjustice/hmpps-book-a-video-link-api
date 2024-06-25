package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.jvm.internal.impl.load.kotlin.AbstractBinaryClassAnnotationLoader.Companion

/**
 * This entity represents the history of a booking at a point in time, and is created/saved
 * whenever a video link booking is CREATED, AMENDED or CANCELLED. Upon each the details of the
 * related appointments should also be written into the appointments attribute, as a list.
 */

@Entity
@Table(name = "booking_history")
class BookingHistory private constructor (
  @Id
  val bookingHistoryId: Long = 0L,

  val videoBookingId: Long,

  val historyType: String, // CREATED, AMENDED, CANCELLED

  val courtId: Long? = null,

  val hearingType: String? = null,

  val probationTeamId: Long? = null,

  val probationMeetingType: String? = null,

  val videoUrl: String? = null,

  val comments: String? = null,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),

  @OneToMany(mappedBy = "booking_history", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  val appointments: MutableList<BookingHistoryAppointment> = mutableListOf(),
) {

  companion object {
    fun courtHistory(booking: VideoBooking, historyType: String, prisoners: List<PrisonerDetails>) =
      BookingHistory(
        videoBookingId = booking.videoBookingId,
        historyType = historyType,
        courtId = booking.court?.courtId,
        hearingType = booking.hearingType,
        videoUrl = booking.videoUrl,
        comments = booking.comments,
        createdBy = booking.createdBy,
        appointments = ,
      )

    fun probationHistory(booking: VideoBooking, historyType: String, prisoners: List<PrisonerDetails>) =
      BookingHistory(
        videoBookingId = booking.videoBookingId,
        historyType = historyType,
        probationTeamId = booking.probationTeam?.probationTeamId,
        probationMeetingType = booking.probationMeetingType,
        videoUrl = booking.videoUrl,
        comments = booking.comments,
        createdBy = booking.createdBy,
        appointments = ,
      )
  }
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
  val bookingHistoryAppointmentId: Long,

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

