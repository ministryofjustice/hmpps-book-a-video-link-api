package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import java.time.LocalTime

class LocalTimeUtilsTest {

  @Test
  fun `should not be overlapping times`() {
    isTimesOverlap(time(9, 0), time(10, 0), time(11, 0), time(12, 0)) isBool false
    isTimesOverlap(time(9, 0), time(11, 0), time(11, 0), time(12, 0)) isBool false
    isTimesOverlap(time(11, 0), time(12, 0), time(9, 0), time(11, 0)) isBool false
  }

  @Test
  fun `should be overlapping times`() {
    isTimesOverlap(time(9, 0), time(10, 0), time(9, 59), time(12, 0)) isBool true
    isTimesOverlap(time(9, 59), time(12, 0), time(9, 0), time(10, 0)) isBool true
    isTimesOverlap(time(9, 0), time(10, 0), time(8, 30), time(12, 0)) isBool true
    isTimesOverlap(time(8, 30), time(12, 0), time(9, 0), time(10, 0)) isBool true
  }

  private fun time(hour: Int, min: Int) = LocalTime.of(hour, min)
}
