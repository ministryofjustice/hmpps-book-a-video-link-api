package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.CsvMapperConfig.csvMapper
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingHistoryRepository
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream
import kotlin.streams.asSequence

@Service
class VideoBookingsToCsvService(
  private val videoBookingHistoryRepository: VideoBookingHistoryRepository,
  private val locationsService: LocationsService,
) {

  @Transactional(readOnly = true)
  fun courtBookingsByHearingDateToCsv(fromDate: LocalDate, toDate: LocalDate, csvOutputStream: OutputStream) {
    writeCourtBookingsToCsv(videoBookingHistoryRepository.findByMainDateBetween(fromDate, toDate), csvOutputStream)
  }

  @Transactional(readOnly = true)
  fun courtBookingsByBookingDateToCsv(fromDate: LocalDate, toDate: LocalDate, csvOutputStream: OutputStream) {
    writeCourtBookingsToCsv(videoBookingHistoryRepository.findByDateOfBookingBetween(fromDate, toDate), csvOutputStream)
  }

  private fun writeCourtBookingsToCsv(bookings: Stream<VideoBookingHistory>, csvOutputStream: OutputStream) {
    val locationsByPrisonCode = mutableMapOf<String, List<Location>>()

    val courtBookings = bookings.filter(VideoBookingHistory::isCourtBooking)
      .peek { locationsByPrisonCode.getOrPut(it.prisonCode) { locationsService.getVideoLinkLocationsAtPrison(it.prisonCode, false) } }
      .map { CourtBookingDto(it, locationsByPrisonCode) }
      .asSequence()

    csvMapper
      .writer(csvMapper.schemaFor(CourtBookingDto::class.java).withHeader())
      .writeValues(csvOutputStream.bufferedWriter())
      .use { writer -> courtBookings.forEach(writer::write) }
  }
}

@JsonPropertyOrder(
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
data class CourtBookingDto(
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
  constructor(vbh: VideoBookingHistory, locations: Map<String, List<Location>>) : this(
    vbh.timestamp,
    vbh.videoBookingId,
    // Old BVLS does not have CANCEL, it has DELETE instead
    vbh.historyType.let { if (it == "CANCEL") "DELETE" else it },
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
