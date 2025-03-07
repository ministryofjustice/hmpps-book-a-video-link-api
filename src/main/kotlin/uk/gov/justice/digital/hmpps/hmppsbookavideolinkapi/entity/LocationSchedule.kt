package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.between
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isBetween
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "location_schedule")
class LocationSchedule private constructor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val locationScheduleId: Long = 0,

  @JoinColumn(name = "location_attribute_id")
  @ManyToOne(fetch = FetchType.LAZY)
  val locationAttribute: LocationAttribute,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) {
  var startDayOfWeek: Int = -1
    private set

  var endDayOfWeek: Int = -1
    private set

  var startTime: LocalTime = LocalTime.MIN
    private set

  var endTime: LocalTime = LocalTime.MIN
    private set

  @Enumerated(EnumType.STRING)
  var locationUsage: LocationScheduleUsage = LocationScheduleUsage.SHARED
    private set

  var allowedParties: String? = null
    private set

  var notes: String? = null
    private set

  init {
    require(locationAttribute.locationUsage == LocationUsage.SCHEDULE) {
      "The location usage type must be SCHEDULE for a list of schedule rows to be associated with it."
    }
  }

  var amendedBy: String? = null
    private set

  var amendedTime: LocalDateTime? = null
    private set

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as LocationSchedule

    return locationScheduleId == other.locationScheduleId
  }

  override fun hashCode(): Int = locationScheduleId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName +
    "(locationScheduleId = $locationScheduleId, startDay = $startDayOfWeek, endDay = $endDayOfWeek " +
    "startTime = $startTime, endTime = $endTime)"

  fun fallsOn(dateTime: LocalDateTime) = dateTime.dayOfWeek.between(startDayOfWeek, endDayOfWeek) && dateTime.toLocalTime().isBetween(startTime, endTime)

  fun isForProbationTeam(team: ProbationTeam) = locationUsage == LocationScheduleUsage.PROBATION && allowedParties.orEmpty().split(",").contains(team.code)

  fun isForAnyProbationTeam() = locationUsage == LocationScheduleUsage.PROBATION && allowedParties.isNullOrBlank()

  fun isShared() = locationUsage == LocationScheduleUsage.SHARED

  fun amend(
    locationUsage: LocationScheduleUsage,
    startDayOfWeek: Int,
    endDayOfWeek: Int,
    startTime: LocalTime,
    endTime: LocalTime,
    allowedParties: Set<String>?,
    notes: String?,
    amendedBy: ExternalUser,
  ) = apply {
    require(startDayOfWeek <= endDayOfWeek) { "The end day cannot be before the start day." }
    require(startTime.isBefore(endTime)) { "The end time must come after the start time." }

    if (
      locationAttribute.schedule().any {
        it.locationScheduleId != locationScheduleId
        it.locationUsage == locationUsage &&
          it.startDayOfWeek == startDayOfWeek &&
          it.endDayOfWeek == endDayOfWeek &&
          it.startTime == startTime &&
          it.endTime == endTime &&
          it.allowedParties == allowedParties.takeUnless { ap -> ap.isNullOrEmpty() }?.sorted()?.joinToString(",")
      }
    ) {
      throw IllegalArgumentException("Cannot amend, amendment would conflict with an existing scheduled row.")
    }

    this.locationUsage = locationUsage
    this.startDayOfWeek = startDayOfWeek
    this.endDayOfWeek = endDayOfWeek
    this.startTime = startTime
    this.endTime = endTime
    this.allowedParties = allowedParties.takeUnless { it.isNullOrEmpty() }?.sorted()?.joinToString(",")
    this.notes = notes
    this.amendedBy = amendedBy.username
    this.amendedTime = LocalDateTime.now()
  }

  companion object {
    fun newSchedule(
      locationAttribute: LocationAttribute,
      locationUsage: LocationScheduleUsage,
      startDayOfWeek: Int,
      endDayOfWeek: Int,
      startTime: LocalTime,
      endTime: LocalTime,
      allowedParties: Set<String>?,
      notes: String? = null,
      createdBy: ExternalUser,
    ) = LocationSchedule(
      locationAttribute = locationAttribute,
      createdBy = createdBy.username,
    ).apply {
      require(startDayOfWeek <= endDayOfWeek) { "The end day cannot be before the start day." }
      require(startTime.isBefore(endTime)) { "The end time must come after the start time." }

      this.locationUsage = locationUsage
      this.startDayOfWeek = startDayOfWeek
      this.endDayOfWeek = endDayOfWeek
      this.startTime = startTime
      this.endTime = endTime
      this.allowedParties = allowedParties.takeUnless { it.isNullOrEmpty() }?.sorted()?.joinToString(",")
      this.notes = notes
    }
  }
}

enum class LocationScheduleUsage {
  COURT,
  PROBATION,
  SHARED,
  BLOCKED,
}
