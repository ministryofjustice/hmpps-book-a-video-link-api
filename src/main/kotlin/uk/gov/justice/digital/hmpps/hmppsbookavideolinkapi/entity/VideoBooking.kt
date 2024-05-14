package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
@Table(name = "video_booking")
class VideoBooking private constructor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val videoBookingId: Long = 0,

  val bookingType: String,

  var statusCode: String = "ACTIVE",

  @OneToOne
  @JoinColumn(name = "court_id")
  val court: Court?,

  val hearingType: String?,

  @OneToOne
  @JoinColumn(name = "probation_team_id")
  val probationTeam: ProbationTeam?,

  val probationMeetingType: String?,

  var videoUrl: String?,

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

    other as VideoBooking

    return videoBookingId == other.videoBookingId
  }

  override fun hashCode(): Int {
    return videoBookingId.hashCode()
  }

  companion object {
    fun newCourtBooking(
      court: Court,
      hearingType: String,
      videoUrl: String?,
      createdBy: String,
    ): VideoBooking =
      // TODO check court is enabled?
      VideoBooking(
        bookingType = "COURT",
        court = court,
        hearingType = hearingType,
        probationTeam = null,
        probationMeetingType = null,
        videoUrl = videoUrl,
        createdBy = createdBy,
      )

    fun newProbationBooking(
      probationTeam: ProbationTeam,
      probationMeetingType: String,
      videoUrl: String?,
      createdBy: String,
    ): VideoBooking =
      // TODO check probation team is enabled?
      VideoBooking(
        bookingType = "PROBATION",
        court = null,
        hearingType = null,
        probationTeam = probationTeam,
        probationMeetingType = probationMeetingType,
        videoUrl = videoUrl,
        createdBy = createdBy,
      )
  }
}