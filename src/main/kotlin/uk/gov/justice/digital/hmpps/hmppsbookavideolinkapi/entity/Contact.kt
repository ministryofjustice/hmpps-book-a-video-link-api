package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import java.io.Serializable

@Entity
@Immutable
@Table(name = "v_all_contacts")
@IdClass(UniquePropertyId::class)
data class Contact(
  @Id
  @Enumerated(EnumType.STRING)
  val contactType: ContactType,

  val code: String,

  val name: String?,

  val position: String? = null,

  @Id
  val email: String?,

  val telephone: String? = null,

  val primaryContact: Boolean,
)

data class UniquePropertyId(val contactType: ContactType?, val email: String?) : Serializable {
  constructor() : this(null, null)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as UniquePropertyId
    if (contactType != other.contactType) return false
    return email == other.email
  }

  override fun hashCode(): Int = contactType.hashCode() * 31 + email.hashCode()
}

enum class ContactType {
  USER,
  COURT,
  PROBATION,
  PRISON,
  THIRD_PARTY,
}
