package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import java.time.LocalTime

class LocalTimeExtTest {

  @Test
  fun `should format time to minute precision style`() {
    LocalTime.of(1, 1, 1, 1).toHourMinuteStyle() isEqualTo "01:01"
    LocalTime.of(12, 2, 2, 2).toHourMinuteStyle() isEqualTo "12:02"
    LocalTime.of(13, 3, 3, 3).toHourMinuteStyle() isEqualTo "13:03"
  }
}
