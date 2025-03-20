package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PublishEventUtilityModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService

/**
 * These endpoints are secured in the ingress rather than the app so that they can be called from
 * within the namespace without requiring authentication
 */

@Tag(name = "Utility Controller")
@RestController
@ProtectedByIngress
@RequestMapping(value = ["utility"], produces = [MediaType.TEXT_PLAIN_VALUE])
class UtilityController(private val outboundEventsService: OutboundEventsService) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(summary = "Endpoint to publish an event to the domain events SNS topic.")
  @PostMapping(path = ["/publish"])
  @ResponseStatus(HttpStatus.OK)
  fun publishDomainEvent(
    @Valid
    @RequestBody
    publishEventUtilityModel: PublishEventUtilityModel,
  ) = run {
    log.info("UTILITY: publishing domain event ${publishEventUtilityModel.event}")

    publishEventUtilityModel.identifiers!!.forEach { outboundEventsService.send(publishEventUtilityModel.event!!.toDomainEvent(), it) }

    "UTILITY: published domain event ${publishEventUtilityModel.event}"
  }
}
