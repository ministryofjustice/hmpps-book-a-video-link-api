package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

const val UNKNOWN_COURT_CODE = "UNKNOWN"

@Entity
@Table(name = "court")
class Court(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val courtId: Long = 0,

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

    other as Court

    return courtId == other.courtId && code == other.code
  }

  override fun hashCode(): Int {
    return courtId.hashCode()
  }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(courtId = $courtId)"
  }
}
