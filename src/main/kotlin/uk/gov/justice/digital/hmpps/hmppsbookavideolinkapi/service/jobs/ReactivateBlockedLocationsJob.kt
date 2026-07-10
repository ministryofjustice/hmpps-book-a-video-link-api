package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.administration.DecoratedLocationsService

@Component
class ReactivateBlockedLocationsJob(
  private val decoratedLocationsService: DecoratedLocationsService,
  private val timeSource: TimeSource,
) : JobDefinition(
  JobType.REACTIVATE_BLOCKED_LOCATIONS,
  block = { decoratedLocationsService.reactivateBlockedLocationsBefore(timeSource.now()) },
)
