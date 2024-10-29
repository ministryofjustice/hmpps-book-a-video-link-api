package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMinutePrecision
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "prison_appointment")
class PrisonAppointment private constructor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonAppointmentId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "video_booking_id")
  val videoBooking: VideoBooking,

  @OneToOne
  @JoinColumn(name = "prison_id")
  val prison: Prison,

  val prisonerNumber: String,

  val appointmentType: String,

  val comments: String? = null,

  val prisonLocUuid: UUID,

  val appointmentDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime,
) {

  fun prisonCode() = prison.code

  fun isType(value: String) = appointmentType == value

  fun start(): LocalDateTime = appointmentDate.atTime(startTime)

  fun end(): LocalDateTime = appointmentDate.atTime(endTime)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as PrisonAppointment

    return prisonAppointmentId == other.prisonAppointmentId
  }

  override fun hashCode(): Int {
    return prisonAppointmentId.hashCode()
  }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(prisonAppointmentId = $prisonAppointmentId)"
  }

  companion object {
    fun newAppointment(
      videoBooking: VideoBooking,
      prison: Prison,
      prisonerNumber: String,
      appointmentType: String,
      appointmentDate: LocalDate,
      startTime: LocalTime,
      endTime: LocalTime,
      locationId: UUID,
    ) = PrisonAppointment(
      videoBooking = videoBooking,
      prison = prison,
      prisonerNumber = prisonerNumber,
      appointmentType = appointmentType,
      appointmentDate = appointmentDate,
      startTime = startTime.toMinutePrecision(),
      endTime = endTime.toMinutePrecision(),
      prisonLocUuid = locationId,
      comments = videoBooking.comments,
    )
  }
}
