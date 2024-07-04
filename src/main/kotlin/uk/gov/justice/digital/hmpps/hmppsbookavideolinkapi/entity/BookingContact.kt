package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.springframework.data.annotation.Immutable

@Entity
@Immutable
@Table(name = "v_booking_contacts")
@IdClass(UniquePropertyId::class)
data class BookingContact(
  val videoBookingId: Long,

  @Id
  @Enumerated(EnumType.STRING)
  val contactType: ContactType,

  val name: String?,

  val position: String? = null,

  @Id
  val email: String?,

  val telephone: String? = null,

  val primaryContact: Boolean,
)
