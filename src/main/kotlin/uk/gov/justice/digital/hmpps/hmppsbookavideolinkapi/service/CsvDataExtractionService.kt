package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.CsvMapperConfig.csvMapper
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBookingEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingEventRepository
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.DAYS
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.system.measureTimeMillis

@Service
class CsvDataExtractionService(
  private val videoBookingEventRepository: VideoBookingEventRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * fromDate is inclusive and endDate is exclusive
   */
  @Transactional(readOnly = true)
  fun courtBookingsByHearingDateToCsv(inclusiveFromDate: LocalDate, exclusiveToDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(inclusiveFromDate, exclusiveToDate)

    val total: Int
    val elapsed = measureTimeMillis {
      total = writeCourtBookingsToCsv(videoBookingEventRepository.findByMainDateBetween(true, inclusiveFromDate, exclusiveToDate), csvOutputStream)
    }

    log.info("CSV: time taken downloading $total court bookings by hearing date from $inclusiveFromDate to $exclusiveToDate in millis=$elapsed")
  }

  /**
   * fromDate is inclusive and endDate is exclusive
   */
  @Transactional(readOnly = true)
  fun courtBookingsByBookingDateToCsv(inclusiveFromDate: LocalDate, exclusiveToDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(inclusiveFromDate, exclusiveToDate)

    val total: Int
    val elapsed = measureTimeMillis {
      total = writeCourtBookingsToCsv(videoBookingEventRepository.findByDateOfBookingBetween(true, inclusiveFromDate, exclusiveToDate), csvOutputStream)
    }

    log.info("CSV: time taken downloading $total court bookings by booking date from $inclusiveFromDate to $exclusiveToDate in millis=$elapsed")
  }

  private fun writeCourtBookingsToCsv(events: Stream<VideoBookingEvent>, csvOutputStream: OutputStream): Int {
    val courtEvents = events
      // We include all non-residential locations for CSV to support migrated bookings which could be at any appointment location
      .map { CourtBookingEvent(it, locationsInsidePrisonClient.getNonResidentialAppointmentLocationsAtPrison(it.prisonCode).toSet()) }
      .asSequence()

    var counter = 0
    csvMapper
      .writer(csvMapper.schemaFor(CourtBookingEvent::class.java).withHeader())
      .writeValues(csvOutputStream.bufferedWriter())
      .use { writer ->
        courtEvents.forEach { event ->
          writer.write(event)
          counter++
          if (counter % 100 == 0) log.info("CSV: extracted $counter court events so far ...")
        }
      }

    return counter
  }

  /**
   * fromDate is inclusive and endDate is exclusive
   */
  @Transactional(readOnly = true)
  fun probationBookingsByMeetingDateToCsv(inclusiveFromDate: LocalDate, exclusiveToDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(inclusiveFromDate, exclusiveToDate)

    val total: Int
    val elapsed = measureTimeMillis {
      total = writeProbationBookingsToCsv(videoBookingEventRepository.findByMainDateBetween(false, inclusiveFromDate, exclusiveToDate), csvOutputStream)
    }

    log.info("CSV: time taken downloading $total probation bookings by meeting date from $inclusiveFromDate to $exclusiveToDate in millis=$elapsed")
  }

  /**
   * fromDate is inclusive and endDate is exclusive
   */
  @Transactional(readOnly = true)
  fun probationBookingsByBookingDateToCsv(inclusiveFromDate: LocalDate, exclusiveToDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(inclusiveFromDate, exclusiveToDate)

    val total: Int
    val elapsed = measureTimeMillis {
      total = writeProbationBookingsToCsv(videoBookingEventRepository.findByDateOfBookingBetween(false, inclusiveFromDate, exclusiveToDate), csvOutputStream)
    }

    log.info("CSV: time taken downloading $total probation bookings by booking date from $inclusiveFromDate to $exclusiveToDate in millis=$elapsed")
  }

  private fun writeProbationBookingsToCsv(events: Stream<VideoBookingEvent>, csvOutputStream: OutputStream): Int {
    val probationEvents = events
      // We include all non-residential locations for CSV to support migrated bookings which could be at any appointment location
      .map { ProbationBookingEvent(it, locationsInsidePrisonClient.getNonResidentialAppointmentLocationsAtPrison(it.prisonCode).toSet()) }
      .asSequence()

    var counter = 0
    csvMapper
      .writer(csvMapper.schemaFor(ProbationBookingEvent::class.java).withHeader())
      .writeValues(csvOutputStream.bufferedWriter())
      .use { writer ->
        probationEvents.forEach { event ->
          writer.write(event)
          counter++
          if (counter % 100 == 0) log.info("CSV: extracted $counter probation events so far ...")
        }
      }

    return counter
  }

  private fun checkDaysBetweenDoesNotExceedAYear(fromDate: LocalDate, toDate: LocalDate) {
    require(DAYS.between(fromDate, toDate) <= 365) {
      "CSV extracts are limited to a years worth of data."
    }
  }
}

