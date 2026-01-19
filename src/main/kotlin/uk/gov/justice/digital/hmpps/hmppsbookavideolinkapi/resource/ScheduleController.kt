package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.FindCourtBookingsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.FindProbationBookingsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ScheduleService
import java.time.LocalDate

@Tag(name = "Schedule Controller")
@RestController
@RequestMapping(value = ["schedule"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ScheduleController(val prisonScheduleService: ScheduleService) {

  @Operation(summary = "Endpoint to retrieve a schedule of bookings for a prison")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of scheduled video link appointments and booking details at the prison.",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ScheduleItem::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. Message contains the detail.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/prison/{prisonCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getScheduleForPrison(
    @Parameter(description = "A prison code", required = true)
    @PathVariable("prisonCode")
    prisonCode: String,
    @Parameter(description = "A date in ISO format (YYYY-MM-DD). Defaults to today if not supplied.")
    @RequestParam(name = "date")
    date: LocalDate = LocalDate.now(),
    @Parameter(description = "Include cancelled bookings (true or false), defaults to false.")
    @RequestParam(name = "includeCancelled")
    includeCancelled: Boolean = false,
  ) = prisonScheduleService.getScheduleForPrison(prisonCode, date, includeCancelled)

  @Operation(summary = "Endpoint to retrieve a schedule of bookings for a court")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of scheduled video link appointments and booking details for one court",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ScheduleItem::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. Message contains the detail.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/court/{courtCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getCourtSchedule(
    @Parameter(description = "A court code", required = true)
    @PathVariable("courtCode")
    courtCode: String,
    @Parameter(description = "A date in ISO format (YYYY-MM-DD). Defaults to today if not supplied.")
    @RequestParam(name = "date")
    date: LocalDate = LocalDate.now(),
    @Parameter(description = "Include cancelled bookings (true or false), defaults to false.")
    @RequestParam(name = "includeCancelled")
    includeCancelled: Boolean = false,
  ) = prisonScheduleService.getScheduleForCourt(courtCode, date, includeCancelled)

  @Operation(summary = "Endpoint to retrieve a schedule of bookings for a probation team")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "List of scheduled video link appointments and booking details for one probation team",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ScheduleItem::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. Message contains the detail.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/probation/{probationTeamCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getProbationSchedule(
    @Parameter(description = "A probation team code", required = true)
    @PathVariable("probationTeamCode")
    probationTeamCode: String,
    @Parameter(description = "A date in ISO format (YYYY-MM-DD). Defaults to today if not supplied.")
    @RequestParam(name = "date")
    date: LocalDate = LocalDate.now(),
    @Parameter(description = "Include cancelled bookings (true or false), defaults to false.")
    @RequestParam(name = "includeCancelled")
    includeCancelled: Boolean = false,
  ) = prisonScheduleService.getScheduleForProbationTeam(probationTeamCode, date, includeCancelled)

  @Operation(summary = "Endpoint to retrieve a paginated schedule of video link bookings for one or more courts")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A page of scheduled video link bookings for one or more courts",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. Message contains the detail.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/courts/paginated"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getPaginatedScheduleForMultipleCourts(
    @Valid @RequestBody @Parameter(description = "A request to find bookings for a date and list of court codes", required = true)
    request: FindCourtBookingsRequest,
    @Parameter(hidden = true)
    pageable: Pageable = PageRequest.of(0, 10, Sort.by("appointmentDate", "startTime").ascending()),
  ): Page<ScheduleItem> = prisonScheduleService.getScheduleForCourtsPaginated(request.courtCodes!!, request.date, pageable)

  @Operation(summary = "Endpoint to retrieve a paginated schedule of video link bookings for one or more probation teams")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A page of scheduled video link bookings for one or more probation teams",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. Message contains the detail.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/probation-teams/paginated"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getPaginatedScheduleForMultipleProbationTeams(
    @Valid @RequestBody @Parameter(description = "A request to find bookings for a date and list of probation team codes", required = true)
    request: FindProbationBookingsRequest,
    @Parameter(hidden = true)
    pageable: Pageable = PageRequest.of(0, 10, Sort.by("appointmentDate", "startTime").ascending()),
  ): Page<ScheduleItem> = prisonScheduleService.getScheduleForProbationTeamsPaginated(request.probationTeamCodes!!, request.date, pageable)

  @Operation(summary = "Endpoint to retrieve an unpaginated list of video link bookings for one or more courts")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of scheduled video link bookings for one or more courts",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. Message contains the detail.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/courts"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getUnpaginatedScheduleForMultipleCourts(
    @Valid @RequestBody @Parameter(description = "A request to find bookings for a date and list of court codes", required = true)
    request: FindCourtBookingsRequest,
    @Parameter(hidden = true)
    sort: Sort = Sort.by("appointmentDate", "startTime").ascending(),
  ): List<ScheduleItem> = prisonScheduleService.getScheduleForCourtsUnpaginated(request.courtCodes!!, request.date, sort)

  @Operation(summary = "Endpoint to retrieve an unpaginated list of video link bookings for one or more probation teams")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of scheduled video link bookings for one or more probation teams",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request. Message contains the detail.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/probation-teams"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getUnpaginatedScheduleForMultipleProbationTeams(
    @Valid @RequestBody @Parameter(description = "A request to find bookings for a date and list of probation team codes", required = true)
    request: FindProbationBookingsRequest,
    @Parameter(hidden = true)
    sort: Sort = Sort.by("appointmentDate", "startTime").ascending(),
  ): List<ScheduleItem> = prisonScheduleService.getScheduleForProbationTeamsUnpaginated(request.probationTeamCodes!!, request.date, sort)
}
