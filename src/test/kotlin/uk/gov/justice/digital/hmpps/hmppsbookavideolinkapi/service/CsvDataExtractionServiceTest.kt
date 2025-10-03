package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationScheduleUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingEventRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.util.UUID
import java.util.stream.Stream

class CsvDataExtractionServiceTest {
  private val videoBookingEventRepository: VideoBookingEventRepository = mock()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val locationsService: LocationsService = mock()
  private val prisonsService: PrisonsService = mock()
  private val courtsService: CourtsService = mock()
  private val probationTeamsService: ProbationTeamsService = mock()

  private val service = CsvDataExtractionService(
    videoBookingEventRepository,
    locationsInsidePrisonClient,
    locationsService,
    prisonsService,
    courtsService,
    probationTeamsService,
  )

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
    type = "Appeal",
    user = "court_user",
    cvpLink = "cvp-link",
  )
  private val wandsworthCancelCourtBookingEvent = wandsworthCreateCourtBookingEvent.copy(eventType = "CANCEL")
  private val wandsworthAmendCourtBookingEvent = wandsworthCreateCourtBookingEvent.copy(eventType = "AMEND")
  private val wandsworthCreateProbationBookingEvent = wandsworthCreateCourtBookingEvent.copy(courtDescription = null, courtCode = null, probationTeamCode = "probation code", probationTeamDescription = "probation team description", courtBooking = false, type = "Pre-sentence report", user = "probation_user", probationOfficerName = "wandsworth officer name", probationOfficerEmail = "wandsworth_officer@email.com")
  private val wandsworthCancelProbationBooking = wandsworthCreateProbationBookingEvent.copy(eventType = "CANCEL")
  private val wandsworthAmendProbationBooking = wandsworthCreateProbationBookingEvent.copy(eventType = "AMEND")
  private val wandsworthPrisonCourtBooking = wandsworthCreateCourtBookingEvent.copy(createdByPrison = true)
  private val risleyCourtBooking = wandsworthCreateCourtBookingEvent.copy(prisonCode = RISLEY, eventType = "CANCEL", mainLocationId = risleyLocation.id, preLocationId = risleyLocation.id, postLocationId = risleyLocation.id)
  private val risleyPrisonCourtBooking = risleyCourtBooking.copy(createdByPrison = true, courtBooking = false)
  private val risleyProbationBooking = wandsworthCreateProbationBookingEvent.copy(prisonCode = RISLEY, createdByPrison = false, mainLocationId = risleyLocation.id, probationOfficerName = "risley officer name", probationOfficerEmail = "risley_officer@email.com")

  @BeforeEach
  fun before() {
    locationsInsidePrisonClient.stub {
      on { getNonResidentialAppointmentLocationsAtPrison(WANDSWORTH) } doReturn listOf(wandsworthLocation)
      on { getNonResidentialAppointmentLocationsAtPrison(RISLEY) } doReturn listOf(risleyLocation)
    }
  }

  @Test
  fun `should produce CSV for court court bookings by hearing date only`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(eq(true), any(), any())) doReturn Stream.of(wandsworthCreateCourtBookingEvent)

    service.courtBookingsByHearingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(true, today(), tomorrow())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName,hearingType,user,cvpLink\n" +
      "1,2024-07-01T09:00:00,1,CREATE,$WANDSWORTH,\"court description\",\"court code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",Appeal,court_user,cvp-link\n"
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
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName,hearingType,user,cvpLink\n" +
      "1,2024-07-01T09:00:00,1,CREATE,$WANDSWORTH,\"court description\",\"court code\",false,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",Appeal,court_user,cvp-link\n"
  }

  @Test
  fun `should produce CSV for court court bookings by booking date`() {
    whenever(videoBookingEventRepository.findByDateOfBookingBetween(eq(true), any(), any())) doReturn Stream.of(risleyCourtBooking)

    service.courtBookingsByBookingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByDateOfBookingBetween(true, today(), tomorrow())
    verify(videoBookingEventRepository, never()).findByMainDateBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName,hearingType,user,cvpLink\n" +
      "1,2024-07-01T09:00:00,1,DELETE,$RISLEY,\"court description\",\"court code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${risleyLocation.localName}\",\"${risleyLocation.localName}\",\"${risleyLocation.localName}\",Appeal,court_user,cvp-link\n"
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
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName,hearingType,user,cvpLink\n" +
      "1,2024-07-01T09:00:00,1,DELETE,$RISLEY,\"court description\",\"court code\",false,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${risleyLocation.localName}\",\"${risleyLocation.localName}\",\"${risleyLocation.localName}\",Appeal,court_user,cvp-link\n"
  }

  @Test
  fun `should replace CANCEL with DELETE for court booking event`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(eq(true), any(), any())) doReturn Stream.of(wandsworthCancelCourtBookingEvent)

    service.courtBookingsByHearingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(any(), any(), any())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName,hearingType,user,cvpLink\n" +
      "1,2024-07-01T09:00:00,1,DELETE,$WANDSWORTH,\"court description\",\"court code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",Appeal,court_user,cvp-link\n"
  }

  @Test
  fun `should replace AMEND with UPDATE for court booking event`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(eq(true), any(), any())) doReturn Stream.of(wandsworthAmendCourtBookingEvent)

    service.courtBookingsByHearingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(any(), any(), any())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName,hearingType,user,cvpLink\n" +
      "1,2024-07-01T09:00:00,1,UPDATE,$WANDSWORTH,\"court description\",\"court code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T09:00:00,2024-07-10T10:00:00,2024-07-10T11:00:00,2024-07-10T12:00:00,\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",\"${wandsworthLocation.localName}\",Appeal,court_user,cvp-link\n"
  }

  @Test
  fun `should produce CSV for probation bookings by meeting date only`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(eq(false), any(), any())) doReturn Stream.of(wandsworthCreateProbationBookingEvent)

    service.probationBookingsByMeetingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(false, today(), tomorrow())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,probationTeam,probationTeamId,madeByProbation,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName,meetingType,user,probationOfficerName,probationOfficerEmail\n" +
      "1,2024-07-01T09:00:00,1,CREATE,$WANDSWORTH,\"probation team description\",\"probation code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,,,,,\"${wandsworthLocation.localName}\",,,\"Pre-sentence report\",probation_user,\"wandsworth officer name\",\"wandsworth_officer@email.com\"\n"
  }

  @Test
  fun `should replace CANCEL with DELETE for probation booking event`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(any(), any(), any())) doReturn Stream.of(wandsworthCancelProbationBooking)

    service.probationBookingsByMeetingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(any(), any(), any())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,probationTeam,probationTeamId,madeByProbation,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName,meetingType,user,probationOfficerName,probationOfficerEmail\n" +
      "1,2024-07-01T09:00:00,1,DELETE,$WANDSWORTH,\"probation team description\",\"probation code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,,,,,\"${wandsworthLocation.localName}\",,,\"Pre-sentence report\",probation_user,\"wandsworth officer name\",\"wandsworth_officer@email.com\"\n"
  }

  @Test
  fun `should replace AMEND with UPDATE for probation booking event`() {
    whenever(videoBookingEventRepository.findByMainDateBetween(any(), any(), any())) doReturn Stream.of(wandsworthAmendProbationBooking)

    service.probationBookingsByMeetingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByMainDateBetween(any(), any(), any())
    verify(videoBookingEventRepository, never()).findByDateOfBookingBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,probationTeam,probationTeamId,madeByProbation,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName,meetingType,user,probationOfficerName,probationOfficerEmail\n" +
      "1,2024-07-01T09:00:00,1,UPDATE,$WANDSWORTH,\"probation team description\",\"probation code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,,,,,\"${wandsworthLocation.localName}\",,,\"Pre-sentence report\",probation_user,\"wandsworth officer name\",\"wandsworth_officer@email.com\"\n"
  }

  @Test
  fun `should produce CSV for probation bookings by booking date only`() {
    whenever(videoBookingEventRepository.findByDateOfBookingBetween(eq(false), any(), any())) doReturn Stream.of(risleyProbationBooking)

    service.probationBookingsByBookingDateToCsv(today(), tomorrow(), csvOutputStream)

    verify(videoBookingEventRepository).findByDateOfBookingBetween(false, today(), tomorrow())
    verify(videoBookingEventRepository, never()).findByMainDateBetween(any(), any(), any())

    csvOutputStream.toString() isEqualTo
      "eventId,timestamp,videoLinkBookingId,eventType,agencyId,probationTeam,probationTeamId,madeByProbation,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName,meetingType,user,probationOfficerName,probationOfficerEmail\n" +
      "1,2024-07-01T09:00:00,1,CREATE,$RISLEY,\"probation team description\",\"probation code\",true,2024-07-10T10:00:00,2024-07-10T11:00:00,,,,,\"${risleyLocation.localName}\",,,\"Pre-sentence report\",probation_user,\"risley officer name\",risley_officer@email.com\n"
  }

  @Test
  fun `should produce CSV for prison room configuration`() {
    whenever(courtsService.getCourts(eq(true))) doReturn listOfCourts
    whenever(probationTeamsService.getProbationTeams(eq(true))) doReturn listOfTeams
    whenever(prisonsService.getListOfPrisons(eq(true))) doReturn listOfPrisons
    whenever(locationsService.getVideoLinkLocationsAtPrison(eq("MDI"), eq(true))) doReturn locationsAtMoorland

    service.prisonRoomConfigurationToCsv(csvOutputStream)

    verify(courtsService).getCourts(eq(true))
    verify(probationTeamsService).getProbationTeams(eq(true))
    verify(prisonsService).getListOfPrisons(eq(true))
    verify(locationsService).getVideoLinkLocationsAtPrison(eq("MDI"), eq(true))

    csvOutputStream.toString() isEqualTo
      "prisonCode,prisonDescription,roomKey,roomDescription,roomVideoLink,roomSetup,roomStatus,permission,allowedParties,schedule\n" +
      "MDI,\"HMP Moorland\",MDI-RM-1,\"Room 1\",/video-link-url,Customised,Active,Schedule,,\"Monday-Tuesday 10:00-11:00 Court CourtA\"\n" +
      "MDI,\"HMP Moorland\",MDI-RM-1,\"Room 1\",/video-link-url,Customised,Active,Schedule,,\"Wednesday-Thursday 10:00-11:00 Probation TeamA\"\n" +
      "MDI,\"HMP Moorland\",MDI-RM-2,\"Room 2\",/video-link-url,Customised,Active,Schedule,,\"Monday-Tuesday 10:00-11:00 Court CourtA\"\n" +
      "MDI,\"HMP Moorland\",MDI-RM-2,\"Room 2\",/video-link-url,Customised,Active,Schedule,,\"Wednesday-Thursday 10:00-11:00 Probation TeamA\"\n" +
      "MDI,\"HMP Moorland\",MDI-RM-3,\"Room 3\",/video-link-url,Customised,Active,Court,CourtA,No\n" +
      "MDI,\"HMP Moorland\",MDI-RM-4,\"Room 4\",/video-link-url,Customised,Active,Shared,,No\n" +
      "MDI,\"HMP Moorland\",MDI-RM-5,\"Room 5\",/video-link-url,Customised,Active,Probation,,No\n" +
      "MDI,\"HMP Moorland\",MDI-RM-6,\"Room 6\",/video-link-url,Customised,Active,Court,,No\n" +
      "MDI,\"HMP Moorland\",MDI-RM-7,\"Room 7\",/video-link-url,Customised,Active,Probation,TeamA:TeamB,No\n" +
      "MDI,\"HMP Moorland\",MDI-RM-8,\"Room 8\",/video-link-url,Customised,\"Out of use\",Court,,No\n" +
      "MDI,\"HMP Moorland\",MDI-RM-9,\"Room 9\",/video-link-url,Customised,\"Temporarily blocked\",Shared,,No\n"
  }

  private val listOfPrisons = listOf(
    Prison(prisonId = 1L, code = "MDI", name = "HMP Moorland", enabled = true, notes = null),
  )

  private val roomSchedules = listOf(
    RoomSchedule(
      scheduleId = 1L,
      startDayOfWeek = DayOfWeek.MONDAY,
      endDayOfWeek = DayOfWeek.TUESDAY,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      locationUsage = LocationScheduleUsage.COURT,
      allowedParties = listOf("A"),
    ),
    RoomSchedule(
      scheduleId = 2L,
      startDayOfWeek = DayOfWeek.WEDNESDAY,
      endDayOfWeek = DayOfWeek.THURSDAY,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      locationUsage = LocationScheduleUsage.PROBATION,
      allowedParties = listOf("A"),
    ),
  )

  private val roomAttributes1 = RoomAttributes(
    attributeId = 1L,
    locationStatus = LocationStatus.ACTIVE,
    locationUsage = LocationUsage.SCHEDULE,
    prisonVideoUrl = "/video-link-url",
    allowedParties = emptyList(),
    notes = null,
    statusMessage = null,
    schedule = roomSchedules,
  )

  private val roomAttributes2 = roomAttributes1.copy(attributeId = 2L)
  private val roomAttributes3 = roomAttributes1.copy(attributeId = 3L, schedule = emptyList(), locationUsage = LocationUsage.COURT, allowedParties = listOf("A"))
  private val roomAttributes4 = roomAttributes1.copy(attributeId = 4L, schedule = emptyList(), locationUsage = LocationUsage.SHARED)
  private val roomAttributes5 = roomAttributes1.copy(attributeId = 5L, schedule = emptyList(), locationUsage = LocationUsage.PROBATION)
  private val roomAttributes6 = roomAttributes1.copy(attributeId = 6L, schedule = emptyList(), locationUsage = LocationUsage.COURT)
  private val roomAttributes7 = roomAttributes1.copy(attributeId = 7L, schedule = emptyList(), locationUsage = LocationUsage.PROBATION, allowedParties = listOf("A", "B"))
  private val roomAttributes8 = roomAttributes1.copy(attributeId = 8L, schedule = emptyList(), locationUsage = LocationUsage.COURT, locationStatus = LocationStatus.INACTIVE)
  private val roomAttributes9 = roomAttributes1.copy(attributeId = 9L, schedule = emptyList(), locationUsage = LocationUsage.SHARED, locationStatus = LocationStatus.TEMPORARILY_BLOCKED, blockedFrom = today(), blockedTo = today())

  private val locationsAtMoorland = listOf(
    Location(dpsLocationId = UUID.randomUUID(), key = "MDI-RM-1", prisonCode = "MDI", description = "Room 1", enabled = true, extraAttributes = roomAttributes1),
    Location(dpsLocationId = UUID.randomUUID(), key = "MDI-RM-2", prisonCode = "MDI", description = "Room 2", enabled = true, extraAttributes = roomAttributes2),
    Location(dpsLocationId = UUID.randomUUID(), key = "MDI-RM-3", prisonCode = "MDI", description = "Room 3", enabled = true, extraAttributes = roomAttributes3),
    Location(dpsLocationId = UUID.randomUUID(), key = "MDI-RM-4", prisonCode = "MDI", description = "Room 4", enabled = true, extraAttributes = roomAttributes4),
    Location(dpsLocationId = UUID.randomUUID(), key = "MDI-RM-5", prisonCode = "MDI", description = "Room 5", enabled = true, extraAttributes = roomAttributes5),
    Location(dpsLocationId = UUID.randomUUID(), key = "MDI-RM-6", prisonCode = "MDI", description = "Room 6", enabled = true, extraAttributes = roomAttributes6),
    Location(dpsLocationId = UUID.randomUUID(), key = "MDI-RM-7", prisonCode = "MDI", description = "Room 7", enabled = true, extraAttributes = roomAttributes7),
    Location(dpsLocationId = UUID.randomUUID(), key = "MDI-RM-8", prisonCode = "MDI", description = "Room 8", enabled = true, extraAttributes = roomAttributes8),
    Location(dpsLocationId = UUID.randomUUID(), key = "MDI-RM-9", prisonCode = "MDI", description = "Room 9", enabled = true, extraAttributes = roomAttributes9),
  )

  private val listOfCourts = listOf(
    Court(1L, "A", "CourtA", true, ""),
    Court(2L, "B", "CourtB", true, ""),
    Court(3L, "C", "CourtC", true, ""),
  )

  private val listOfTeams = listOf(
    ProbationTeam(1L, "A", "TeamA", true, ""),
    ProbationTeam(2L, "B", "TeamB", true, ""),
    ProbationTeam(3L, "C", "TeamC", true, ""),
  )

  @Test
  fun `Court codes are mapped to names for allowed parties on room attributes`() {
    val parties = listOf("A", "B", "C")
    val locationUsageCourt = LocationUsage.COURT
    val courts = mapOf("A" to "AAA", "B" to "BBB", "C" to "CCC")

    val response = allowedPartiesToString(allowedParties = parties, usage = locationUsageCourt, courts = courts)

    assertThat(response).isEqualTo("AAA:BBB:CCC")
  }

  @Test
  fun `Probation team codes are mapped to names for allowed parties on room attributes`() {
    val parties = listOf("A", "B", "C")
    val locationUsageProbation = LocationUsage.PROBATION
    val teams = mapOf("A" to "AAA", "B" to "BBB", "C" to "CCC")

    val response = allowedPartiesToString(allowedParties = parties, usage = locationUsageProbation, teams = teams)

    assertThat(response).isEqualTo("AAA:BBB:CCC")
  }

  @Test
  fun `Shared rooms will not show any allowed parties`() {
    val parties = listOf("A", "B", "C")
    val locationUsageShared = LocationUsage.SHARED
    val courts = mapOf("A" to "AAA", "B" to "BBB", "C" to "CCC")
    val teams = mapOf("A" to "AAA", "B" to "BBB", "C" to "CCC")

    val response = allowedPartiesToString(allowedParties = parties, usage = locationUsageShared, courts = courts, teams = teams)

    assertThat(response).isEqualTo("")
  }

  @Test
  fun `Room schedule for court is formatted correctly`() {
    val roomSchedule = RoomScheduleWithDpsId(
      dpsLocationId = UUID.randomUUID(),
      roomSchedule = RoomSchedule(
        scheduleId = 1L,
        startDayOfWeek = DayOfWeek.MONDAY,
        endDayOfWeek = DayOfWeek.SUNDAY,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        locationUsage = LocationScheduleUsage.COURT,
        allowedParties = listOf("A", "B", "C"),
      ),
    )

    val courts = mapOf("A" to "AAA", "B" to "BBB", "C" to "CCC")

    val response = scheduleToString(schedule = roomSchedule, courts = courts)

    assertThat(response).isEqualTo("Monday-Sunday 09:00-17:00 Court AAA:BBB:CCC")
  }

  @Test
  fun `Room schedule for probation is formatted correctly`() {
    val roomSchedule = RoomScheduleWithDpsId(
      dpsLocationId = UUID.randomUUID(),
      roomSchedule = RoomSchedule(
        scheduleId = 1L,
        startDayOfWeek = DayOfWeek.MONDAY,
        endDayOfWeek = DayOfWeek.SUNDAY,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        locationUsage = LocationScheduleUsage.PROBATION,
        allowedParties = listOf("A", "B", "C"),
      ),
    )

    val teams = mapOf("A" to "AAA", "B" to "BBB", "C" to "CCC")

    val response = scheduleToString(schedule = roomSchedule, teams = teams)

    assertThat(response).isEqualTo("Monday-Sunday 09:00-17:00 Probation AAA:BBB:CCC")
  }

  @Test
  fun `Room schedule for blocked is formatted correctly`() {
    val roomSchedule = RoomScheduleWithDpsId(
      dpsLocationId = UUID.randomUUID(),
      roomSchedule = RoomSchedule(
        scheduleId = 1L,
        startDayOfWeek = DayOfWeek.MONDAY,
        endDayOfWeek = DayOfWeek.SUNDAY,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        locationUsage = LocationScheduleUsage.BLOCKED,
        allowedParties = emptyList(),
      ),
    )

    val response = scheduleToString(schedule = roomSchedule)

    assertThat(response).isEqualTo("Monday-Sunday 09:00-17:00 Blocked ")
  }
}
