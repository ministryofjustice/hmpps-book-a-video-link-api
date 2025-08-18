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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService

@Tag(name = "Prisons Controller")
@RestController
@RequestMapping(value = ["prisons"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class PrisonsController(
  private val prisonsService: PrisonsService,
  private val locationsService: LocationsService,
) {

  @Operation(summary = "Endpoint to return the list of prisons sorted by name known to the service")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of prisons",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Prison::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/list"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun prisonsList(
    @Parameter(description = "EnabledOnly true or false. Defaults to false if not supplied.")
    @RequestParam(name = "enabledOnly", required = false)
    enabledOnly: Boolean = false,
  ): List<Prison> = prisonsService.getListOfPrisons(enabledOnly)

  @Operation(summary = "Endpoint to return a list of suitable appointment locations sorted by description at a given prison")
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
    ],
  )
  @GetMapping(value = ["/{prisonCode}/locations"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getAppointmentLocationsAtPrison(
    @Parameter(description = "The prison code for which locations will be retrieved.")
    @PathVariable(name = "prisonCode", required = true)
    prisonCode: String,
    @Parameter(description = "Enabled (active) locations only, true or false. Defaults to true if not supplied.")
    @RequestParam(name = "enabledOnly", required = true)
    enabledOnly: Boolean = false,
    @Parameter(description = "Video link only, true or false. When true only returns video link suitable locations. Defaults to true if not supplied.")
    @RequestParam(name = "videoLinkOnly", required = false)
    videoLinkOnly: Boolean = true,
  ): List<Location> = if (videoLinkOnly) {
    locationsService.getVideoLinkLocationsAtPrison(prisonCode, enabledOnly)
  } else {
    locationsService.getNonResidentialLocationsAtPrison(prisonCode, enabledOnly)
  }

  @Operation(summary = "Endpoint to find a prison by its code")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Get a prison by its code",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Prison::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/{prisonCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getPrisonByCode(
    @Parameter(description = "The code of the prison to be returned.")
    @PathVariable("prisonCode", required = true) prisonCode: String,
  ) = prisonsService.getPrison(prisonCode)
}
