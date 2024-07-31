package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CsvDataExtractionService
import java.io.OutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * StreamingResponseBody return types are asynchronous by default, the methods themselves do not need annotating.
 *
 * By using the @EnableAsync annotation we can override Springs defaults settings in the Spring configuration (file).
 */
@EnableAsync
@Tag(name = "CSV Data Extraction Controller")
@RestController
@RequestMapping(value = ["download-csv"], produces = ["text/csv", MediaType.APPLICATION_JSON_VALUE])
class CsvDataExtractionController(private val service: CsvDataExtractionService) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @GetMapping(path = ["/court-data-by-hearing-date"])
  @Operation(description = "Return details of court video link bookings by hearing date in CSV format. Restrict the response to events occurring within 'days' of start-date.")
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun downloadCourtBookingsByHearingDate(
    @RequestParam(name = "start-date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "The earliest hearing date for which to return event details.", required = true)
    startDate: LocalDate,
    @RequestParam(name = "days")
    @Parameter(description = "Return details of events occurring within this number of days of start-date. A maximum of 365 days.")
    days: Long = 7,
  ): ResponseEntity<StreamingResponseBody> =
    streamedCsvResponse(
      startDate,
      days,
      "video-links-by-court-hearing-date-from-${startDate.toIsoDate()}-for-$days-days.csv",
    ) { os -> service.courtBookingsByHearingDateToCsv(fromDate = startDate, toDate = startDate.plusDays(days), os) }

  @GetMapping(path = ["/probation-data-by-meeting-date"])
  @Operation(description = "Return details of probation video link bookings by meeting date in CSV format. Restrict the response to events occurring within 'days' of start-date.")
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun downloadProbationBookingsByMeetingDate(
    @RequestParam(name = "start-date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "The earliest meeting date for which to return event details.", required = true)
    startDate: LocalDate,
    @RequestParam(name = "days")
    @Parameter(description = "Return details of events occurring within this number of days of start-date. A maximum of 365 days.")
    days: Long = 7,
  ): ResponseEntity<StreamingResponseBody> =
    streamedCsvResponse(
      startDate,
      days,
      "video-links-by-probation-meeting-date-from-${startDate.toIsoDate()}-for-$days-days.csv",
    ) { os -> service.probationBookingsByMeetingDateToCsv(fromDate = startDate, toDate = startDate.plusDays(days), os) }

  @GetMapping(path = ["/court-data-by-booking-date"])
  @Operation(description = "Return details of court video link bookings by booking date in CSV format. Restrict the response to events occurring within 'days' of start-date.")
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun downloadCourtBookingsByBookingDate(
    @RequestParam(name = "start-date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "The earliest booking date for which to return bookings for.", required = true)
    startDate: LocalDate,
    @RequestParam(name = "days")
    @Parameter(description = "Return details of bookings occurring within this number of days of start-date. A maximum of 365 days.")
    days: Long = 7,
  ): ResponseEntity<StreamingResponseBody> =
    streamedCsvResponse(
      startDate,
      days,
      "video-links-by-court-booking-date-from-${startDate.toIsoDate()}-for-$days-days.csv",
    ) { os -> service.courtBookingsByBookingDateToCsv(fromDate = startDate, toDate = startDate.plusDays(days), os) }

  @GetMapping(path = ["/probation-data-by-booking-date"])
  @Operation(description = "Return details of probation video link bookings by booking date in CSV format. Restrict the response to events occurring within 'days' of start-date.")
  @PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
  fun downloadProbationBookingsByBookingDate(
    @RequestParam(name = "start-date", required = true)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "The earliest booking date for which to return bookings for.", required = true)
    startDate: LocalDate,
    @RequestParam(name = "days")
    @Parameter(description = "Return details of bookings occurring within this number of days of start-date. A maximum of 365 days.")
    days: Long = 7,
  ): ResponseEntity<StreamingResponseBody> =
    streamedCsvResponse(
      startDate,
      days,
      "video-links-by-probation-booking-date-from-${startDate.toIsoDate()}-for-$days-days.csv",
    ) { os -> service.probationBookingsByBookingDateToCsv(fromDate = startDate, toDate = startDate.plusDays(days), os) }

  private fun streamedCsvResponse(startDate: LocalDate, days: Long, filename: String, block: (outputStream: OutputStream) -> Unit): ResponseEntity<StreamingResponseBody> {
    require(ChronoUnit.DAYS.between(startDate, startDate.plusDays(days)) <= 365) {
      "CSV extracts are limited to a years worth of data."
    }

    log.info("CSV controller: beginning CSV download for $filename")

    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
      .contentType(MediaType.parseMediaType("text/csv"))
      .body(StreamingResponseBody { block(it) })
  }
}
