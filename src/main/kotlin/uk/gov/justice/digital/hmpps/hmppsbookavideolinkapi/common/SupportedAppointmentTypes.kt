package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType

@Component
class SupportedAppointmentTypes {

  enum class Type(val code: String) {
    COURT("VLB"),
    PROBATION("VLPM"),
  }

  fun typeOf(type: BookingType) = when (type) {
    BookingType.COURT -> Type.COURT
    BookingType.PROBATION -> Type.PROBATION
  }

  fun isSupported(appointmentType: String) = Type.entries.map(Type::code).contains(appointmentType)
}
