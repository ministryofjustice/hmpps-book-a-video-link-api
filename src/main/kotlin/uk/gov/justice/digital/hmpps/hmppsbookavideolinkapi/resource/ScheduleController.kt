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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ScheduleService
import java.time.LocalDate

@Tag(name = "Schedule Controller")
@RestController
@RequestMapping(value = ["schedule"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ScheduleController(
  val prisonScheduleService: ScheduleService,
) {

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
  @GetMapping(value = ["/prison/{prisonCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
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
  @GetMapping(value = ["/court/{courtCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
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
  @GetMapping(value = ["/probation/{probationTeamCode}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
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
}
