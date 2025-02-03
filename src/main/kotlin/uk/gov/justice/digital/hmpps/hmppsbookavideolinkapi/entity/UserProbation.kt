package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
@Table(name = "user_probation")
class UserProbation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val userProbationId: Long = 0,

  @JoinColumn(name = "probation_team_id")
  @ManyToOne(fetch = FetchType.LAZY)
  val probationTeam: ProbationTeam,

  val username: String,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as UserProbation

    return userProbationId == other.userProbationId
  }

  override fun hashCode(): Int = userProbationId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(userProbationId = $userProbationId)"
}
