package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBookingEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingEventRepository
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.util.stream.Stream

class CsvDataExtractionServiceTest {
  private val videoBookingEventRepository: VideoBookingEventRepository = mock()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val service = CsvDataExtractionService(videoBookingEventRepository, locationsInsidePrisonClient)
  private val csvOutputStream = ByteArrayOutputStream()
  private val wandsworthCreateCourtBookingEvent = VideoBookingEvent(
    eventId = 1,
    videoBookingId = 1,
    dateOfBooking = today(),
    timestamp = LocalDateTime.of(2024, Month.JULY, 1, 9, 0),
    eventType = "CREATE",
    prisonCode = WANDSWORTH,
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
    mainLocationId = wandsworthLocation.id,
    preLocationId = wandsworthLocation.id,
    postLocationId = wandsworthLocation.id,
    courtBooking = true,
  )
  private val wandsworthCancelCourtBookingEvent = wandsworthCreateCourtBookingEvent.copy(eventType = "CANCEL")
  private val wandsworthAmendCourtBookingEvent = wandsworthCreateCourtBookingEvent.copy(eventType = "AMEND")
  private val wandsworthCreateProbationBookingEvent = wandsworthCreateCourtBookingEvent.copy(courtDescription = null, courtCode = null, probationTeamCode = "probation code", probationTeamDescription = "probation team description", courtBooking = false)
  private val wandsworthCancelProbationBooking = wandsworthCreateProbationBookingEvent.copy(eventType = "CANCEL")
  private val wandsworthAmendProbationBooking = wandsworthCreateProbationBookingEvent.copy(eventType = "AMEND")
  private val wandsworthPrisonCourtBooking = wandsworthCreateCourtBookingEvent.copy(createdByPrison = true)
  private val risleyCourtBooking = wandsworthCreateCourtBookingEvent.copy(prisonCode = RISLEY, eventType = "CANCEL", mainLocationId = risleyLocation.id, preLocationId = risleyLocation.id, postLocationId = risleyLocation.id)
  private val risleyPrisonCourtBooking = risleyCourtBooking.copy(createdByPrison = true, courtBooking = false)
  private val risleyProbationBooking = wandsworthCreateProbationBookingEvent.copy(prisonCode = RISLEY, createdByPrison = false, mainLocationId = risleyLocation.id, preLocationId = risleyLocation.id, postLocationId = risleyLocation.id)

  @BeforeEach
  fun before() {
    locationsInsidePrisonClient.stub {
      on { getNonResidentialAppointmentLocationsAtPrison(WANDSWORTH, false) } doReturn listOf(wandsworthLocation)
      on { getNonResidentialAppointmentLocationsAtPrison(RISLEY, false) } doReturn listOf(risleyLocation)
    }
  }

