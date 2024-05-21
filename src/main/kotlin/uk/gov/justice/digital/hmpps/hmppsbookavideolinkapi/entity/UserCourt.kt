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
@Table(name = "user_court")
class UserCourt(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val userCourtId: Long = 0,

  @JoinColumn(name = "court_id")
  @ManyToOne(fetch = FetchType.LAZY)
  val court: Court,

  val username: String,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as UserCourt

    return userCourtId == other.userCourtId
  }

  override fun hashCode(): Int {
    return userCourtId.hashCode()
  }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(userCourtId = $userCourtId)"
  }
}
