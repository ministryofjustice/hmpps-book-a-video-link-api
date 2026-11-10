package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison

import jakarta.validation.ValidationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.inactiveBirminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import java.util.UUID

class LocationValidatorTest {

  private val client: LocationsInsidePrisonClient = mock()
  private val validator = LocationValidator(client)

  @Test
  fun `should pass validation when location found`() {
    whenever(client.getLocationById(birminghamLocation.id)) doReturn birminghamLocation

    assertDoesNotThrow {
      validator.validatePrisonLocationId(BIRMINGHAM, birminghamLocation.id)
    }
  }

  @Test
  fun `should fail validation when single location not found`() {
    val unknownLocationId = UUID.randomUUID()
    whenever(client.getLocationById(unknownLocationId)) doReturn null

    val error = assertThrows<ValidationException> { validator.validatePrisonLocationId(BIRMINGHAM, unknownLocationId) }

    error.message isEqualTo "The following location was not found [$unknownLocationId]"
  }

  @Test
  fun `should fail validation when multiple locations not found`() {
    val unknownLocationId = UUID.randomUUID()
    whenever(client.getLocationById(unknownLocationId)) doReturn null

    val unknownLocationId2 = UUID.randomUUID()
    whenever(client.getLocationById(unknownLocationId2)) doReturn null

    whenever(client.getLocationById(unknownLocationId)) doReturn null
    whenever(client.getLocationById(unknownLocationId2)) doReturn null

    val error = assertThrows<ValidationException> { validator.validatePrisonLocationIds(BIRMINGHAM, setOf(unknownLocationId, unknownLocationId2)) }

    error.message isEqualTo "The following locations were not found [$unknownLocationId, $unknownLocationId2]"
  }

  @Test
  fun `should fail validation when location not at chosen prison`() {
    whenever(client.getLocationById(birminghamLocation.id)) doReturn birminghamLocation
    whenever(client.getLocationById(wandsworthLocation.id)) doReturn wandsworthLocation

    val error = assertThrows<ValidationException> { validator.validatePrisonLocationIds(BIRMINGHAM, setOf(birminghamLocation.id, wandsworthLocation.id)) }

    error.message isEqualTo "The following location is not at prison code $BIRMINGHAM [${wandsworthLocation.id}]"
  }

  @Test
  fun `should fail validation when locations not at chosen prison`() {
    whenever(client.getLocationById(birminghamLocation.id)) doReturn birminghamLocation
    whenever(client.getLocationById(wandsworthLocation.id)) doReturn wandsworthLocation

    val error = assertThrows<ValidationException> { validator.validatePrisonLocationIds(RISLEY, setOf(birminghamLocation.id, wandsworthLocation.id)) }

    error.message isEqualTo "The following locations are not at prison code $RISLEY [${birminghamLocation.id}, ${wandsworthLocation.id}]"
  }

  @Test
  fun `should fail validation when location at chosen prison is inactive`() {
    whenever(client.getLocationById(inactiveBirminghamLocation.id)) doReturn inactiveBirminghamLocation

    val error = assertThrows<ValidationException> { validator.validatePrisonLocationIds(BIRMINGHAM, setOf(inactiveBirminghamLocation.id)) }

    error.message isEqualTo "The following location is not active at prison code $BIRMINGHAM [${inactiveBirminghamLocation.id}]"
  }
}
