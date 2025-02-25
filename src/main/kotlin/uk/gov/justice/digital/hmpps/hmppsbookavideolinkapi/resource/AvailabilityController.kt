package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailableLocationsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailabilityResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AvailabilityService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AvailableLocationsService

@Tag(name = "Availability Controller")
@RestController
@RequestMapping(value = ["availability"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class AvailabilityController(
  private val availabilityService: AvailabilityService,
  private val availableLocationsService: AvailableLocationsService,
) {

  @Operation(summary = "Endpoint to assess booking availability and to suggest alternatives")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Availability response, including any suggested alternative booking options",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AvailabilityResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun checkAvailability(
    @Valid
    @RequestBody
    @Parameter(description = "The request containing the times and locations of hearings to check for availability", required = true)
    request: AvailabilityRequest,
  ) = availabilityService.checkAvailability(request)

  @Operation(summary = "Endpoint to provide locations which are available for booking at time of request")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Available locations response, including available locations for given criteria",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AvailableLocationsResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/locations"])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun availableLocations(
    @Valid
    @RequestBody
    @Parameter(
      description = "The request containing the criteria for looking up available locations",
      required = true,
    )
    request: AvailableLocationsRequest,
  ): AvailableLocationsResponse = availableLocationsService.findAvailableLocations(request)
}
