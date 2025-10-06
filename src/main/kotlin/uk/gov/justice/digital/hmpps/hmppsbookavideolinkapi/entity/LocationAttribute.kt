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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.between
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.requireNot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "location_attribute")
class LocationAttribute private constructor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val locationAttributeId: Long = 0,

  val dpsLocationId: UUID,

  @OneToOne
  @JoinColumn(name = "prison_id")
  val prison: Prison,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) {
  @Enumerated(EnumType.STRING)
  var locationStatus: LocationStatus = LocationStatus.ACTIVE
    private set

  @Enumerated(EnumType.STRING)
  var locationUsage: LocationUsage = LocationUsage.SHARED
    private set

  var prisonVideoUrl: String? = null
    private set

  var notes: String? = null
    private set

  var amendedBy: String? = null
    private set

  var allowedParties: String? = null
    private set

  var amendedTime: LocalDateTime? = null
    private set

  var blockedFrom: LocalDate? = null
    private set

  var blockedTo: LocalDate? = null
    private set

  @OneToMany(mappedBy = "locationAttribute", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val locationSchedule: MutableList<LocationSchedule> = mutableListOf()

  fun schedule() = locationSchedule.toList()

  /**
   * You can add a schedule row to a location attribute even when it is not a scheduled attribute, however it won't have
   * any effect unless the location attribute itself is marked as scheduled.
   */
  fun addSchedule(
    usage: LocationScheduleUsage,
    startDayOfWeek: Int,
    endDayOfWeek: Int,
    startTime: LocalTime,
    endTime: LocalTime,
    allowedParties: Set<String> = emptySet(),
    notes: String? = null,
    createdBy: ExternalUser,
  ) {
    if (
      locationSchedule.any {
        it.locationUsage == usage &&
          it.startDayOfWeek == startDayOfWeek &&
          it.endDayOfWeek == endDayOfWeek &&
          it.startTime == startTime &&
          it.endTime == endTime &&
          it.allowedParties == allowedParties.takeUnless { ap -> ap.isEmpty() }?.sorted()?.joinToString(",")
      }
    ) {
      throw IllegalArgumentException("Cannot add a duplicate schedule row to location attribute with ID $locationAttributeId.")
    }

    locationSchedule.add(
      LocationSchedule.newSchedule(
        locationAttribute = this,
        locationUsage = usage,
        startDayOfWeek = startDayOfWeek,
        endDayOfWeek = endDayOfWeek,
        startTime = startTime,
        endTime = endTime,
        allowedParties = allowedParties,
        notes = notes,
        createdBy = createdBy,
      ),
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as LocationAttribute

    return locationAttributeId == other.locationAttributeId
  }

  override fun hashCode(): Int = locationAttributeId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName +
    "(locationAttributeId = $locationAttributeId, prisonId = ${prison.prisonId}, dpsLocationId = $dpsLocationId)"

  fun isAvailableFor(usageType: LocationUsageType, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime): AvailabilityStatus = run {
    if (locationStatus == LocationStatus.INACTIVE) return AvailabilityStatus.NONE

    if (locationStatus == LocationStatus.TEMPORARILY_BLOCKED && onDate.between(blockedFrom!!, blockedTo)) return AvailabilityStatus.NONE

    when (usageType) {
      is Court -> check(usageType, onDate, startTime, endTime)
      is ProbationTeam -> check(usageType, onDate, startTime, endTime)
    }
  }

  private fun check(probationTeam: ProbationTeam, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime): AvailabilityStatus {
    return when (locationUsage) {
      LocationUsage.SHARED -> AvailabilityStatus.SHARED
      LocationUsage.PROBATION -> when {
        allowedParties.isNullOrBlank() -> AvailabilityStatus.PROBATION_ANY
        isPartyAllowed(probationTeam.code) -> AvailabilityStatus.PROBATION_ROOM
        else -> AvailabilityStatus.NONE
      }

      LocationUsage.SCHEDULE -> getScheduleAvailability(probationTeam, onDate, startTime, endTime)
      else -> return AvailabilityStatus.NONE
    }
  }

  /**
   * We need to look at the schedules as a whole to determine availability. A schedule on its own is not enough.
   */
  private fun getScheduleAvailability(team: ProbationTeam, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime): AvailabilityStatus = run {
    val blocked = OverlappingSpecification(LocationScheduleUsage.BLOCKED, onDate.dayOfWeek, startTime, endTime)
    val freeForProbationTeam = ProbationTeamSpecification(team, onDate.dayOfWeek, startTime, endTime)
    val freeForAnyProbationTeam = ProbationAnySpecification(onDate.dayOfWeek, startTime, endTime)
    val overlapsWithCourtSlot = OverlappingSpecification(LocationScheduleUsage.COURT, onDate.dayOfWeek, startTime, endTime)
    val overlapsWithOtherProbationTeamSlot = OverlappingRoomSpecification(LocationScheduleUsage.PROBATION, onDate.dayOfWeek, startTime, endTime)

    // The order in which the checks are carried out is important and must be maintained.
    return when {
      fallsWithin(blocked) -> AvailabilityStatus.NONE
      fallsWithin(freeForProbationTeam) -> AvailabilityStatus.PROBATION_ROOM
      fallsWithin(freeForAnyProbationTeam) -> AvailabilityStatus.PROBATION_ANY

      // If none of the above match, we need to make sure the requested probation slot date and times to not overlap any court schedules
      fallsWithin(overlapsWithCourtSlot) -> AvailabilityStatus.NONE

      // If none of the above match, we need to make sure the requested probation slot date and times to not overlap any other probation team room schedules
      fallsWithin(overlapsWithOtherProbationTeamSlot) -> AvailabilityStatus.NONE

      else -> AvailabilityStatus.SHARED
    }
  }

  private fun check(court: Court, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime): AvailabilityStatus {
    return when (locationUsage) {
      LocationUsage.SHARED -> AvailabilityStatus.SHARED
      LocationUsage.COURT -> when {
        allowedParties.isNullOrBlank() -> AvailabilityStatus.COURT_ANY
        isPartyAllowed(court.code) -> AvailabilityStatus.COURT_ROOM
        else -> AvailabilityStatus.NONE
      }

      LocationUsage.SCHEDULE -> getScheduleAvailability(court, onDate, startTime, endTime)
      else -> return AvailabilityStatus.NONE
    }
  }

  /**
   * We need to look at the schedules as a whole to determine availability. A schedule on its own is not enough.
   */
  private fun getScheduleAvailability(court: Court, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime): AvailabilityStatus = run {
    val blocked = OverlappingSpecification(LocationScheduleUsage.BLOCKED, onDate.dayOfWeek, startTime, endTime)
    val freeForCourtRoom = CourtRoomSpecification(court, onDate.dayOfWeek, startTime, endTime)
    val freeForAnyCourtRoom = CourtAnySpecification(onDate.dayOfWeek, startTime, endTime)
    val overlapsWithProbationSlot = OverlappingSpecification(LocationScheduleUsage.PROBATION, onDate.dayOfWeek, startTime, endTime)
    val overlapsWithOtherCourtRoomSlot = OverlappingRoomSpecification(LocationScheduleUsage.COURT, onDate.dayOfWeek, startTime, endTime)

    // The order in which the checks are carried out is important and must be maintained.
    return when {
      fallsWithin(blocked) -> AvailabilityStatus.NONE
      fallsWithin(freeForCourtRoom) -> AvailabilityStatus.COURT_ROOM
      fallsWithin(freeForAnyCourtRoom) -> AvailabilityStatus.COURT_ANY

      // If none of the above match, we need to make sure the requested court slot date and times to not overlap any probation schedules
      fallsWithin(overlapsWithProbationSlot) -> AvailabilityStatus.NONE

      // If none of the above match, we need to make sure the requested court slot date and times to not overlap any other court room schedules
      fallsWithin(overlapsWithOtherCourtRoomSlot) -> AvailabilityStatus.NONE

      else -> AvailabilityStatus.SHARED
    }
  }

  private fun fallsWithin(specification: Specification) = locationSchedule.any { it.isSatisfiedBy(specification) }

  private fun isPartyAllowed(party: String) = allowedParties.orEmpty().replace(" ", "").split(",").contains(party)

  companion object {
    fun decoratedRoom(
      dpsLocationId: UUID,
      prison: Prison,
      locationUsage: LocationUsage,
      allowedParties: Set<String>,
      locationStatus: LocationStatus,
      prisonVideoUrl: String?,
      notes: String?,
      blockedFrom: LocalDate? = null,
      blockedTo: LocalDate? = null,
      createdBy: ExternalUser,
    ) = LocationAttribute(
      dpsLocationId = dpsLocationId,
      prison = prison,
      createdBy = createdBy.username,
    ).apply {
      if (locationStatus == LocationStatus.TEMPORARILY_BLOCKED) {
        require(blockedFrom != null && blockedTo != null) {
          "Cannot create a temporary location attribute without a blocked from and blocked to date."
        }

        requireNot(blockedFrom.isBefore(LocalDate.now())) {
          "The blocked from date must be today or later."
        }

        requireNot(blockedFrom.isAfter(blockedTo)) {
          "The blocked to date must be on after the blocked from date."
        }
      }

      this.locationStatus = locationStatus
      this.locationUsage = locationUsage
      this.prisonVideoUrl = prisonVideoUrl
      this.notes = notes
      this.allowedParties = allowedParties.takeUnless { it.isEmpty() }?.joinToString(",")
      this.blockedFrom = blockedFrom.takeIf { locationStatus == LocationStatus.TEMPORARILY_BLOCKED }
      this.blockedTo = blockedTo.takeIf { locationStatus == LocationStatus.TEMPORARILY_BLOCKED }
    }

    fun amend(
      locationAttributeToAmend: LocationAttribute,
      locationStatus: LocationStatus,
      locationUsage: LocationUsage,
      allowedParties: Set<String>,
      prisonVideoUrl: String?,
      comments: String?,
      blockedFrom: LocalDate? = null,
      blockedTo: LocalDate? = null,
      amendedBy: ExternalUser,
    ) = run {
      if (locationStatus == LocationStatus.TEMPORARILY_BLOCKED) {
        require(blockedFrom != null && blockedTo != null) {
          "Cannot amend a temporary blocked location attribute without a blocked from and blocked to date."
        }

        requireNot(blockedFrom.isAfter(blockedTo)) {
          "The blocked to date must be on after the blocked from date."
        }

        requireNot(blockedTo.isBefore(LocalDate.now())) {
          "The blocked to date must be today or later."
        }
      }

      locationAttributeToAmend.apply {
        this.locationStatus = locationStatus
        this.locationUsage = locationUsage
        this.prisonVideoUrl = prisonVideoUrl
        this.notes = comments
        this.amendedBy = amendedBy.username
        this.amendedTime = LocalDateTime.now()
        this.allowedParties = allowedParties.takeUnless { it.isEmpty() }?.joinToString(",")
        this.blockedFrom = blockedFrom.takeIf { locationStatus == LocationStatus.TEMPORARILY_BLOCKED }
        this.blockedTo = blockedTo.takeIf { locationStatus == LocationStatus.TEMPORARILY_BLOCKED }
      }
    }
  }
}

enum class LocationStatus {
  ACTIVE,
  INACTIVE,
  TEMPORARILY_BLOCKED,
}

enum class LocationUsage {
  COURT,
  PROBATION,
  SHARED,
  SCHEDULE,
}

enum class AvailabilityStatus {
  PROBATION_ROOM,
  PROBATION_ANY,
  COURT_ROOM,
  COURT_ANY,
  SHARED,
  NONE,
}

// A marker interface to help identify the types of supported location usages e.g., Court, ProbationTeam
sealed interface LocationUsageType
