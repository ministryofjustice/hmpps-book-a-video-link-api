package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMinutePrecision
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "prison_appointment")
class PrisonAppointment private constructor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonAppointmentId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "video_booking_id")
  val videoBooking: VideoBooking,

  val prisonCode: String,

  val prisonerNumber: String,

  val appointmentType: String,

  val comments: String? = null,

  val prisonLocKey: String,

  val appointmentDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) {

  var amendedBy: String? = null
    private set

  var amendedTime: LocalDateTime? = null
    private set

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as PrisonAppointment

    return prisonAppointmentId == other.prisonAppointmentId
  }

  override fun hashCode(): Int {
    return prisonAppointmentId.hashCode()
  }

  companion object {
    fun newAppointment(
      videoBooking: VideoBooking,
      prisonCode: String,
      prisonerNumber: String,
      appointmentType: String,
      appointmentDate: LocalDate,
      startTime: LocalTime,
      endTime: LocalTime,
      locationKey: String,
      createdBy: String,
    ) = PrisonAppointment(
      videoBooking = videoBooking,
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      appointmentType = appointmentType,
      appointmentDate = appointmentDate,
      startTime = startTime.toMinutePrecision(),
      endTime = endTime.toMinutePrecision(),
      prisonLocKey = locationKey,
      createdBy = createdBy,
    )
  }
}
