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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isOnOrAfter
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isOnOrBefore
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isTimesOverlap
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import java.time.DayOfWeek
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

  var endDayOfWeek: Int = -1

  var startTime: LocalTime = LocalTime.MIN

  var endTime: LocalTime = LocalTime.MIN

  @Enumerated(EnumType.STRING)
  var locationUsage: LocationScheduleUsage = LocationScheduleUsage.BLOCKED

  var allowedParties: String? = null

  var notes: String? = null

  var amendedBy: String? = null

  var amendedTime: LocalDateTime? = null

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

  fun isUsage(usage: LocationScheduleUsage) = locationUsage == usage

  fun isSatisfiedBy(specification: Specification) = specification.predicate(this)

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
      locationAttribute.schedule().filterNot { it.locationScheduleId == this.locationScheduleId }.any {
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
  PROBATION_COURT,
  PROBATION_SENTENCE,
  BLOCKED,
}

fun interface Specification {
  fun predicate(schedule: LocationSchedule): Boolean
}

/**
 * Returns true if same location usage and there is any overlap with the supplied start and end times and the schedules.
 */
class OverlappingSpecification(
  private val usage: LocationScheduleUsage,
  private val dayOfWeek: DayOfWeek,
  private val startTime: LocalTime,
  private val endTime: LocalTime,
) : Specification {
  override fun predicate(schedule: LocationSchedule): Boolean = run {
    schedule.isUsage(usage) && dayOfWeek.between(schedule.startDayOfWeek, schedule.endDayOfWeek) && isTimesOverlap(startTime, endTime, schedule.startTime, schedule.endTime)
  }
}

/**
 * Returns true if same location usage and there is any overlap with the supplied start and end times and the schedule has allowed parties.
 */
class OverlappingRoomSpecification(
  private val usage: LocationScheduleUsage,
  private val dayOfWeek: DayOfWeek,
  private val startTime: LocalTime,
  private val endTime: LocalTime,
) : Specification {
  override fun predicate(schedule: LocationSchedule): Boolean = run {
    schedule.isUsage(usage) && dayOfWeek.between(schedule.startDayOfWeek, schedule.endDayOfWeek) && isTimesOverlap(startTime, endTime, schedule.startTime, schedule.endTime) && !schedule.allowedParties.isNullOrEmpty()
  }
}

abstract class AbstractSpecification(
  private val usage: LocationScheduleUsage,
  private val dayOfWeek: DayOfWeek,
  private val startTime: LocalTime,
  private val endTime: LocalTime,
) : Specification {
  override fun predicate(schedule: LocationSchedule): Boolean = run {
    schedule.isUsage(usage) &&
      dayOfWeek.between(schedule.startDayOfWeek, schedule.endDayOfWeek) &&
      startTime.isOnOrAfter(schedule.startTime) &&
      endTime.isOnOrBefore(schedule.endTime)
  }
}

/**
 * Returns true if court location usage, the supplied start and end times are between the schedules and the court matches allowed parties.
 */
class CourtRoomSpecification(
  private val court: Court,
  dayOfWeek: DayOfWeek,
  startTime: LocalTime,
  endTime: LocalTime,
) : AbstractSpecification(
  LocationScheduleUsage.COURT,
  dayOfWeek,
  startTime,
  endTime,
) {
  override fun predicate(schedule: LocationSchedule): Boolean = run {
    super.predicate(schedule) && schedule.allowedParties.orEmpty().split(",").contains(court.code)
  }
}

/**
 * Returns true if court location usage and the supplied start and end times are between the schedules.
 */
class CourtAnySpecification(
  dayOfWeek: DayOfWeek,
  startTime: LocalTime,
  endTime: LocalTime,
) : AbstractSpecification(
  LocationScheduleUsage.COURT,
  dayOfWeek,
  startTime,
  endTime,
) {
  override fun predicate(schedule: LocationSchedule): Boolean = run {
    super.predicate(schedule) && schedule.allowedParties.isNullOrBlank()
  }
}

/**
 * Returns true if same location usage, the supplied start and end times are between the schedules and the probation team matches allowed parties.
 */
class ProbationTeamSpecification(
  private val probationTeam: ProbationTeam,
  dayOfWeek: DayOfWeek,
  startTime: LocalTime,
  endTime: LocalTime,
) : AbstractSpecification(
  LocationScheduleUsage.PROBATION,
  dayOfWeek,
  startTime,
  endTime,
) {
  override fun predicate(schedule: LocationSchedule): Boolean = run {
    super.predicate(schedule) && schedule.allowedParties.orEmpty().split(",").contains(probationTeam.code)
  }
}

/**
 * Returns true if probation team usage and the supplied start and end times are between the schedules.
 */
class ProbationAnySpecification(
  dayOfWeek: DayOfWeek,
  startTime: LocalTime,
  endTime: LocalTime,
) : AbstractSpecification(
  LocationScheduleUsage.PROBATION,
  dayOfWeek,
  startTime,
  endTime,
) {
  override fun predicate(schedule: LocationSchedule): Boolean = run {
    super.predicate(schedule) && schedule.allowedParties.isNullOrBlank()
  }
}

/**
 * Returns true if probation court usage and the supplied start and end times are between the schedules.
 */
class ProbationCourtSpecification(
  private val probationTeam: ProbationTeam,
  dayOfWeek: DayOfWeek,
  startTime: LocalTime,
  endTime: LocalTime,
) : AbstractSpecification(
  LocationScheduleUsage.PROBATION_COURT,
  dayOfWeek,
  startTime,
  endTime,
) {
  override fun predicate(schedule: LocationSchedule): Boolean = run {
    super.predicate(schedule) && schedule.allowedParties.isNullOrBlank() && probationTeam.courtTeam
  }
}

/**
 * Returns true if probation sentence usage and the supplied start and end times are between the schedules.
 */
class ProbationSentenceManagementSpecification(
  private val probationTeam: ProbationTeam,
  dayOfWeek: DayOfWeek,
  startTime: LocalTime,
  endTime: LocalTime,
) : AbstractSpecification(
  LocationScheduleUsage.PROBATION_SENTENCE,
  dayOfWeek,
  startTime,
  endTime,
) {
  override fun predicate(schedule: LocationSchedule): Boolean = run {
    super.predicate(schedule) && schedule.allowedParties.isNullOrBlank() && probationTeam.sentenceManagementTeam
  }
}
