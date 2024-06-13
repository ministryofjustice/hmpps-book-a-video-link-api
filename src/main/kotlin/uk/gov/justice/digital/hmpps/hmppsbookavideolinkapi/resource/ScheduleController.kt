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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonScheduleService
import java.time.LocalDate

@Tag(name = "Schedule Controller")
@RestController
@RequestMapping(value = ["schedule"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ScheduleController(
  val prisonScheduleService: PrisonScheduleService,
) {

  @Operation(summary = "Endpoint to retrieve a schedule of bookings for a prison on one date")
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
  @GetMapping
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun getSchedule(
    @Parameter(description = "The date to retrieve the prison schedule for", required = true)
    @RequestParam(name = "date", required = true) date: LocalDate,
    @Parameter(description = "The prison code to obtain the schedule for", required = true)
    @RequestParam(name = "prisonCode", required = true) prisonCode: String,
  ) = prisonScheduleService.getScheduleIncludingCancelled(date, prisonCode)
}
