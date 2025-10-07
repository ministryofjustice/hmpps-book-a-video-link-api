package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

const val UNKNOWN_PROBATION_TEAM_CODE = "UNKNOWN"

@Entity
@Table(name = "probation_team")
class ProbationTeam(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val probationTeamId: Long = 0,

  val code: String,

  val description: String,

  val enabled: Boolean,

  // Read only is used for the UNKNOWN probation team code used to migrate legacy bookings. Probation teams with this flag will not be allowed for new bookings.
  val readOnly: Boolean = false,

  val notes: String?,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) : LocationBookingType {
  var amendedBy: String? = null
    private set

  var amendedTime: LocalDateTime? = null
    private set

  fun isUnknown() = code == UNKNOWN_PROBATION_TEAM_CODE

  fun isReadable() = !readOnly

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as ProbationTeam

    return probationTeamId == other.probationTeamId && code == other.code
  }

  override fun hashCode(): Int = probationTeamId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(probationTeamId = $probationTeamId)"
}
