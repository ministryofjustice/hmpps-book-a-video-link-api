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

  // Read only is used for the UNKNOWN court code used to migrate legacy bookings. Courts with this flag will not be allowed for new bookings.
  val readOnly: Boolean = false,

  val notes: String?,

  val createdBy: String,

  val createdTime: LocalDateTime = LocalDateTime.now(),
) : LocationBookingType {
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

  fun isUnknown() = code == UNKNOWN_COURT_CODE

  fun isReadable() = !readOnly

  override fun hashCode(): Int = courtId.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(courtId = $courtId, code = $code)"
}
