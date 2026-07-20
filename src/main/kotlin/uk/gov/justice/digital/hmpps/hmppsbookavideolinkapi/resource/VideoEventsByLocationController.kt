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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoEventRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.VideoEventsByLocationService

@Tag(name = "Video events by location controller")
@RestController
@RequestMapping(value = ["video-events"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class VideoEventsByLocationController(val videoEventsByLocationService: VideoEventsByLocationService) {

  @Operation(summary = "Endpoint to retrieve video events at a prison sorted by location and start time")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "A list of video events at the prison between two dates",
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
  @PostMapping(value = ["/prison/{prisonCode}/list-by-location"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW', 'BVLS_ACCESS__R')")
  fun getVideoEventsForAPrison(
    @PathVariable @RequestParam(value = "prisonCode", required = true)
    prisonCode: String,
    @Valid @RequestBody @Parameter(description = "A request to return video events at the prison", required = true)
    request: VideoEventRequest,
  ) = videoEventsByLocationService.videoEventsByLocation(prisonCode, request)
}
