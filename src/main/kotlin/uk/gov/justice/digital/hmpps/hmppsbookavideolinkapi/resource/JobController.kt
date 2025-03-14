package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PublishEventUtilityModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobTriggerService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType

/**
 * These endpoints are secured in the ingress rather than the app so that they can be called from
 * within the namespace without requiring authentication
 */

@Tag(name = "Job Controller")
@RestController
@ProtectedByIngress
@RequestMapping(value = ["job-admin"], produces = [MediaType.TEXT_PLAIN_VALUE])
class JobController(
  private val jobTriggerService: JobTriggerService,
  private val outboundEventsService: OutboundEventsService,
) {
  @Operation(summary = "Endpoint to trigger a job, perhaps from a cron schedule.")
  @PostMapping(path = ["/run/{jobName}"])
  @ResponseStatus(HttpStatus.OK)
  fun runJob(@PathVariable("jobName") jobName: JobType) = jobTriggerService.run(jobName).resultMessage

  @Operation(summary = "Endpoint to publish an event to the domain events SNS topic.")
  @PostMapping(path = ["/publish/{domainEventType}"])
  @ResponseStatus(HttpStatus.OK)
  fun publishDomainEvent(
    @PathVariable("domainEventType") domainEventType: DomainEventType,
    @Valid
    @RequestBody
    publishEventUtilityModel: PublishEventUtilityModel,
  ) = publishEventUtilityModel.identifiers!!.forEach{ outboundEventsService.send(domainEventType, it) }
}
