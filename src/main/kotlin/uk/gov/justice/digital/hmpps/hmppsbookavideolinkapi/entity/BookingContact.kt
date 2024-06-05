package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable
import java.io.Serializable

@Entity
@Immutable
@Table(name = "v_booking_contacts")
@IdClass(UniquePropertyId::class)
data class BookingContact(
  val videoBookingId: Long,

  @Id
  val contactType: String,

  val name: String?,

  val position: String?,

  @Id
  val email: String?,

  val telephone: String?,

  val primaryContact: Boolean,
)

data class UniquePropertyId(val contactType: String, val email: String?) : Serializable {
  constructor() : this("", "")

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as UniquePropertyId
    if (contactType != other.contactType) return false
    return email == other.email
  }

  override fun hashCode(): Int {
    return contactType.hashCode() * 31 + email.hashCode()
  }
}