@JsonPropertyOrder(
  "eventId",
  "timestamp",
  "videoLinkBookingId",
  "eventType",
  "agencyId",
  "court",
  "courtId",
  "madeByTheCourt",
  "mainStartTime",
  "mainEndTime",
  "preStartTime",
  "preEndTime",
  "postStartTime",
  "postEndTime",
  "mainLocationName",
  "preLocationName",
  "postLocationName",
  "hearingType",
  "user",
  "cvpLink",
)
data class CourtBookingEvent(
  val eventId: Long,
  val timestamp: LocalDateTime,
  val videoLinkBookingId: Long,
  val eventType: String,
  val agencyId: String,
  val court: String,
  val courtId: String,
  val madeByTheCourt: Boolean,
  val mainStartTime: LocalDateTime,
  val mainEndTime: LocalDateTime,
  val preStartTime: LocalDateTime?,
  val preEndTime: LocalDateTime?,
  val postStartTime: LocalDateTime?,
  val postEndTime: LocalDateTime?,
  val mainLocationName: String,
  val preLocationName: String?,
  val postLocationName: String?,
  val hearingType: String,
  val user: String,
  val cvpLink: String?,
) {
  constructor(vbe: VideoBookingEvent, locations: Set<Location>) : this(
    vbe.eventId,
    vbe.timestamp,
    vbe.videoBookingId,
    // Old BVLS has DELETE instead of CANCEL and UPDATE instead of AMEND
    vbe.eventType.let {
      if (it == "CANCEL") {
        "DELETE"
      } else if (it == "AMEND") {
        "UPDATE"
      } else {
        it
      }
    },
    vbe.prisonCode,
    vbe.courtDescription!!,
    vbe.courtCode!!,
    vbe.createdByPrison.not(),
    vbe.mainDate.atTime(vbe.mainStartTime),
    vbe.mainDate.atTime(vbe.mainEndTime),
    vbe.preDate?.atTime(vbe.preStartTime),
    vbe.preDate?.atTime(vbe.preEndTime),
    vbe.postDate?.atTime(vbe.postStartTime),
    vbe.postDate?.atTime(vbe.postEndTime),
    vbe.mainLocationId.let { id -> locations.singleOrNull { it.id == id }?.let { it.localName ?: it.key } ?: id.toString() },
    vbe.preLocationId?.let { id -> locations.singleOrNull { it.id == id }?.let { it.localName ?: it.key } ?: id.toString() },
    vbe.postLocationId?.let { id -> locations.singleOrNull { it.id == id }?.let { it.localName ?: it.key } ?: id.toString() },
    vbe.type,
    vbe.user,
    vbe.cvpLink,
  )
}

@JsonPropertyOrder(
  "eventId",
  "timestamp",
  "videoLinkBookingId",
  "eventType",
  "agencyId",
  "probationTeam",
  "probationTeamId",
  "madeByProbation",
  "mainStartTime",
  "mainEndTime",
  "preStartTime",
  "preEndTime",
  "postStartTime",
  "postEndTime",
  "mainLocationName",
  "preLocationName",
  "postLocationName",
  "meetingType",
  "user",
)
data class ProbationBookingEvent(
  val eventId: Long,
  val timestamp: LocalDateTime,
  val videoLinkBookingId: Long,
  val eventType: String,
  val agencyId: String,
  val probationTeam: String,
  val probationTeamId: String,
  val madeByProbation: Boolean,
  val mainStartTime: LocalDateTime,
  val mainEndTime: LocalDateTime,
  val preStartTime: LocalDateTime?,
  val preEndTime: LocalDateTime?,
  val postStartTime: LocalDateTime?,
  val postEndTime: LocalDateTime?,
  val mainLocationName: String,
  val preLocationName: String?,
  val postLocationName: String?,
  val meetingType: String,
  val user: String,
) {
  constructor(vbe: VideoBookingEvent, locations: Set<Location>) : this(
    vbe.eventId,
    vbe.timestamp,
    vbe.videoBookingId,
    // Old BVLS has DELETE instead of CANCEL and UPDATE instead of AMEND
    vbe.eventType.let {
      if (it == "CANCEL") {
        "DELETE"
      } else if (it == "AMEND") {
        "UPDATE"
      } else {
        it
      }
    },
    vbe.prisonCode,
    vbe.probationTeamDescription!!,
    vbe.probationTeamCode!!,
    vbe.createdByPrison.not(),
    vbe.mainDate.atTime(vbe.mainStartTime),
    vbe.mainDate.atTime(vbe.mainEndTime),
    null,
    null,
    null,
    null,
    vbe.mainLocationId.let { id -> locations.singleOrNull { it.id == id }?.let { it.localName ?: it.key } ?: id.toString() },
    null,
    null,
    vbe.type,
    vbe.user,
  )
}
