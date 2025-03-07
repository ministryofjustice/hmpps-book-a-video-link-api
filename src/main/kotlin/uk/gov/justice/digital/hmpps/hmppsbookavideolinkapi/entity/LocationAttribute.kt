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
    require(locationUsage == LocationUsage.SCHEDULE) {
      "The location usage type must be SCHEDULE to add a schedule row to it."
    }

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

  fun isAvailableFor(probationTeam: ProbationTeam, startingOnDateTime: LocalDateTime): AvailabilityStatus = check(probationTeam, startingOnDateTime)

  private fun check(probationTeam: ProbationTeam, startingOnDateTime: LocalDateTime): AvailabilityStatus {
    if (locationStatus == LocationStatus.INACTIVE) return AvailabilityStatus.NONE

    return when (locationUsage) {
      LocationUsage.SHARED -> AvailabilityStatus.SHARED
      LocationUsage.PROBATION -> when {
        allowedParties.isNullOrBlank() -> AvailabilityStatus.PROBATION_ANY
        isPartyAllowed(probationTeam.code) -> AvailabilityStatus.PROBATION_TEAM
        else -> AvailabilityStatus.NONE
      }

      LocationUsage.SCHEDULE -> getScheduleAvailability(probationTeam, startingOnDateTime)
      else -> return AvailabilityStatus.NONE
    }
  }

  /**
   * We need to look at the schedules as a whole to determine availability. A schedule on its own is not enough.
   */
  private fun getScheduleAvailability(probationTeam: ProbationTeam, dateAndTime: LocalDateTime): AvailabilityStatus {
    val schedules = locationSchedule.filter { it.fallsOn(dateAndTime) }

    return when {
      // If there aren't any schedules which fall on the requested date and time then default to SHARED
      schedules.isEmpty() -> AvailabilityStatus.SHARED
      // If there are schedules fall on the date and time look and see if any match the following
      schedules.any { schedule -> schedule.isForProbationTeam(probationTeam) } -> AvailabilityStatus.PROBATION_TEAM
      schedules.any { schedule -> schedule.isForAnyProbationTeam() } -> AvailabilityStatus.PROBATION_ANY
      schedules.any { schedule -> schedule.isShared() } -> AvailabilityStatus.SHARED
      // If there are no matches above then it is not available
      else -> AvailabilityStatus.NONE
    }
  }

  fun isAvailableFor(court: Court, startingOnDateTime: LocalDateTime): AvailabilityStatus = check(court, startingOnDateTime)

  private fun check(court: Court, startingOnDateTime: LocalDateTime): AvailabilityStatus {
    if (locationStatus == LocationStatus.INACTIVE) return AvailabilityStatus.NONE

    return when (locationUsage) {
      LocationUsage.SHARED -> AvailabilityStatus.SHARED
      LocationUsage.COURT -> when {
        allowedParties.isNullOrBlank() -> AvailabilityStatus.COURT_ANY
        isPartyAllowed(court.code) -> AvailabilityStatus.COURT_ROOM
        else -> AvailabilityStatus.NONE
      }

      LocationUsage.SCHEDULE -> getScheduleAvailability(court, startingOnDateTime)
      else -> return AvailabilityStatus.NONE
    }
  }

  /**
   * We need to look at the schedules as a whole to determine availability. A schedule on its own is not enough.
   */
  private fun getScheduleAvailability(court: Court, dateAndTime: LocalDateTime): AvailabilityStatus {
    val schedules = locationSchedule.filter { it.fallsOn(dateAndTime) }

    return when {
      // If there aren't any schedules which fall on the requested date and time then default to SHARED
      schedules.isEmpty() -> AvailabilityStatus.SHARED
      // If there are schedules fall on the date and time look and see if any match the following
      schedules.any { schedule -> schedule.isForCourt(court) } -> AvailabilityStatus.COURT_ROOM
      schedules.any { schedule -> schedule.isForAnyCourt() } -> AvailabilityStatus.COURT_ANY
      schedules.any { schedule -> schedule.isShared() } -> AvailabilityStatus.SHARED
      // If there are no matches above then it is not available
      else -> AvailabilityStatus.NONE
    }
  }

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
  PROBATION_TEAM,
  PROBATION_ANY,
  COURT_ROOM,
  COURT_ANY,
  SHARED,
  NONE,
}
