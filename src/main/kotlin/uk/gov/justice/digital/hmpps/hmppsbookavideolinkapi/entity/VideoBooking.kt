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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.requireNot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.LocalTime
import java.util.Objects
import java.util.UUID

@Entity
@Table(name = "video_booking")
class VideoBooking private constructor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val videoBookingId: Long = 0,

  @Enumerated(EnumType.STRING)
  val bookingType: BookingType,

  @OneToOne
  @JoinColumn(name = "court_id")
  val court: Court?,

  var hearingType: String?,

  @OneToOne
  @JoinColumn(name = "probation_team_id")
  val probationTeam: ProbationTeam?,

  var probationMeetingType: String?,

  @Deprecated(message = "This is superseded by notesForStaff and notesForPrisoners and no longer used.")
  var comments: String? = null,

  val createdByPrison: Boolean = false,

  val createdBy: String,

  val createdTime: LocalDateTime = now(),

  val migratedVideoBookingId: Long? = null,

  val migratedDescription: String? = null,
) {
  @OneToMany(mappedBy = "videoBooking", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val prisonAppointments: MutableList<PrisonAppointment> = mutableListOf()

  @Enumerated(EnumType.STRING)
  var statusCode: StatusCode = StatusCode.ACTIVE
    private set

  var amendedBy: String? = null

  var amendedTime: LocalDateTime? = null

  var notesForStaff: String? = null
    private set

  var notesForPrisoners: String? = null
    private set

  var videoUrl: String? = null
    private set

  var hmctsNumber: String? = null
    private set

  var guestPin: String? = null
    private set

  fun isBookingType(bookingType: BookingType) = this.bookingType == bookingType

  fun appointments() = prisonAppointments.toList()

  // TODO: Assumes one person per booking, so revisit for co-defendant cases
  fun prisoner() = appointments().map { it.prisonerNumber }.distinct().single()

  // TODO: Assumes one person per booking, so revisit for co-defendant cases
  fun prisonCode() = appointments().map { it.prison }.distinct().single().code

  fun isStatus(status: StatusCode) = statusCode == status

  fun isMigrated() = migratedVideoBookingId != null

  fun prisonIsEnabledForSelfService() = appointments().all { a -> a.prison.enabled }

  fun addAppointment(
    prison: Prison,
    prisonerNumber: String,
    appointmentType: String,
    date: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    locationId: UUID,
  ) = apply {
    prisonAppointments.add(
      PrisonAppointment.newAppointment(
        videoBooking = this,
        prison = prison,
        prisonerNumber = prisonerNumber,
        appointmentType = appointmentType,
        appointmentDate = date,
        startTime = startTime,
        endTime = endTime,
        locationId = locationId,
      ),
    )
  }

  fun amendCourtBooking(
    hearingType: String,
    notesForStaff: String?,
    notesForPrisoners: String?,
    cvpLinkDetails: CvpLinkDetails?,
    guestPin: String?,
    amendedBy: User,
  ) = apply {
    require(bookingType == BookingType.COURT) {
      "Booking is not a court booking."
    }

    this.hearingType = hearingType
    this.notesForStaff = notesForStaff
    if (amendedBy is PrisonUser) {
      this.notesForPrisoners = notesForPrisoners
    }
    this.videoUrl = cvpLinkDetails?.videoUrl
    this.hmctsNumber = cvpLinkDetails?.hmctsNumber
    this.guestPin = guestPin
    this.amendedBy = amendedBy.username
    amendedTime = now()
  }

  fun amendProbationBooking(
    probationMeetingType: String,
    notesForStaff: String?,
    notesForPrisoners: String?,
    amendedBy: User,
  ) = apply {
    require(bookingType == BookingType.PROBATION) {
      "Booking is not a probation booking."
    }

    this.probationMeetingType = probationMeetingType
    this.notesForStaff = notesForStaff
    if (amendedBy is PrisonUser) {
      this.notesForPrisoners = notesForPrisoners
    }
    this.amendedBy = amendedBy.username
    amendedTime = now()
  }

  fun removeAllAppointments() = prisonAppointments.clear()

  fun cancel(cancelledBy: User) = apply {
    require(statusCode != StatusCode.CANCELLED) {
      "Video booking $videoBookingId is already cancelled"
    }

    require(prisonAppointments.all { it.start().isAfter(now()) }) { "Video booking $videoBookingId cannot be cancelled" }

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

  override fun hashCode(): Int = videoBookingId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(videoBookingId = $videoBookingId)"

  fun preHearing(): PrisonAppointment? = appointments().singleOrNull { it.appointmentType == "VLB_COURT_PRE" }

  fun mainHearing(): PrisonAppointment? = appointments().singleOrNull { it.appointmentType == "VLB_COURT_MAIN" }

  fun postHearing(): PrisonAppointment? = appointments().singleOrNull { it.appointmentType == "VLB_COURT_POST" }

  fun probationMeeting(): PrisonAppointment? = appointments().singleOrNull { it.appointmentType == "VLB_PROBATION" }

  companion object {

    fun newCourtBooking(
      court: Court,
      hearingType: String,
      notesForStaff: String?,
      notesForPrisoners: String?,
      cvpLinkDetails: CvpLinkDetails?,
      guestPin: String?,
      createdBy: User,
    ): VideoBooking = VideoBooking(
      bookingType = BookingType.COURT,
      court = court,
      hearingType = hearingType,
      probationTeam = null,
      probationMeetingType = null,
      createdBy = createdBy.username,
      createdByPrison = createdBy is PrisonUser,
    ).apply {
      this.videoUrl = cvpLinkDetails?.videoUrl
      this.guestPin = guestPin
      this.notesForStaff = notesForStaff
      this.notesForPrisoners = notesForPrisoners?.takeIf { createdBy is PrisonUser }
      this.hmctsNumber = cvpLinkDetails?.hmctsNumber
    }

    fun newProbationBooking(
      probationTeam: ProbationTeam,
      probationMeetingType: String,
      notesForStaff: String?,
      notesForPrisoners: String?,
      createdBy: User,
    ): VideoBooking = VideoBooking(
      bookingType = BookingType.PROBATION,
      court = null,
      hearingType = null,
      probationTeam = probationTeam.also { requireNot(probationTeam.readOnly) { "Probation team with code ${it.code} is read only" } },
      probationMeetingType = probationMeetingType,
      createdBy = createdBy.username,
      createdByPrison = createdBy is PrisonUser,
    ).apply {
      this.notesForStaff = notesForStaff
      this.notesForPrisoners = notesForPrisoners?.takeIf { createdBy is PrisonUser }
    }
  }
}

enum class StatusCode {
  ACTIVE,
  CANCELLED,
}

/**
 * Simple value object for CVP link details which will have either a HMCTS number or a video URL but never both.
 */
class CvpLinkDetails private constructor(val hmctsNumber: String? = null, val videoUrl: String? = null) {
  companion object {
    fun hmctsNumber(value: String) = run {
      require(value.isNotBlank() && value.length <= 8) { "CVP HMCTS number must not be blank or greater than 8 characters" }

      CvpLinkDetails(hmctsNumber = value)
    }

    fun url(value: String) = run {
      require(value.isNotBlank() && value.length <= 120) { "CVP URL must not be blank or greater than 120 characters" }

      CvpLinkDetails(videoUrl = value)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CvpLinkDetails

    if (hmctsNumber != other.hmctsNumber) return false
    if (videoUrl != other.videoUrl) return false

    return true
  }

  override fun hashCode() = Objects.hash(hmctsNumber, videoUrl)
}
