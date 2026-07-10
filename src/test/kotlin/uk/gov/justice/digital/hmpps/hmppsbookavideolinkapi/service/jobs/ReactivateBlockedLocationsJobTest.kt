package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.administration.DecoratedLocationsService
import java.time.LocalDateTime

class ReactivateBlockedLocationsJobTest {
  private val decoratedLocationsService: DecoratedLocationsService = mock()
  private val timeSource: TimeSource = TimeSource { LocalDateTime.of(2026, 7, 10, 12, 0) }

  private val job = ReactivateBlockedLocationsJob(decoratedLocationsService, timeSource)

  @Test
  fun `should call the decorated locations service when run`() {
    job.runJob()

    verify(decoratedLocationsService).reactivateBlockedLocationsBefore(LocalDateTime.of(2026, 7, 10, 12, 0))
  }
}
