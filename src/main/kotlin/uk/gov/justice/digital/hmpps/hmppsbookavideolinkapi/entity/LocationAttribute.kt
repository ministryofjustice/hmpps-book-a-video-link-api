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

  val statusMessage: String? = null,

  val expectedActiveDate: LocalDate? = null,

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

  @OneToMany(mappedBy = "locationAttribute", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val locationSchedule: MutableList<LocationSchedule> = mutableListOf()

  fun amend(
    locationStatus: LocationStatus,
    locationUsage: LocationUsage,
    allowedParties: Set<String>,
    prisonVideoUrl: String?,
    comments: String?,
    amendedBy: ExternalUser,
  ) = apply {
    this.locationStatus = locationStatus
    this.locationUsage = locationUsage
    this.prisonVideoUrl = prisonVideoUrl
    this.notes = comments
    this.amendedBy = amendedBy.username
    this.amendedTime = LocalDateTime.now()
    this.allowedParties = allowedParties.takeUnless { it.isEmpty() }?.joinToString(",")
  }

  fun schedule() = locationSchedule.toList()

  fun isLocationUsage(usage: LocationUsage) = locationUsage == usage

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

  fun isAvailableFor(probationTeam: ProbationTeam, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime): AvailabilityStatus = check(probationTeam, onDate, startTime, endTime)

  private fun check(probationTeam: ProbationTeam, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime): AvailabilityStatus {
    if (locationStatus == LocationStatus.INACTIVE) return AvailabilityStatus.NONE

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

      // If none of the above match we need to make sure the requested probation slot date and times to not overlap any court schedules
      fallsWithin(overlapsWithCourtSlot) -> AvailabilityStatus.NONE

      // If none of the above match we need to make sure the requested probation slot date and times to not overlap any other probation team room schedules
      fallsWithin(overlapsWithOtherProbationTeamSlot) -> AvailabilityStatus.NONE

      else -> AvailabilityStatus.SHARED
    }
  }

  fun isAvailableFor(court: Court, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime): AvailabilityStatus = check(court, onDate, startTime, endTime)

  private fun check(court: Court, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime): AvailabilityStatus {
    if (locationStatus == LocationStatus.INACTIVE) return AvailabilityStatus.NONE

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

      // If none of the above match we need to make sure the requested court slot date and times to not overlap any probation schedules
      fallsWithin(overlapsWithProbationSlot) -> AvailabilityStatus.NONE

      // If none of the above match we need to make sure the requested court slot date and times to not overlap any other court room schedules
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
      createdBy: ExternalUser,
    ) = LocationAttribute(
      dpsLocationId = dpsLocationId,
      prison = prison,
      createdBy = createdBy.username,
    ).apply {
      this.locationStatus = locationStatus
      this.locationUsage = locationUsage
      this.prisonVideoUrl = prisonVideoUrl
      this.notes = notes
      this.allowedParties = allowedParties.takeUnless { it.isEmpty() }?.joinToString(",")
    }
  }
}

enum class LocationStatus {
  ACTIVE,
  INACTIVE,
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
