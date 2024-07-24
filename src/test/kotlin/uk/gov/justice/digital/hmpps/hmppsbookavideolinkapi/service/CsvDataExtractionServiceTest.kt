package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingHistoryRepository
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.util.stream.Stream

class CsvDataExtractionServiceTest {
  private val videoBookingHistoryRepository: VideoBookingHistoryRepository = mock()
  private val locationsService: LocationsService = mock()
  private val service = CsvDataExtractionService(videoBookingHistoryRepository, locationsService)
  private val csvOutputStream = ByteArrayOutputStream()
  private val moorlandCourtBooking = VideoBookingHistory(
    videoBookingId = 1,
    dateOfBooking = today(),
    timestamp = LocalDateTime.of(2024, Month.JULY, 1, 9, 0),
    historyType = "CREATE",
    prisonCode = MOORLAND,
    courtDescription = "court description",
    courtCode = "court code",
    createdByPrison = false,
    mainDate = LocalDate.of(2024, Month.JULY, 10),
    mainStartTime = LocalTime.of(10, 0),
    mainEndTime = LocalTime.of(11, 0),
    preDate = LocalDate.of(2024, Month.JULY, 10),
    preStartTime = LocalTime.of(9, 0),
    preEndTime = LocalTime.of(10, 0),
    postDate = LocalDate.of(2024, Month.JULY, 10),
    postStartTime = LocalTime.of(11, 0),
    postEndTime = LocalTime.of(12, 0),
    mainLocationKey = "main-loc-key",
    preLocationKey = "pre-loc-key",
    postLocationKey = "post-loc-key",
  )
  private val moorlandProbationBooking = moorlandCourtBooking.copy(courtDescription = null, courtCode = null, probationTeamCode = "probation code", probationTeamDescription = "probation team description")
  private val locations = listOf(
    location(key = "pre-loc-key", description = "Pre location"),
    location(key = "main-loc-key", description = "Main location"),
    location(key = "post-loc-key", description = "Post location"),
  )
  private val moorlandPrisonCourtBooking = moorlandCourtBooking.copy(createdByPrison = true)
  private val risleyCourtBooking = moorlandCourtBooking.copy(prisonCode = RISLEY, historyType = "CANCEL")
  private val risleyPrisonCourtBooking = risleyCourtBooking.copy(createdByPrison = true)
  private val risleyProbationBooking = moorlandProbationBooking.copy(prisonCode = RISLEY)

  @BeforeEach
  fun before() {
    locationsService.stub {
      on { getVideoLinkLocationsAtPrison(MOORLAND, false) } doReturn locations
      on { getVideoLinkLocationsAtPrison(RISLEY, false) } doReturn locations
    }
  }

  @Test
  fun `should produce CSV for court court bookings by hearing date only and probation meetings are not included`() {
    whenever(videoBookingHistoryRepository.findByMainDateBetween(any(), any())) doReturn Stream.of(moorlandCourtBooking, moorlandProbationBooking)

    service.courtBookingsByHearingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingHistoryRepository).findByMainDateBetween(today(), tomorrow())
    verify(videoBookingHistoryRepository, never()).findByDateOfBookingBetween(any(), any())

    csvOutputStream.toString() isEqualTo
      "timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "2024-07-01T09:00:00,1,CREATE,$MOORLAND,\"court description\",\"court code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"Main location\",\"Pre location\",\"Post location\"\n"
  }

  @Test
  fun `should produce CSV for prison court bookings by hearing date`() {
    whenever(videoBookingHistoryRepository.findByMainDateBetween(any(), any())) doReturn Stream.of(moorlandPrisonCourtBooking)

    service.courtBookingsByHearingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingHistoryRepository).findByMainDateBetween(today(), tomorrow())
    verify(videoBookingHistoryRepository, never()).findByDateOfBookingBetween(any(), any())

    csvOutputStream.toString() isEqualTo
      "timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "2024-07-01T09:00:00,1,CREATE,$MOORLAND,\"court description\",\"court code\",false,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"Main location\",\"Pre location\",\"Post location\"\n"
  }

  @Test
  fun `should produce CSV for court court bookings by booking date and probation meetings are not included`() {
    whenever(videoBookingHistoryRepository.findByDateOfBookingBetween(any(), any())) doReturn Stream.of(
      risleyCourtBooking,
      risleyProbationBooking,
    )

    service.courtBookingsByBookingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingHistoryRepository).findByDateOfBookingBetween(today(), tomorrow())
    verify(videoBookingHistoryRepository, never()).findByMainDateBetween(any(), any())

    csvOutputStream.toString() isEqualTo
      "timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "2024-07-01T09:00:00,1,DELETE,$RISLEY,\"court description\",\"court code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"Main location\",\"Pre location\",\"Post location\"\n"
  }

  @Test
  fun `should produce CSV for prison court bookings by booking date`() {
    whenever(videoBookingHistoryRepository.findByDateOfBookingBetween(any(), any())) doReturn Stream.of(
      risleyPrisonCourtBooking,
    )

    service.courtBookingsByBookingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingHistoryRepository).findByDateOfBookingBetween(today(), tomorrow())
    verify(videoBookingHistoryRepository, never()).findByMainDateBetween(any(), any())

    csvOutputStream.toString() isEqualTo
      "timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "2024-07-01T09:00:00,1,DELETE,$RISLEY,\"court description\",\"court code\",false,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"Main location\",\"Pre location\",\"Post location\"\n"
  }

  private fun location(key: String, description: String) =
    Location(key = key, description = description, enabled = true)
}
