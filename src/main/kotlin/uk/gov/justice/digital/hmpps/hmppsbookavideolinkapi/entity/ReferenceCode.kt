package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
@Table(name = "reference_code")
class ReferenceCode(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val referenceCodeId: Long = 0,

  val groupCode: String,

  val code: String,

  val description: String,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),

  val enabled: Boolean,
) {
  var amendedBy: String? = null
    private set

  var amendedTime: LocalDateTime? = null
    private set

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as ReferenceCode

    return referenceCodeId == other.referenceCodeId
  }

  override fun hashCode(): Int = referenceCodeId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(referenceCodeId = $referenceCodeId)"
}
