package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.getBvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoBookingSearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.VideoLinkBookingsService

@Tag(name = "Video Link Booking Controller")
@RestController
@RequestMapping(value = ["video-link-booking"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class VideoLinkBookingController(
  val bookingFacade: BookingFacade,
  val videoLinkBookingsService: VideoLinkBookingsService,
) {

  @Operation(
    summary = "Endpoint to support the creation of video link bookings",
    description = "Only external users can create probation meetings, prison users cannot create them.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The unique identifier of the created video booking",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Long::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun create(
    @Valid
    @RequestBody
    @Parameter(description = "The request with the new video link booking details", required = true)
    request: CreateVideoBookingRequest,
    httpRequest: HttpServletRequest,
  ): Long = bookingFacade.create(request, httpRequest.getBvlsRequestContext().user)

  @Operation(summary = "Endpoint to support the amendment of video link bookings")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The unique identifier of the video booking",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = Long::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The video booking ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PutMapping(value = ["/id/{videoBookingId}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun amend(
    @PathVariable("videoBookingId")
    videoBookingId: Long,
    @Valid
    @RequestBody
    @Parameter(description = "The request with the amended video link booking details", required = true)
    request: AmendVideoBookingRequest,
    httpRequest: HttpServletRequest,
  ): Long = bookingFacade.amend(videoBookingId, request, httpRequest.getBvlsRequestContext().user)

  @Operation(summary = "Endpoint to return the details of a video link booking using its internal ID")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Video link booking details",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = VideoLinkBooking::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The video booking ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping(value = ["/id/{videoBookingId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun getVideoLinkBookingById(
    @PathVariable("videoBookingId") videoBookingId: Long,
    httpRequest: HttpServletRequest,
  ) = videoLinkBookingsService.getVideoLinkBookingById(videoBookingId, httpRequest.getBvlsRequestContext().user)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(value = ["/id/{videoBookingId}"])
  @Operation(summary = "Endpoint to support cancellation of a video booking.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Video booking was cancelled.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The video booking ID was not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun cancelVideoBooking(
    @PathVariable("videoBookingId") videoBookingId: Long,
    httpRequest: HttpServletRequest,
  ) {
    when (val user = httpRequest.getBvlsRequestContext().user) {
      is PrisonUser -> bookingFacade.cancel(videoBookingId, user)
      is ExternalUser -> bookingFacade.cancel(videoBookingId, user)
      else -> throw IllegalArgumentException("Unsupported user for cancel operation.")
    }
  }

  @Operation(summary = "Endpoint to allow external users request a prison to create a video link booking for a prisoner due to arrive")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The video link booking has been requested",
      ),
    ],
  )
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/request"])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun requestBooking(
    @Valid
    @RequestBody
    @Parameter(description = "The request with the requested video link booking details", required = true)
    request: RequestVideoBookingRequest,
    httpRequest: HttpServletRequest,
  ) = bookingFacade.request(request, httpRequest.getBvlsRequestContext().user as ExternalUser)

  @Operation(summary = "Endpoint to search for a unique matching video link booking.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "The matching video link booking details",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = VideoLinkBooking::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE], path = ["/search"])
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN', 'BVLS_ACCESS__RW')")
  fun searchForBooking(
    @Valid
    @RequestBody
    @Parameter(description = "The request with the search criteria for a video booking", required = true)
    searchRequest: VideoBookingSearchRequest,
    httpRequest: HttpServletRequest,
  ) = videoLinkBookingsService.findMatchingVideoLinkBooking(searchRequest, httpRequest.getBvlsRequestContext().user)
}
