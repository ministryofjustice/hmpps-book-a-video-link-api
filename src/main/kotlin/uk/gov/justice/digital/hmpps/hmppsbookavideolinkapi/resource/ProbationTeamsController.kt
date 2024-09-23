package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.getBvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.SetProbationTeamPreferencesRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.SetProbationTeamPreferencesResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ProbationTeamsService

@Tag(name = "Probation Teams Controller")
@RestController
@RequestMapping(value = ["probation-teams"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class ProbationTeamsController(private val probationTeamsService: ProbationTeamsService) {

  @Operation(summary = "Endpoint to return a list of probation teams for video link bookings")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Probation teams",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ProbationTeam::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getProbationTeams(
    @Parameter(description = "Enabled only, true or false. When true only returns enabled probation teams. Defaults to true if not supplied.")
    @RequestParam(name = "enabledOnly", required = false)
    enabledOnly: Boolean = true,
  ): List<ProbationTeam> = probationTeamsService.getProbationTeams(enabledOnly)

  @Operation(summary = "Endpoint to return the list of probation teams select by a user (identified from the token content)")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Probation teams select by this user",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = ProbationTeam::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/user-preferences"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun probationTeamsUserPreferences(
    httpRequest: HttpServletRequest,
  ) = probationTeamsService.getUserProbationTeamPreferences(httpRequest.getBvlsRequestContext().user)

  @Operation(summary = "Endpoint to set the probation team preferences for a user (identified from the token content)")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Count of the number of probation teams saved in this request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SetProbationTeamPreferencesResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request provided",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(
    value = ["/user-preferences/set"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun setUserProbationTeamPreferences(
    @Valid
    @RequestBody
    @Parameter(description = "The request body containing the user probation team preferences", required = true)
    request: SetProbationTeamPreferencesRequest,
    httpRequest: HttpServletRequest,
  ) = probationTeamsService.setUserProbationTeamPreferences(request, httpRequest.getBvlsRequestContext().user)
}