  @Test
  fun `should produce CSV for court court bookings by hearing date only`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(eq(true), any(), any())) doReturn Stream.of(wandsworthCreateCourtBookingEvent)

    service.courtBookingsByHearingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(true, today(), tomorrow())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "1,2024-07-01T09:00:00,1,CREATE,$WANDSWORTH,\"court description\",\"court code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\"\n"
  }

  @Test
  fun `should fail to produce CSV reports when requested date range more than a year`() {
    assertThrows<IllegalArgumentException> {
      service.courtBookingsByHearingDateToCsv(today(), today().plusDays(366), csvOutputStream)
    }
      .message isEqualTo "CSV extracts are limited to a years worth of data."

    assertThrows<IllegalArgumentException> {
      service.courtBookingsByBookingDateToCsv(today(), today().plusDays(366), csvOutputStream)
    }
      .message isEqualTo "CSV extracts are limited to a years worth of data."

    assertThrows<IllegalArgumentException> {
      service.probationBookingsByMeetingDateToCsv(today(), today().plusDays(366), csvOutputStream)
    }
      .message isEqualTo "CSV extracts are limited to a years worth of data."

    assertThrows<IllegalArgumentException> {
      service.probationBookingsByBookingDateToCsv(today(), today().plusDays(366), csvOutputStream)
    }
      .message isEqualTo "CSV extracts are limited to a years worth of data."
  }

  @Test
  fun `should produce CSV for prison court bookings by hearing date`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(eq(true), any(), any())) doReturn Stream.of(wandsworthPrisonCourtBooking)

    service.courtBookingsByHearingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(true, today(), tomorrow())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "1,2024-07-01T09:00:00,1,CREATE,$WANDSWORTH,\"court description\",\"court code\",false,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\"\n"
  }

  @Test
  fun `should produce CSV for court court bookings by booking date`() {
    whenever(videoBookingEventRepository.findByDateOfBookingBetween(eq(true), any(), any())) doReturn Stream.of(risleyCourtBooking)

    service.courtBookingsByBookingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByDateOfBookingBetween(true, today(), tomorrow())
    verify(videoBookingEventRepository, never()).findByMainDateBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "1,2024-07-01T09:00:00,1,DELETE,$RISLEY,\"court description\",\"court code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${risleyLocation.localName}\",\"${risleyLocation.localName}\",\"${risleyLocation.localName}\"\n"
  }

  @Test
  fun `should produce CSV for prison court bookings by booking date`() {
    whenever(videoBookingEventRepository.findByDateOfBookingBetween(eq(true), any(), any())) doReturn Stream.of(
      risleyPrisonCourtBooking,
    )

    service.courtBookingsByBookingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByDateOfBookingBetween(true, today(), tomorrow())
    verify(videoBookingEventRepository, never()).findByMainDateBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "1,2024-07-01T09:00:00,1,DELETE,$RISLEY,\"court description\",\"court code\",false,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${risleyLocation.localName}\",\"${risleyLocation.localName}\",\"${risleyLocation.localName}\"\n"
  }

  @Test
  fun `should replace CANCEL with DELETE for court booking event`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(eq(true), any(), any())) doReturn Stream.of(wandsworthCancelCourtBookingEvent)

    service.courtBookingsByHearingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(any(), any(), any())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "1,2024-07-01T09:00:00,1,DELETE,$WANDSWORTH,\"court description\",\"court code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\"\n"
  }

  @Test
  fun `should replace AMEND with UPDATE for court booking event`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(eq(true), any(), any())) doReturn Stream.of(wandsworthAmendCourtBookingEvent)

    service.courtBookingsByHearingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(any(), any(), any())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "1,2024-07-01T09:00:00,1,UPDATE,$WANDSWORTH,\"court description\",\"court code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\"\n"
  }

  @Test
  fun `should produce CSV for probation bookings by meeting date only`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(eq(false), any(), any())) doReturn Stream.of(wandsworthCreateProbationBookingEvent)

    service.probationBookingsByMeetingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(false, today(), tomorrow())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,probationTeam,probationTeamId,madeByProbation,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "1,2024-07-01T09:00:00,1,CREATE,$WANDSWORTH,\"probation team description\",\"probation code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,,,,,\"${wandsworthLocation.localName}\",,\n"
  }

  @Test
  fun `should replace CANCEL with DELETE for probation booking event`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(any(), any(), any())) doReturn Stream.of(wandsworthCancelProbationBooking)

    service.probationBookingsByMeetingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(any(), any(), any())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,probationTeam,probationTeamId,madeByProbation,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "1,2024-07-01T09:00:00,1,DELETE,$WANDSWORTH,\"probation team description\",\"probation code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,,,,,\"${wandsworthLocation.localName}\",,\n"
  }

  @Test
  fun `should replace AMEND with UPDATE for probation booking event`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(any(), any(), any())) doReturn Stream.of(wandsworthAmendProbationBooking)

    service.probationBookingsByMeetingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(any(), any(), any())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,probationTeam,probationTeamId,madeByProbation,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "1,2024-07-01T09:00:00,1,UPDATE,$WANDSWORTH,\"probation team description\",\"probation code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,,,,,\"${wandsworthLocation.localName}\",,\n"
  }

  @Test
  fun `should produce CSV for probation bookings by booking date only`() {
    whenever(videoBookingEventRepository.findByDateOfBookingBetween(eq(false), any(), any())) doReturn Stream.of(risleyProbationBooking)

    service.probationBookingsByBookingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByDateOfBookingBetween(false, today(), tomorrow())
    verify(videoBookingEventRepository, never()).findByMainDateBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,probationTeam,probationTeamId,madeByProbation,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "1,2024-07-01T09:00:00,1,CREATE,$RISLEY,\"probation team description\",\"probation code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,,,,,\"${risleyLocation.localName}\",,\n"
  }
}
