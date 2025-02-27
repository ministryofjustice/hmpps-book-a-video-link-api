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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isBetween
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "location_attribute")
class LocationAttribute(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val locationAttributeId: Long = 0,

  val dpsLocationId: UUID,

  @OneToOne
  @JoinColumn(name = "prison_id")
  val prison: Prison,

  @Enumerated(EnumType.STRING)
  val locationStatus: LocationStatus = LocationStatus.ACTIVE,

  val statusMessage: String? = null,

  val expectedActiveDate: LocalDate? = null,

  @Enumerated(EnumType.STRING)
  val locationUsage: LocationUsage,

  val allowedParties: String? = null,

  val prisonVideoUrl: String? = null,

  val notes: String? = null,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) {
  var amendedBy: String? = null
    private set

  var amendedTime: LocalDateTime? = null
    private set

  @OneToMany(mappedBy = "locationAttribute", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  private val locationSchedule: MutableList<LocationSchedule> = mutableListOf()

  fun setLocationSchedule(schedules: List<LocationSchedule>) {
    require(locationUsage == LocationUsage.SCHEDULE) {
      "The location usage type must be SCHEDULE for a list of schedule rows to be associated with it."
    }
    this.locationSchedule.addAll(schedules)
  }

  fun schedule() = locationSchedule.toList()

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

  fun isAvailableFor(court: Court, startingOnDateTime: LocalDateTime): AvailabilityStatus = check(court, startingOnDateTime)

  private fun check(probationTeam: ProbationTeam, startingOnDateTime: LocalDateTime): AvailabilityStatus {
    if (locationStatus == LocationStatus.INACTIVE) {
      return AvailabilityStatus.NONE
    }

    return when (locationUsage) {
      LocationUsage.SHARED -> AvailabilityStatus.SHARED
      LocationUsage.PROBATION -> {
        if (allowedParties == null) {
          AvailabilityStatus.PROBATION_ANY
        } else {
          if (isPartyAllowed(probationTeam.code)) {
            AvailabilityStatus.PROBATION_TEAM
          } else {
            AvailabilityStatus.NONE
          }
        }
      }
      LocationUsage.SCHEDULE -> locationSchedule.check(probationTeam, startingOnDateTime)
      else -> return AvailabilityStatus.NONE
    }
  }

  private fun List<LocationSchedule>.check(probationTeam: ProbationTeam, startingOnDateTime: LocalDateTime): AvailabilityStatus = filter {
    startingOnDateTime.dayOfWeek.between(it.startDayOfWeek, it.endDayOfWeek) && startingOnDateTime.toLocalTime().isBetween(it.startTime, it.endTime)
  }
    .let {
      if (it.isEmpty()) {
        return AvailabilityStatus.SHARED
      } else {
        if (any { schedule ->
            schedule.isAvailableForProbationTeam(
              probationTeam,
              startingOnDateTime,
            )
          }
        ) {
          return AvailabilityStatus.PROBATION_TEAM
        }
        if (any { schedule -> schedule.isAvailableForAnyProbationTeam(startingOnDateTime) }) return AvailabilityStatus.PROBATION_ANY
        return AvailabilityStatus.NONE
      }
    }

  private fun check(court: Court, startingOnDateTime: LocalDateTime): AvailabilityStatus {
    if (locationStatus == LocationStatus.INACTIVE) {
      return AvailabilityStatus.NONE
    }

    return when (locationUsage) {
      LocationUsage.SHARED -> AvailabilityStatus.SHARED
      LocationUsage.COURT -> {
        if (allowedParties == null) {
          AvailabilityStatus.COURT_ANY
        } else {
          if (isPartyAllowed(court.code)) {
            AvailabilityStatus.COURT_ROOM
          } else {
            AvailabilityStatus.NONE
          }
        }
      }
      LocationUsage.SCHEDULE -> TODO()
      else -> return AvailabilityStatus.NONE
    }
  }

  private fun isPartyAllowed(party: String) = allowedParties.orEmpty().replace(" ", "").split(",").contains(party)
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
