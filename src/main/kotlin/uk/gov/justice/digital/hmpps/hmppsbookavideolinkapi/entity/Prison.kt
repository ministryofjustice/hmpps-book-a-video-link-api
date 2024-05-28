package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
@Table(name = "prison")
class Prison(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonId: Long = 0,

  val code: String,

  val name: String,

  // Enabled == false means courts/probation cannot self-serve, but we will accept bookings from the prison (via DPS and A&A)
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

    other as Prison

    return prisonId == other.prisonId
  }

  override fun hashCode(): Int {
    return prisonId.hashCode()
  }

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(prisonId = $prisonId)"
  }
}
