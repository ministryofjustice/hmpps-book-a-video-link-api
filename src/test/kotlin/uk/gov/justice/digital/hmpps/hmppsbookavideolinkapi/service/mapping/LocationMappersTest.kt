package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location as ModelLocation

class LocationMappersTest {

  @Test
  fun `should map active location to model representation`() {
    val location = location(PENTONVILLE, "SUFFIX", true, "PENTONVILLE    ROOM A1")

    location.toModel() isEqualTo ModelLocation("PVI-SUFFIX", "Pentonville Room A1", true)
  }

  @Test
  fun `should map inactive location to model representation`() {
    val location = location(RISLEY, "SUFFIX", false, "RISLEY ROOM    B1")

    location.toModel() isEqualTo ModelLocation("RSI-SUFFIX", "Risley Room B1", false)
  }
}