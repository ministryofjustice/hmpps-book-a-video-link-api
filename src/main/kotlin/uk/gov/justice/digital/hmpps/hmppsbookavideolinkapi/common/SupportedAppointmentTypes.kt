package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType

@Component
class SupportedAppointmentTypes(private val featureSwitches: FeatureSwitches) {

  enum class Type(val code: String) {
    COURT("VLB"),
    PROBATION("VLPM"),
  }

  fun typeOf(type: BookingType) =
    when (featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) {
      true -> Type.COURT.takeIf { type.isCourtBooking() } ?: Type.PROBATION
      false -> Type.COURT
    }

  fun isSupported(appointmentType: String) =
    when (featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) {
      true -> Type.entries.map(Type::code).contains(appointmentType)
      false -> Type.COURT.code == appointmentType
    }
}
