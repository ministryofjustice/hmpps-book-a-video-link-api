package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.LocationsService

@Tag(name = "Locations Controller")
@RestController
@RequestMapping(value = ["locations"], produces = [MediaType.APPLICATION_JSON_VALUE])
class LocationsController(private val locationsService: LocationsService) {

  @Operation(summary = "Endpoint to return a list of locations for a prison")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Locations",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Location::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun prisonLocations(
    @Parameter(description = "The prison code for which locations will be retrieved.")
    @RequestParam(name = "prisonCode", required = true)
    prisonCode: String,
    @Parameter(description = "Enabled only, true or false. Defaults to false if not supplied.")
    @RequestParam(name = "enabledOnly", required = false)
    enabledOnly: Boolean = false,
  ): List<Location> = locationsService.getLocationsAtPrison(prisonCode, enabledOnly)
}
