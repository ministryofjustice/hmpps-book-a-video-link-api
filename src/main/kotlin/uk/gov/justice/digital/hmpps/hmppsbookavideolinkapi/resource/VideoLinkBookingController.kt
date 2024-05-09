package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoLinkBookingCreateRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CreateVideoLinkBookingService

@Tag(name = "Video Link Booking")
@RestController
@RequestMapping(value = ["video-link-booking"], produces = [MediaType.APPLICATION_JSON_VALUE])
class VideoLinkBookingController(val createVideoLinkBookingService: CreateVideoLinkBookingService) {

  @Operation(summary = "Endpoint to support the creation of video link bookings")
  @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun create(
    @Valid
    @RequestBody
    @Parameter(description = "The request with the new video link booking details", required = true)
    request: VideoLinkBookingCreateRequest,
  ): Long = createVideoLinkBookingService.create(request)
}
