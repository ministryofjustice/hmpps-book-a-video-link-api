package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonerSchedulesService
import java.time.LocalDate
import java.util.UUID

@Tag(name = "Integrations Controller", description = "Prisoner schedules endpoints for integrating with external systems")
@RestController
@RequestMapping(value = ["integrations/schedules"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
@PreAuthorize("hasAnyRole('ROLE_PRISONER_SCHEDULES__RO')")
class PrisonerSchedulesIntegrationsController(private val prisonerSchedulesService: PrisonerSchedulesService) {
  @Operation(
    summary = "Return a paged list of court booking IDs matching the supplied criteria",
    description = "The date range is mandatory and inclusive. All other filters are optional and combined with AND. Only the matching court booking IDs are returned.",
  )
  @GetMapping("/court-bookings/ids")
  @Parameters(
    Parameter(
      name = "page",
      `in` = ParameterIn.QUERY,
      description = "Zero-based page index (0..N)",
      schema = Schema(type = "integer", defaultValue = "0"),
    ),
    Parameter(name = "size", `in` = ParameterIn.QUERY, description = "The size of the page to be returned", schema = Schema(type = "integer", defaultValue = "20")),
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A paginated list of matching court booking IDs",
      ),
    ],
  )
  fun findCourtBookingIds(
    @RequestParam
    @Parameter(description = "The earliest booking date in ISO format (YYYY-MM-DD)", example = "2026-11-01", required = true)
    fromDate: LocalDate,
    @RequestParam
    @Parameter(description = "The latest booking date in ISO format (YYYY-MM-DD)", example = "2026-11-30", required = true)
    toDate: LocalDate,
    @RequestParam(required = false)
    @Parameter(description = "The prison code", example = "MDI")
    prisonCode: String?,
    @RequestParam(required = false)
    @Parameter(description = "The prisoner number", example = "A1234AA")
    prisonerNumber: String?,
    @RequestParam(required = false)
    @Parameter(description = "The DPS location ID", example = "9485cf4a-750b-4d74-b594-59bacbcda247")
    locationId: UUID?,
    @Parameter(hidden = true)
    @PageableDefault(size = 20, page = 0, direction = Sort.Direction.ASC, sort = ["videoBookingId"])
    pageable: Pageable,
  ) = prisonerSchedulesService.findCourtBookingIds(fromDate, toDate, prisonCode, prisonerNumber, locationId, pageable)

  @Operation(
    summary = "Return a paged list of probation booking IDs matching the supplied criteria",
    description = "The date range is mandatory and inclusive. All other filters are optional and combined with AND. Only the matching probation booking IDs are returned.",
  )
  @GetMapping("/probation-bookings/ids")
  @Parameters(
    Parameter(
      name = "page",
      `in` = ParameterIn.QUERY,
      description = "Zero-based page index (0..N)",
      schema = Schema(type = "integer", defaultValue = "0"),
    ),
    Parameter(name = "size", `in` = ParameterIn.QUERY, description = "The size of the page to be returned", schema = Schema(type = "integer", defaultValue = "20")),
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A paginated list of matching probation booking IDs",
      ),
    ],
  )
  fun findProbationBookingIds(
    @RequestParam
    @Parameter(description = "The earliest booking date in ISO format (YYYY-MM-DD)", example = "2026-11-01", required = true)
    fromDate: LocalDate,
    @RequestParam
    @Parameter(description = "The latest booking date in ISO format (YYYY-MM-DD)", example = "2026-11-30", required = true)
    toDate: LocalDate,
    @RequestParam(required = false)
    @Parameter(description = "The prison code", example = "MDI")
    prisonCode: String?,
    @RequestParam(required = false)
    @Parameter(description = "The prisoner number", example = "A1234AA")
    prisonerNumber: String?,
    @RequestParam(required = false)
    @Parameter(description = "The DPS location ID", example = "9485cf4a-750b-4d74-b594-59bacbcda247")
    locationId: UUID?,
    @Parameter(hidden = true)
    @PageableDefault(size = 20, page = 0, direction = Sort.Direction.ASC, sort = ["videoBookingId"])
    pageable: Pageable,
  ) = prisonerSchedulesService.findProbationBookingIds(fromDate, toDate, prisonCode, prisonerNumber, locationId, pageable)
}
