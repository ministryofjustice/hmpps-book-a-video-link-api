package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.administration.DecoratedLocationsService
import java.time.LocalDate

@Component
class ReactivateBlockedLocationsJob(
  decoratedLocationsService: DecoratedLocationsService,
) : JobDefinition(
  JobType.REACTIVATE_BLOCKED_LOCATIONS,
  block = { decoratedLocationsService.reactivateBlockedLocationsBefore(LocalDate.now()) },
)
