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
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.LocalTime

@Entity
@Table(name = "video_booking")
class VideoBooking private constructor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val videoBookingId: Long = 0,

  val bookingType: String,

  @OneToOne
  @JoinColumn(name = "court_id")
  var court: Court?,

  var hearingType: String?,

  @OneToOne
  @JoinColumn(name = "probation_team_id")
  var probationTeam: ProbationTeam?,

  var probationMeetingType: String?,

  var comments: String? = null,

  var videoUrl: String? = null,

  val createdByPrison: Boolean = false,

  val createdBy: String,

  val createdTime: LocalDateTime = now(),
) {

  @OneToMany(mappedBy = "videoBooking", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val prisonAppointments: MutableList<PrisonAppointment> = mutableListOf()

  @Enumerated(EnumType.STRING)
  var statusCode: StatusCode = StatusCode.ACTIVE
    private set

  var amendedBy: String? = null

  var amendedTime: LocalDateTime? = null

  fun isCourtBooking() = bookingType == "COURT"

  fun appointments() = prisonAppointments.toList()

  // TODO: Assumes one person per booking, so revisit for co-defendant cases
  fun prisoner() = appointments().map { it.prisonerNumber }.distinct().single()

  fun isStatus(status: StatusCode) = statusCode == status

  fun addAppointment(
    prisonCode: String,
    prisonerNumber: String,
    appointmentType: String,
    date: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    locationKey: String,
  ) =
    apply {
      prisonAppointments.add(
        PrisonAppointment.newAppointment(
          videoBooking = this,
          prisonCode = prisonCode,
          prisonerNumber = prisonerNumber,
          appointmentType = appointmentType,
          appointmentDate = date,
          startTime = startTime,
          endTime = endTime,
          locationKey = locationKey,
        ),
      )
    }

  fun removeAllAppointments() = prisonAppointments.clear()

  fun cancel(cancelledBy: User) =
    apply {
      require(statusCode != StatusCode.CANCELLED) {
        "Video booking $videoBookingId is already cancelled"
      }

      require(prisonAppointments.all { it.isStartsAfter(now()) }) { "Video booking $videoBookingId cannot be cancelled" }

      statusCode = StatusCode.CANCELLED
      amendedBy = cancelledBy.username
      amendedTime = now()
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as VideoBooking

    return videoBookingId == other.videoBookingId
  }

  override fun hashCode(): Int {
    return videoBookingId.hashCode()
  }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(videoBookingId = $videoBookingId)"
  }

  companion object {
    fun newCourtBooking(
      court: Court,
      hearingType: String,
      comments: String?,
      videoUrl: String?,
      createdBy: String,
      createdByPrison: Boolean,
    ): VideoBooking =
      VideoBooking(
        bookingType = "COURT",
        court = court,
        hearingType = hearingType,
        probationTeam = null,
        probationMeetingType = null,
        comments = comments,
        videoUrl = videoUrl,
        createdBy = createdBy,
        createdByPrison = createdByPrison,
      )

    fun newProbationBooking(
      probationTeam: ProbationTeam,
      probationMeetingType: String,
      comments: String?,
      videoUrl: String?,
      createdBy: String,
      createdByPrison: Boolean,
    ): VideoBooking =
      VideoBooking(
        bookingType = "PROBATION",
        court = null,
        hearingType = null,
        probationTeam = probationTeam,
        probationMeetingType = probationMeetingType,
        comments = comments,
        videoUrl = videoUrl,
        createdBy = createdBy,
        createdByPrison = createdByPrison,
      )
  }
}

enum class StatusCode {
  ACTIVE,
  CANCELLED,
}
