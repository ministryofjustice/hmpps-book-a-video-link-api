package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CsvDataExtractionService
import java.time.LocalDate

@Tag(name = "CSV Data Extraction Controller")
@RestController
@RequestMapping(value = ["download-csv"])
class CsvDataExtractionController(private val service: CsvDataExtractionService) {

  @GetMapping(path = ["/court-data-by-hearing-date"], produces = ["text/csv"])
  @Operation(description = "Return details of court video link bookings by hearing date in CSV format. Restrict the response to events occurring within 'days' of start-date.")
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun downloadCourtBookingsByHearingDate(
    @RequestParam(name = "start-date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "The earliest hearing date for which to return event details.", required = true)
    startDate: LocalDate,
    @RequestParam(name = "days")
    @Parameter(description = "Return details of events occurring within this number of days of start-date")
    days: Long = 7,
  ): ResponseEntity<StreamingResponseBody> {
    return ResponseEntity.ok()
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=video-links-by-court-hearing-date-from-${startDate.toIsoDate()}-for-$days-days.csv",
      )
      .contentType(MediaType.parseMediaType("text/csv"))
      .body(
        StreamingResponseBody {
          service.courtBookingsByHearingDateToCsv(
            fromDate = startDate,
            toDate = startDate.plusDays(days),
            it,
          )
        },
      )
  }

  @GetMapping(path = ["/probation-data-by-meeting-date"], produces = ["text/csv"])
  @Operation(description = "Return details of probation video link bookings by meeting date in CSV format. Restrict the response to events occurring within 'days' of start-date.")
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun downloadProbationBookingsByMeetingDate(
    @RequestParam(name = "start-date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "The earliest meeting date for which to return event details.", required = true)
    startDate: LocalDate,
    @RequestParam(name = "days")
    @Parameter(description = "Return details of events occurring within this number of days of start-date")
    days: Long = 7,
  ): ResponseEntity<StreamingResponseBody> {
    return ResponseEntity.ok()
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=video-links-by-probation-meeting-date-from-${startDate.toIsoDate()}-for-$days-days.csv",
      )
      .contentType(MediaType.parseMediaType("text/csv"))
      .body(
        StreamingResponseBody {
          service.probationBookingsByMeetingDateToCsv(
            fromDate = startDate,
            toDate = startDate.plusDays(days),
            it,
          )
        },
      )
  }

  @GetMapping(path = ["/court-data-by-booking-date"], produces = ["text/csv"])
  @Operation(description = "Return details of court video link bookings by booking date in CSV format. Restrict the response to events occurring within 'days' of start-date.")
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun downloadCourtBookingsByBookingDate(
    @RequestParam(name = "start-date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "The earliest booking date for which to return bookings for.", required = true)
    startDate: LocalDate,
    @RequestParam(name = "days")
    @Parameter(description = "Return details of bookings occurring within this number of days of start-date")
    days: Long = 7,
  ): ResponseEntity<StreamingResponseBody> =
    ResponseEntity.ok()
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=video-links-by-court-booking-date-from-${startDate.toIsoDate()}-for-$days-days.csv",
      )
      .contentType(MediaType.parseMediaType("text/csv"))
      .body(
        StreamingResponseBody {
          service.courtBookingsByBookingDateToCsv(
            fromDate = startDate,
            toDate = startDate.plusDays(days),
            it,
          )
        },
      )

  @GetMapping(path = ["/probation-data-by-booking-date"], produces = ["text/csv"])
  @Operation(description = "Return details of probation video link bookings by booking date in CSV format. Restrict the response to events occurring within 'days' of start-date.")
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun downloadProbationBookingsByBookingDate(
    @RequestParam(name = "start-date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "The earliest booking date for which to return bookings for.", required = true)
    startDate: LocalDate,
    @RequestParam(name = "days")
    @Parameter(description = "Return details of bookings occurring within this number of days of start-date")
    days: Long = 7,
  ): ResponseEntity<StreamingResponseBody> =
    ResponseEntity.ok()
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=video-links-by-probation-booking-date-from-${startDate.toIsoDate()}-for-$days-days.csv",
      )
      .contentType(MediaType.parseMediaType("text/csv"))
      .body(
        StreamingResponseBody {
          service.probationBookingsByBookingDateToCsv(
            fromDate = startDate,
            toDate = startDate.plusDays(days),
            it,
          )
        },
      )
}
