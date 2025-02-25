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
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "location_schedule")
class LocationSchedule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val locationScheduleId: Long = 0,

  @JoinColumn(name = "location_attribute_id")
  @ManyToOne(fetch = FetchType.LAZY)
  val locationAttribute: LocationAttribute,

  val startDayOfWeek: Int,

  val endDayOfWeek: Int,

  val startTime: LocalTime,

  val endTime: LocalTime,

  @Enumerated(EnumType.STRING)
  val locationUsage: LocationUsage,

  val allowedParties: String? = null,

  val notes: String? = null,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) {
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

  // TODO
  fun isAvailableFor(probationTeam: ProbationTeam, onDateTime: LocalDateTime) = true

  // TODO
  fun isAvailableFor(court: Court, onDateTime: LocalDateTime) = true
}
