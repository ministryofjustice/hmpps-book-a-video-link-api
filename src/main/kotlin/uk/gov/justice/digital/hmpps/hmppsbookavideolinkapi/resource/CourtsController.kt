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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.getBvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.SetCourtPreferencesRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.SetCourtPreferencesResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CourtsService

@Tag(name = "Courts Controller")
@RestController
@RequestMapping(value = ["courts"], produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtsController(private val courtsService: CourtsService) {

  @Operation(summary = "Endpoint to return a list of enabled courts for video link bookings")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Courts",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Court::class)),
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
  @GetMapping(value = ["/enabled"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun enabledCourts(): List<Court> = courtsService.getEnabledCourts()

  @Operation(summary = "Endpoint to return the list of enabled courts selected by a user (identified from the token content)")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Courts selected by this user",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Court::class)),
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
  @GetMapping(value = ["/user-preferences"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun getUserCourtPreferences(
    httpRequest: HttpServletRequest,
  ) = courtsService.getUserCourtPreferences(httpRequest.getBvlsRequestContext().username)

  @Operation(summary = "Endpoint to set the court preferences for a user (identified from the token content)")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Count of the number of courts saved in this request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = SetCourtPreferencesResponse::class),
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
  @PostMapping(
    value = ["/user-preferences/set"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun setUserCourtPreferences(
    @Valid
    @RequestBody
    @Parameter(description = "The request body containing the user court preferences", required = true)
    request: SetCourtPreferencesRequest,
    httpRequest: HttpServletRequest,
  ) = courtsService.setUserCourtPreferences(request, httpRequest.getBvlsRequestContext().username)
}
