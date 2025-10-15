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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.getBvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.administration.DecoratedLocationsService
import java.util.UUID

@Tag(name = "Room Administration Controller")
@RestController
@RequestMapping(value = ["room-admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class RoomAdministrationController(
  private val decoratedLocationsService: DecoratedLocationsService,
  private val locationsService: LocationsService,
) {
  @Operation(summary = "Endpoint to support retrieval of a room including any decorations if there are any.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully returned the requested room.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Location::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The room was not found.",
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
    summary = "Endpoint to support the initial decoration of a room.",
    description = "Only BVLS administration users can create decorated rooms.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created the decorated room.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Location::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The room to be decorated was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
  ) = decoratedLocationsService.decorateLocation(dpsLocationId, request, httpRequest.getBvlsRequestContext().user as ExternalUser)

  @Operation(
    summary = "Endpoint to support amending an already decorated room.",
    description = "Only BVLS administration users can change decorated rooms.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully amended the decorated room.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Location::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The decorated room to be amended was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PutMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/{dpsLocationId}"])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun amendDecoratedRoom(
    @Parameter(description = "The identifier of the DPS location for the scheduled row to be amended.")
    @PathVariable(name = "dpsLocationId", required = true)
    dpsLocationId: UUID,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the decoration details", required = true)
    request: AmendDecoratedRoomRequest,
    httpRequest: HttpServletRequest,
  ) = decoratedLocationsService.amendDecoratedLocation(dpsLocationId, request, httpRequest.getBvlsRequestContext().user as ExternalUser)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(value = ["/{dpsLocationId}"])
  @Operation(summary = "Endpoint to support deletion of a decorated room including any schedules if there are any.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted the decorated room.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun deleteDecoratedLocation(@PathVariable("dpsLocationId") dpsLocationId: UUID) {
    decoratedLocationsService.deleteDecoratedLocation(dpsLocationId)
  }

  @Operation(
    summary = "Endpoint to support the creation of a schedule row for a decorated room schedule.",
    description = "Only BVLS administration users can create a schedule.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully added the schedule row to the room's schedule",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The decorated room was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
  ) = run {
    decoratedLocationsService.createSchedule(dpsLocationId, request, httpRequest.getBvlsRequestContext().user as ExternalUser)
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(value = ["/{dpsLocationId}/schedule/{scheduleId}"])
  @Operation(summary = "Endpoint to support deletion of a schedule row from a room schedule.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Successfully deleted schedule row from the schedule.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun deleteSchedule(
    @PathVariable("dpsLocationId") dpsLocationId: UUID,
    @PathVariable("scheduleId") scheduleId: Long,
    httpRequest: HttpServletRequest,
  ) {
    decoratedLocationsService.deleteSchedule(dpsLocationId, scheduleId, httpRequest.getBvlsRequestContext().user as ExternalUser)
  }

  @Operation(
    summary = "Endpoint to support amending a room schedule.",
    description = "Only BVLS administration users can change room schedules.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully amended the room schedule.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = RoomSchedule::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The room schedule was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PutMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/{dpsLocationId}/schedule/{scheduleId}"])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun amendDecoratedRoom(
    @Parameter(description = "The identifier of the DPS location for the room schedule to be amended.")
    @PathVariable(name = "dpsLocationId", required = true)
    dpsLocationId: UUID,
    @Parameter(description = "The identifier of the room schedule to be amended.")
    @PathVariable(name = "scheduleId", required = true)
    scheduleId: Long,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the decoration details", required = true)
    request: AmendRoomScheduleRequest,
    httpRequest: HttpServletRequest,
  ) = decoratedLocationsService.amendSchedule(dpsLocationId, scheduleId, request, httpRequest.getBvlsRequestContext().user as ExternalUser)
}
