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

  @Transactional(readOnly = true)
  fun courtBookingsByHearingDateToCsv(fromDate: LocalDate, toDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(fromDate, toDate)

    val total: Int
    val elapsed = measureTimeMillis {
      total = writeCourtBookingsToCsv(videoBookingEventRepository.findByMainDateBetween(true, fromDate, toDate), csvOutputStream)
    }

    log.info("CSV: time taken downloading $total court bookings by hearing date from $fromDate to $toDate in millis=$elapsed")
  }

  @Transactional(readOnly = true)
  fun courtBookingsByBookingDateToCsv(fromDate: LocalDate, toDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(fromDate, toDate)

    val total: Int
    val elapsed = measureTimeMillis {
      total = writeCourtBookingsToCsv(videoBookingEventRepository.findByDateOfBookingBetween(true, fromDate, toDate), csvOutputStream)
    }

    log.info("CSV: time taken downloading $total court bookings by booking date from $fromDate to $toDate in millis=$elapsed")
  }

  private fun writeCourtBookingsToCsv(events: Stream<VideoBookingEvent>, csvOutputStream: OutputStream): Int {
    val locationsByPrisonCode = mutableMapOf<String, List<Location>>()

    val courtEvents = events
      .peek { locationsByPrisonCode.getOrPut(it.prisonCode) { locationsInsidePrisonClient.getNonResidentialAppointmentLocationsAtPrison(it.prisonCode) } }
      .map { CourtBookingEvent(it, locationsByPrisonCode) }
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

  @Transactional(readOnly = true)
  fun probationBookingsByMeetingDateToCsv(fromDate: LocalDate, toDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(fromDate, toDate)

    val total: Int
    val elapsed = measureTimeMillis {
      total = writeProbationBookingsToCsv(videoBookingEventRepository.findByMainDateBetween(false, fromDate, toDate), csvOutputStream)
    }

    log.info("CSV: time taken downloading $total probation bookings by meeting date from $fromDate to $toDate in millis=$elapsed")
  }

  @Transactional(readOnly = true)
  fun probationBookingsByBookingDateToCsv(fromDate: LocalDate, toDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(fromDate, toDate)

    val total: Int
    val elapsed = measureTimeMillis {
      total = writeProbationBookingsToCsv(videoBookingEventRepository.findByDateOfBookingBetween(false, fromDate, toDate), csvOutputStream)
    }

    log.info("CSV: time taken downloading $total probation bookings by booking date from $fromDate to $toDate in millis=$elapsed")
  }

  private fun writeProbationBookingsToCsv(events: Stream<VideoBookingEvent>, csvOutputStream: OutputStream): Int {
    val locationsByPrisonCode = mutableMapOf<String, List<Location>>()

    val probationEvents = events
      .peek { locationsByPrisonCode.getOrPut(it.prisonCode) { locationsInsidePrisonClient.getNonResidentialAppointmentLocationsAtPrison(it.prisonCode) } }
      .map { ProbationBookingEvent(it, locationsByPrisonCode) }
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
) {
  constructor(vbh: VideoBookingEvent, locations: Map<String, List<Location>>) : this(
    vbh.eventId,
    vbh.timestamp,
    vbh.videoBookingId,
    // Old BVLS has DELETE instead of CANCEL and UPDATE instead of AMEND
    vbh.eventType.let { if (it == "CANCEL") "DELETE" else if (it == "AMEND") "UPDATE" else it },
    vbh.prisonCode,
    vbh.courtDescription!!,
    vbh.courtCode!!,
    vbh.createdByPrison.not(),
    vbh.mainDate.atTime(vbh.mainStartTime),
    vbh.mainDate.atTime(vbh.mainEndTime),
    vbh.preDate?.atTime(vbh.preStartTime),
    vbh.preDate?.atTime(vbh.preEndTime),
    vbh.postDate?.atTime(vbh.postStartTime),
    vbh.postDate?.atTime(vbh.postEndTime),
    vbh.mainLocationId.let { id -> locations[vbh.prisonCode]?.singleOrNull { it.id == id }?.let { it.localName ?: it.key } ?: id.toString() },
    vbh.preLocationId?.let { id -> locations[vbh.prisonCode]?.singleOrNull { it.id == id }?.let { it.localName ?: it.key } ?: id.toString() },
    vbh.postLocationId?.let { id -> locations[vbh.prisonCode]?.singleOrNull { it.id == id }?.let { it.localName ?: it.key } ?: id.toString() },
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
) {
  constructor(vbh: VideoBookingEvent, locations: Map<String, List<Location>>) : this(
    vbh.eventId,
    vbh.timestamp,
    vbh.videoBookingId,
    // Old BVLS has DELETE instead of CANCEL and UPDATE instead of AMEND
    vbh.eventType.let { if (it == "CANCEL") "DELETE" else if (it == "AMEND") "UPDATE" else it },
    vbh.prisonCode,
    vbh.probationTeamDescription!!,
    vbh.probationTeamCode!!,
    vbh.createdByPrison.not(),
    vbh.mainDate.atTime(vbh.mainStartTime),
    vbh.mainDate.atTime(vbh.mainEndTime),
    null,
    null,
    null,
    null,
    vbh.mainLocationId.let { id -> locations[vbh.prisonCode]?.singleOrNull { it.id == id }?.let { it.localName ?: it.key } ?: id.toString() },
    null,
    null,
  )
}
