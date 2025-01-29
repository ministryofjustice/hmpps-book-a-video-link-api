package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo

class SupportedAppointmentTypesTest {
  private val courtBookingType = BookingType { true }
  private val probationBookingType = BookingType { false }
  private val featureSwitches: FeatureSwitches = mock()
  private val supportedAppointmentTypes = SupportedAppointmentTypes(featureSwitches)

  @Test
  fun `should match on appointment types`() {
    SupportedAppointmentTypes.Type.COURT.code isEqualTo "VLB"
    SupportedAppointmentTypes.Type.PROBATION.code isEqualTo "VLPM"
  }

  @Nested
  @DisplayName("VLPM feature toggle off")
  inner class ToggleOff {
    @BeforeEach
    fun before() {
      whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn false
    }

    @Test
    fun `should support VLB appointment type`() {
      supportedAppointmentTypes.isSupported("VLB") isBool true
    }

    @Test
    fun `should not support VLPM appointment type`() {
      supportedAppointmentTypes.isSupported("VLPM") isBool false
    }

    @Test
    fun `should support court appointment type only`() {
      supportedAppointmentTypes.typeOf(courtBookingType) isEqualTo SupportedAppointmentTypes.Type.COURT
      supportedAppointmentTypes.typeOf(probationBookingType) isEqualTo SupportedAppointmentTypes.Type.COURT
    }
  }

  @Nested
  @DisplayName("VLPM feature toggle on")
  inner class ToggleOn {
    @BeforeEach
    fun before() {
      whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn true
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
    fun `should support court and probation appointment type`() {
      supportedAppointmentTypes.typeOf(courtBookingType) isEqualTo SupportedAppointmentTypes.Type.COURT
      supportedAppointmentTypes.typeOf(probationBookingType) isEqualTo SupportedAppointmentTypes.Type.PROBATION
    }
  }
}
