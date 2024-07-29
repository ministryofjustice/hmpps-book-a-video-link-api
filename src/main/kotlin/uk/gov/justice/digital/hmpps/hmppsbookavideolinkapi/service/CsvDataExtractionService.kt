package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.CsvMapperConfig.csvMapper
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBookingEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingEventRepository
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.DAYS
import java.util.stream.Stream
import kotlin.streams.asSequence

@Service
class CsvDataExtractionService(
  private val videoBookingEventRepository: VideoBookingEventRepository,
  private val locationsService: LocationsService,
) {

  @Transactional(readOnly = true)
  fun courtBookingsByHearingDateToCsv(fromDate: LocalDate, toDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(fromDate, toDate)

    writeCourtBookingsToCsv(videoBookingEventRepository.findByMainDateBetween(true, fromDate, toDate), csvOutputStream)
  }

  @Transactional(readOnly = true)
  fun courtBookingsByBookingDateToCsv(fromDate: LocalDate, toDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(fromDate, toDate)

    writeCourtBookingsToCsv(videoBookingEventRepository.findByDateOfBookingBetween(true, fromDate, toDate), csvOutputStream)
  }

  private fun writeCourtBookingsToCsv(events: Stream<VideoBookingEvent>, csvOutputStream: OutputStream) {
    val locationsByPrisonCode = mutableMapOf<String, List<Location>>()

    val courtEvents = events
      .peek { locationsByPrisonCode.getOrPut(it.prisonCode) { locationsService.getVideoLinkLocationsAtPrison(it.prisonCode, false) } }
      .map { CourtBookingEvent(it, locationsByPrisonCode) }
      .asSequence()

    csvMapper
      .writer(csvMapper.schemaFor(CourtBookingEvent::class.java).withHeader())
      .writeValues(csvOutputStream.bufferedWriter())
      .use { writer -> courtEvents.forEach(writer::write) }
  }

  @Transactional(readOnly = true)
  fun probationBookingsByMeetingDateToCsv(fromDate: LocalDate, toDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(fromDate, toDate)

    writeProbationBookingsToCsv(videoBookingEventRepository.findByMainDateBetween(false, fromDate, toDate), csvOutputStream)
  }

  @Transactional(readOnly = true)
  fun probationBookingsByBookingDateToCsv(fromDate: LocalDate, toDate: LocalDate, csvOutputStream: OutputStream) {
    checkDaysBetweenDoesNotExceedAYear(fromDate, toDate)

    writeProbationBookingsToCsv(videoBookingEventRepository.findByDateOfBookingBetween(false, fromDate, toDate), csvOutputStream)
  }

  private fun writeProbationBookingsToCsv(events: Stream<VideoBookingEvent>, csvOutputStream: OutputStream) {
    val locationsByPrisonCode = mutableMapOf<String, List<Location>>()

    val probationEvents = events
      .peek { locationsByPrisonCode.getOrPut(it.prisonCode) { locationsService.getVideoLinkLocationsAtPrison(it.prisonCode, false) } }
      .map { ProbationBookingEvent(it, locationsByPrisonCode) }
      .asSequence()

    csvMapper
      .writer(csvMapper.schemaFor(ProbationBookingEvent::class.java).withHeader())
      .writeValues(csvOutputStream.bufferedWriter())
      .use { writer -> probationEvents.forEach(writer::write) }
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
    // Old BVLS does not have CANCEL, it has DELETE instead
    vbh.eventType.let { if (it == "CANCEL") "DELETE" else it },
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
    vbh.mainLocationKey.let { key -> locations[vbh.prisonCode]?.singleOrNull { it.key == key }?.description ?: key },
    vbh.preLocationKey?.let { key -> locations[vbh.prisonCode]?.singleOrNull { it.key == key }?.description ?: key },
    vbh.postLocationKey?.let { key -> locations[vbh.prisonCode]?.singleOrNull { it.key == key }?.description ?: key },
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
data class ProbationBookingEvent(
  val eventId: Long,
  val timestamp: LocalDateTime,
  val videoLinkBookingId: Long,
  val eventType: String,
  val agencyId: String,
  val probationTeam: String,
  val probationTeamId: String,
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
    // Old BVLS does not have CANCEL, it has DELETE instead
    vbh.eventType.let { if (it == "CANCEL") "DELETE" else it },
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
    vbh.mainLocationKey.let { key -> locations[vbh.prisonCode]?.singleOrNull { it.key == key }?.description ?: key },
    null,
    null,
  )
}
