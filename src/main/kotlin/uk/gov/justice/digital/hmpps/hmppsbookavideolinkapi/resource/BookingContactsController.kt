package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ContactsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Tag(name = "Booking Contacts Controller")
@RestController
@RequestMapping(value = ["booking-contacts"], produces = [MediaType.APPLICATION_JSON_VALUE])
@AuthApiResponses
class BookingContactsController(val contactsService: ContactsService) {
  @Operation(summary = "Endpoint to return a list of contacts associated with a booking")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Contacts for this booking",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = BookingContact::class)),
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
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun getContactsForBooking(
    @PathVariable("videoBookingId") videoBookingId: Long,
  ): List<BookingContact> = contactsService.getAllBookingContacts(videoBookingId).toModel()
}
