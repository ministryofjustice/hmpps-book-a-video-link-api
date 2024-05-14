package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ProbationTeam as ModelProbationTeam

@Entity
@Table(name = "probation_team")
class ProbationTeam(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val probationTeamId: Long = 0,

  val code: String,

  val description: String,

  val enabled: Boolean,

  val notes: String?,

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

    other as ProbationTeam

    return probationTeamId == other.probationTeamId
  }

  override fun hashCode(): Int {
    return probationTeamId.hashCode()
  }

  fun toModel() = ModelProbationTeam(
    probationTeamId = probationTeamId,
    code = code,
    description = description,
    enabled = enabled,
    notes = notes,
  )
}

fun List<ProbationTeam>.toModel() = map { it.toModel() }
