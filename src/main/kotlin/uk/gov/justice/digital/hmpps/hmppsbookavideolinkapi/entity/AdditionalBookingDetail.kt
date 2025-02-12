package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isUkPhoneNumber
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.requireNot

@Entity
class AdditionalBookingDetail private constructor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val additionalBookingDetailId: Long = 0,

  @OneToOne
  @JoinColumn(name = "video_booking_id")
  val videoBooking: VideoBooking,

  name: String,

  email: String?,

  phoneNumber: String?,

  information: String?,
) {
  var contactName: String = ""
    set(value) {
      require(value.isNotBlank()) {
        "Contact name cannot be blank"
      }

      field = value
    }

  var contactEmail: String? = ""
    set(value) {
      require(value == null || value.isNotBlank()) {
        "Contact email cannot be blank"
      }

      require(value == null || value.isEmail()) {
        "Contact email is not a valid email address"
      }

      field = value
    }

  var contactNumber: String? = null
    set(value) {
      require(value == null || value.isUkPhoneNumber()) {
        "Contact number is not a valid UK phone number"
      }

      field = value
    }

  var extraInformation: String? = information
    set(value) {
      require(value == null || value.isNotBlank()) {
        "Extra information cannot be blank"
      }

      field = value
    }

  init {
    contactName = name
    contactEmail = email
    contactNumber = phoneNumber
    extraInformation = information

    requireNot(email == null && phoneNumber == null) {
      "Must provide at least one, contact email or contact phone number"
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as AdditionalBookingDetail

    return additionalBookingDetailId == other.additionalBookingDetailId
  }

  override fun hashCode(): Int = additionalBookingDetailId.hashCode()

  companion object {
    fun newDetails(
      videoBooking: VideoBooking,
      contactName: String,
      contactEmail: String?,
      contactPhoneNumber: String?,
      extraInformation: String?,
    ) = AdditionalBookingDetail(
      videoBooking = videoBooking,
      name = contactName,
      email = contactEmail,
      phoneNumber = contactPhoneNumber,
      information = extraInformation,
    )
  }
}
