package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location as ModelLocation

class LocationMappersTest {

  @Test
  fun `should map active location to model representation`() {
    val idToCheck: UUID = UUID.randomUUID()
    val location = location(PENTONVILLE, "SUFFIX", true, "VCC PENTONVILLE ROOM A1", idToCheck)

    location.toModel() isEqualTo ModelLocation(prisonCode = PENTONVILLE, key = "PVI-SUFFIX", description = "VCC PENTONVILLE ROOM A1", enabled = true, dpsLocationId = idToCheck)
  }

  @Test
  fun `should map inactive location to model representation`() {
    val idToCheck: UUID = UUID.randomUUID()
    val location = location(RISLEY, "SUFFIX", false, "PCVL RISLEY ROOM B1", idToCheck)

    location.toModel() isEqualTo ModelLocation(prisonCode = RISLEY, key = "RSI-SUFFIX", description = "PCVL RISLEY ROOM B1", enabled = false, dpsLocationId = idToCheck)
  }
}
