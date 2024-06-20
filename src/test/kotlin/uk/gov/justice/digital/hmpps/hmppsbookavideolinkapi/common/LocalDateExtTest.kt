package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import java.time.LocalDate

class LocalDateExtTest {

  @Test
  fun `should format date to ISO date style`() {
    LocalDate.of(2024, 6, 20).toIsoDate() isEqualTo "2024-06-20"
    LocalDate.EPOCH.toIsoDate() isEqualTo "1970-01-01"
  }
}
