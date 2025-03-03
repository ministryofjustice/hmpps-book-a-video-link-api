package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.getBvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.administration.CreateRoomScheduleService
import java.util.UUID

@Tag(name = "Administration Controller")
@RestController
@RequestMapping(value = ["room-admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class RoomAdministrationController(
  private val createRoomScheduleService: CreateRoomScheduleService,
) {

  @Operation(
    summary = "Endpoint to support the creation of a single schedule row for a scheduled room",
    description = "Only BVLS administration users can create a schedule.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The the schedule row has been added to the room's schedule",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Long::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/{dpsLocationId}/schedule"])
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun create(
    @Parameter(description = "The identifier of the DPS location for the scheduled row to be created.")
    @PathVariable(name = "dpsLocationId", required = true)
    dpsLocationId: UUID,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the new schedule details", required = true)
    request: CreateRoomScheduleRequest,
    httpRequest: HttpServletRequest,
  ) {
    createRoomScheduleService.create(dpsLocationId, request, httpRequest.getBvlsRequestContext().user as ExternalUser)
  }
}
