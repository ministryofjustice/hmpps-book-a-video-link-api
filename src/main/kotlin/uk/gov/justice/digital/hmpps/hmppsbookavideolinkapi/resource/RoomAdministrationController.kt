package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.getBvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.administration.CreateDecoratedRoomService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.administration.CreateRoomScheduleService
import java.util.UUID

@Tag(name = "Room Administration Controller")
@RestController
@RequestMapping(value = ["room-admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class RoomAdministrationController(
  private val createDecoratedRoomService: CreateDecoratedRoomService,
  private val createRoomScheduleService: CreateRoomScheduleService,
  private val locationsService: LocationsService,
) {

  @Operation(summary = "Endpoint to return the details of a location")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The returned location will include any decorations where present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Location::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The DPS location ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/{dpsLocationId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun getLocationById(
    @PathVariable("dpsLocationId") dpsLocationId: UUID,
  ) = locationsService.getLocationById(dpsLocationId) ?: throw EntityNotFoundException("DPS location with ID $dpsLocationId not found.")

  @Operation(
    summary = "Endpoint to support the creation of a decorated room.",
    description = "Only BVLS administration users can create decorated rooms.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The the decorated room has been created",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Location::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/{dpsLocationId}"])
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun createDecoratedRoom(
    @Parameter(description = "The identifier of the DPS location for the scheduled row to be created.")
    @PathVariable(name = "dpsLocationId", required = true)
    dpsLocationId: UUID,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the new decoration details", required = true)
    request: CreateDecoratedRoomRequest,
    httpRequest: HttpServletRequest,
  ) = createDecoratedRoomService.create(dpsLocationId, request, httpRequest.getBvlsRequestContext().user as ExternalUser)

  @Operation(
    summary = "Endpoint to support the creation of a single schedule row for a scheduled room",
    description = "Only BVLS administration users can create a schedule.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The the schedule row has been added to the room's schedule",
      ),
    ],
  )
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/{dpsLocationId}/schedule"])
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun createSchedule(
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
