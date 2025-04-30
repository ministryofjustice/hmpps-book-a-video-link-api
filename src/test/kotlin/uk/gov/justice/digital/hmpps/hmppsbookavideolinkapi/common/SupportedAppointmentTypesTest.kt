package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo

class SupportedAppointmentTypesTest {
  private val supportedAppointmentTypes = SupportedAppointmentTypes()

  @Test
  fun `should match on appointment types`() {
    SupportedAppointmentTypes.Type.COURT.code isEqualTo "VLB"
    SupportedAppointmentTypes.Type.PROBATION.code isEqualTo "VLPM"
  }

  @Test
  fun `should support VLB appointment type`() {
    supportedAppointmentTypes.isSupported("VLB") isBool true
  }

  @Test
  fun `should support VLPM appointment type`() {
    supportedAppointmentTypes.isSupported("VLPM") isBool true
  }

  @Test
  fun `should not support UNKOWN appointment type`() {
    supportedAppointmentTypes.isSupported("UNKOWN") isBool false
  }

  @Test
  fun `should support court and probation appointment type`() {
    supportedAppointmentTypes.typeOf(BookingType.COURT) isEqualTo SupportedAppointmentTypes.Type.COURT
    supportedAppointmentTypes.typeOf(BookingType.PROBATION) isEqualTo SupportedAppointmentTypes.Type.PROBATION
  }
}
