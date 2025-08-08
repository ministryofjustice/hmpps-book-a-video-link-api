package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentAttendeeSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TestEmailConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.TEAM_NOT_LISTED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAmendedBy
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAppointmentDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasAppointmentTypeProbation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasBookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasCreatedBy
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasCreatedByPrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasCreatedTimeCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasEndTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasHistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasNotesForPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasNotesForStaff
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasPrisonCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasStartTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AdditionalBookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ContextConfiguration(classes = [TestEmailConfiguration::class])
@Sql("classpath:integration-test-data/seed-probation-team-user-access.sql")
class ProbationBookingIntegrationTest : SqsIntegrationTestBase() {

  @MockitoBean
  private lateinit var activitiesAppointmentsClient: ActivitiesAppointmentsClient

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var prisonAppointmentRepository: PrisonAppointmentRepository

  @Autowired
  private lateinit var bookingHistoryRepository: BookingHistoryRepository

  @Autowired
  private lateinit var additionalBookingDetailRepository: AdditionalBookingDetailRepository

  private val psrProbationBookingRequest = probationBookingRequest(
    probationTeamCode = BLACKPOOL_MC_PPOC,
    probationMeetingType = ProbationMeetingType.PSR,
    prisonCode = BIRMINGHAM,
    prisonerNumber = "123456",
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(9, 30),
    appointmentType = AppointmentType.VLB_PROBATION,
    location = birminghamLocation,
    notesForStaff = "psr integration test probation staff notes",
    notesForPrisoners = "psr integration test probation prisoner notes",
    additionalBookingDetails = AdditionalBookingDetails(
      contactName = "psr probation contact",
      contactEmail = "psr_probation_contact@email.com",
      contactNumber = null,
    ),
  )

  private val rrProbationBookingRequest = probationBookingRequest(
    probationTeamCode = BLACKPOOL_MC_PPOC,
    probationMeetingType = ProbationMeetingType.RR,
    prisonCode = BIRMINGHAM,
    prisonerNumber = "123456",
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(12, 30),
    appointmentType = AppointmentType.VLB_PROBATION,
    location = birminghamLocation,
    additionalBookingDetails = AdditionalBookingDetails(
      contactName = "rr probation contact",
      contactEmail = "rr_probation_contact@email.com",
      contactNumber = null,
    ),
    notesForStaff = "rr integration test probation staff notes",
    notesForPrisoners = "rr integration test probation prisoner notes",
  )

  private val otherProbationBookingRequest = probationBookingRequest(
    probationTeamCode = BLACKPOOL_MC_PPOC,
    probationMeetingType = ProbationMeetingType.OTHER,
    prisonCode = BIRMINGHAM,
    prisonerNumber = "654321",
    startTime = LocalTime.of(14, 0),
    endTime = LocalTime.of(15, 30),
    appointmentType = AppointmentType.VLB_PROBATION,
    location = birminghamLocation,
    additionalBookingDetails = AdditionalBookingDetails(
      contactName = "other probation contact",
      contactEmail = "other_probation_contact@email.com",
      contactNumber = null,
    ),
    notesForStaff = "other integration test probation staff notes",
    notesForPrisoners = "other integration test probation prisoner notes",
  )

  private val teamNotListedProbationBookingRequest = probationBookingRequest(
    probationTeamCode = TEAM_NOT_LISTED,
    probationMeetingType = ProbationMeetingType.PSR,
    prisonCode = BIRMINGHAM,
    prisonerNumber = "123456",
    startTime = LocalTime.of(16, 0),
    endTime = LocalTime.of(16, 30),
    appointmentType = AppointmentType.VLB_PROBATION,
    location = birminghamLocation,
    notesForStaff = "psr integration test probation staff notes",
    notesForPrisoners = "psr integration test probation prisoner notes",
    additionalBookingDetails = AdditionalBookingDetails(
      contactName = "psr probation contact",
      contactEmail = "psr_probation_contact@email.com",
      contactNumber = null,
    ),
  )

  @Test
  fun `should create a pre-sentence report probation booking using the VLPM appointment type`() {
    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    nomisMappingApi().stubGetNomisLocationMappingBy(birminghamLocation, 1)
    locationsInsidePrisonApi().stubGetLocationById(birminghamLocation)
    prisonApi().stubGetScheduledAppointments(BIRMINGHAM, tomorrow(), 1)
    activitiesAppointmentsClient.stub { on { isAppointmentsRolledOutAt(BIRMINGHAM) } doReturn true }

    val bookingId = webTestClient.createBooking(psrProbationBookingRequest, PROBATION_USER)

    waitUntil {
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        startDate = tomorrow(),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 30),
        dpsLocationId = birminghamLocation.id,
        comments = null,
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    videoBookingRepository
      .findById(bookingId)
      .orElseThrow()
      .hasBookingType(BookingType.PROBATION)
      .hasMeetingType(ProbationMeetingType.PSR)
      .hasNotesForStaff("psr integration test probation staff notes")
      .hasNotesForPrisoner(null)
      .hasCreatedBy(PROBATION_USER)
      .hasCreatedTimeCloseTo(LocalDateTime.now())
      .hasCreatedByPrison(false)
      .also { it.probationTeam?.code isEqualTo BLACKPOOL_MC_PPOC }

    prisonAppointmentRepository
      .findByVideoBooking(persistedBooking)
      .single()
      .hasPrisonCode(BIRMINGHAM)
      .hasPrisonerNumber("123456")
      .hasAppointmentTypeProbation()
      .hasAppointmentDate(tomorrow())
      .hasStartTime(LocalTime.of(9, 0))
      .hasEndTime(LocalTime.of(9, 30))
      .hasLocation(birminghamLocation)
      .hasNotesForStaff("psr integration test probation staff notes")
      .hasNotesForPrisoner(null)

    bookingHistoryRepository
      .findAllByVideoBookingIdOrderByCreatedTime(persistedBooking.videoBookingId)
      .first()
      .hasHistoryType(HistoryType.CREATE)
      .hasProbationMeetingType(ProbationMeetingType.PSR)
      .hasProbationTeam(persistedBooking.probationTeam!!)
      .also { it.appointments() hasSize 1 }

    additionalBookingDetailRepository
      .findByVideoBooking(persistedBooking)!!
      .also { it.contactName isEqualTo "psr probation contact" }
      .also { it.contactEmail isEqualTo "psr_probation_contact@email.com" }
      .also { it.contactNumber isEqualTo null }
  }

  @Test
  fun `should amend a recall report report probation booking using the VLPM appointment type`() {
    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    nomisMappingApi().stubGetNomisLocationMappingBy(birminghamLocation, 1)
    locationsInsidePrisonApi().stubGetLocationById(birminghamLocation)
    prisonApi().stubGetScheduledAppointments(BIRMINGHAM, tomorrow(), 1)
    activitiesAppointmentsClient.stub { on { isAppointmentsRolledOutAt(BIRMINGHAM) } doReturn true }

    val bookingId = webTestClient.createBooking(rrProbationBookingRequest, PROBATION_USER)

    waitUntil {
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        startDate = tomorrow(),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(12, 30),
        dpsLocationId = birminghamLocation.id,
        comments = null,
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    videoBookingRepository
      .findById(bookingId)
      .orElseThrow()
      .hasBookingType(BookingType.PROBATION)
      .hasMeetingType(ProbationMeetingType.RR)
      .hasCreatedBy(PROBATION_USER)
      .hasCreatedTimeCloseTo(LocalDateTime.now())
      .hasCreatedByPrison(false)
      .hasNotesForStaff("rr integration test probation staff notes")
      .hasNotesForPrisoner(null)
      .also { it.probationTeam?.code isEqualTo BLACKPOOL_MC_PPOC }

    prisonAppointmentRepository
      .findByVideoBooking(persistedBooking)
      .single()
      .hasPrisonCode(BIRMINGHAM)
      .hasPrisonerNumber("123456")
      .hasAppointmentTypeProbation()
      .hasAppointmentDate(tomorrow())
      .hasStartTime(LocalTime.of(11, 0))
      .hasEndTime(LocalTime.of(12, 30))
      .hasLocation(birminghamLocation)
      .hasNotesForStaff("rr integration test probation staff notes")
      .hasNotesForPrisoner(null)

    bookingHistoryRepository
      .findAllByVideoBookingIdOrderByCreatedTime(persistedBooking.videoBookingId)
      .first()
      .hasHistoryType(HistoryType.CREATE)
      .hasProbationMeetingType(ProbationMeetingType.RR)
      .hasProbationTeam(persistedBooking.probationTeam!!)
      .also { it.appointments() hasSize 1 }

    activitiesAppointmentsClient.stub {
      on { isAppointmentsRolledOutAt(BIRMINGHAM) } doReturn true
      on { getPrisonersAppointments(prisonCode = BIRMINGHAM, prisonerNumber = "123456", onDate = tomorrow(), birminghamLocation.id) } doReturn listOf(
        appointmentSearchResult(date = tomorrow(), startTime = LocalTime.of(11, 0), endTime = LocalTime.of(12, 30), prisonCode = BIRMINGHAM, prisonerNumber = "123456", locationId = 1, appointmentType = SupportedAppointmentTypes.Type.PROBATION),
      )
    }

    webTestClient.amendBooking(
      persistedBooking.videoBookingId,
      amendProbationBookingRequest(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        probationMeetingType = ProbationMeetingType.RR,
        location = birminghamLocation,
        appointmentDate = tomorrow(),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 0),
        notesForStaff = "rr integration test probation staff notes amended",
        additionalBookingDetails = AdditionalBookingDetails(
          contactName = "rr probation contact",
          contactEmail = "rr_probation_contact@email.com",
          contactNumber = null,
        ),
      ),
      PROBATION_USER,
    )

    waitUntil {
      verify(activitiesAppointmentsClient).patchAppointment(
        appointmentId = 99,
        startDate = tomorrow(),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 0),
        dpsLocationId = birminghamLocation.id,
        comments = null,
      )
    }

    videoBookingRepository
      .findById(bookingId)
      .orElseThrow()
      .hasBookingType(BookingType.PROBATION)
      .hasMeetingType(ProbationMeetingType.RR)
      .hasNotesForStaff("rr integration test probation staff notes amended")
      .hasNotesForPrisoner(null)
      .hasCreatedBy(PROBATION_USER)
      .hasCreatedTimeCloseTo(LocalDateTime.now())
      .hasCreatedByPrison(false)
      .also { it.probationTeam?.code isEqualTo BLACKPOOL_MC_PPOC }

    prisonAppointmentRepository
      .findByVideoBooking(persistedBooking)
      .single()
      .hasPrisonCode(BIRMINGHAM)
      .hasPrisonerNumber("123456")
      .hasAppointmentTypeProbation()
      .hasAppointmentDate(tomorrow())
      .hasStartTime(LocalTime.of(12, 0))
      .hasEndTime(LocalTime.of(13, 0))
      .hasLocation(birminghamLocation)
      .hasNotesForStaff("rr integration test probation staff notes amended")
      .hasNotesForPrisoner(null)

    additionalBookingDetailRepository
      .findByVideoBooking(persistedBooking)!!
      .also { it.contactName isEqualTo "rr probation contact" }
      .also { it.contactEmail isEqualTo "rr_probation_contact@email.com" }
      .also { it.contactNumber isEqualTo null }
  }

  @Test
  fun `should remove existing details on probation booking using the VLPM appointment type`() {
    prisonSearchApi().stubGetPrisoner("654321", BIRMINGHAM)
    nomisMappingApi().stubGetNomisLocationMappingBy(birminghamLocation, 1)
    locationsInsidePrisonApi().stubGetLocationById(birminghamLocation)
    prisonApi().stubGetScheduledAppointments(BIRMINGHAM, tomorrow(), 1)
    activitiesAppointmentsClient.stub { on { isAppointmentsRolledOutAt(BIRMINGHAM) } doReturn true }

    val bookingId = webTestClient.createBooking(otherProbationBookingRequest, PROBATION_USER)

    waitUntil {
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "654321",
        startDate = tomorrow(),
        startTime = LocalTime.of(14, 0),
        endTime = LocalTime.of(15, 30),
        dpsLocationId = birminghamLocation.id,
        comments = null,
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    videoBookingRepository
      .findById(bookingId)
      .orElseThrow()
      .hasBookingType(BookingType.PROBATION)
      .hasMeetingType(ProbationMeetingType.OTHER)
      .hasNotesForStaff("other integration test probation staff notes")
      .hasNotesForPrisoner(null)
      .hasCreatedBy(PROBATION_USER)
      .hasCreatedTimeCloseTo(LocalDateTime.now())
      .hasCreatedByPrison(false)
      .also { it.probationTeam?.code isEqualTo BLACKPOOL_MC_PPOC }

    prisonAppointmentRepository
      .findByVideoBooking(persistedBooking)
      .single()
      .hasPrisonCode(BIRMINGHAM)
      .hasPrisonerNumber("654321")
      .hasAppointmentTypeProbation()
      .hasAppointmentDate(tomorrow())
      .hasStartTime(LocalTime.of(14, 0))
      .hasEndTime(LocalTime.of(15, 30))
      .hasLocation(birminghamLocation)
      .hasNotesForStaff("other integration test probation staff notes")
      .hasNotesForPrisoner(null)

    bookingHistoryRepository
      .findAllByVideoBookingIdOrderByCreatedTime(persistedBooking.videoBookingId)
      .first()
      .hasHistoryType(HistoryType.CREATE)
      .hasProbationMeetingType(ProbationMeetingType.OTHER)
      .hasProbationTeam(persistedBooking.probationTeam!!)
      .also { it.appointments() hasSize 1 }

    additionalBookingDetailRepository
      .findByVideoBooking(persistedBooking)!!
      .also { it.contactName isEqualTo "other probation contact" }
      .also { it.contactEmail isEqualTo "other_probation_contact@email.com" }
      .also { it.contactNumber isEqualTo null }

    activitiesAppointmentsClient.stub {
      on { isAppointmentsRolledOutAt(BIRMINGHAM) } doReturn true
      on { getPrisonersAppointments(prisonCode = BIRMINGHAM, prisonerNumber = "654321", onDate = tomorrow(), birminghamLocation.id) } doReturn listOf(
        appointmentSearchResult(date = tomorrow(), startTime = LocalTime.of(14, 0), endTime = LocalTime.of(15, 30), prisonCode = BIRMINGHAM, prisonerNumber = "654321", locationId = 1, appointmentType = SupportedAppointmentTypes.Type.PROBATION),
      )
    }

    webTestClient.amendBooking(
      persistedBooking.videoBookingId,
      amendProbationBookingRequest(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "654321",
        probationMeetingType = ProbationMeetingType.OTHER,
        location = birminghamLocation,
        appointmentDate = tomorrow(),
        startTime = LocalTime.of(14, 30),
        endTime = LocalTime.of(15, 30),
        notesForStaff = "other integration test probation staff notes amended",
        notesForPrisoners = "other integration test probation prisoner notes amended",
        additionalBookingDetails = null,
      ),
      PRISON_USER_BIRMINGHAM,
    )

    waitUntil {
      verify(activitiesAppointmentsClient).patchAppointment(
        appointmentId = 99,
        startDate = tomorrow(),
        startTime = LocalTime.of(14, 30),
        endTime = LocalTime.of(15, 30),
        dpsLocationId = birminghamLocation.id,
        comments = "other integration test probation prisoner notes amended",
      )
    }

    videoBookingRepository
      .findById(bookingId)
      .orElseThrow()
      .hasBookingType(BookingType.PROBATION)
      .hasMeetingType(ProbationMeetingType.OTHER)
      .hasNotesForStaff("other integration test probation staff notes amended")
      .hasNotesForPrisoner("other integration test probation prisoner notes amended")
      .hasCreatedBy(PROBATION_USER)
      .hasAmendedBy(PRISON_USER_BIRMINGHAM)
      .hasCreatedTimeCloseTo(LocalDateTime.now())
      .hasCreatedByPrison(false)
      .also { it.probationTeam?.code isEqualTo BLACKPOOL_MC_PPOC }

    prisonAppointmentRepository
      .findByVideoBooking(persistedBooking)
      .single()
      .hasPrisonCode(BIRMINGHAM)
      .hasPrisonerNumber("654321")
      .hasAppointmentTypeProbation()
      .hasAppointmentDate(tomorrow())
      .hasStartTime(LocalTime.of(14, 30))
      .hasEndTime(LocalTime.of(15, 30))
      .hasLocation(birminghamLocation)
      .hasNotesForStaff("other integration test probation staff notes amended")
      .hasNotesForPrisoner("other integration test probation prisoner notes amended")

    additionalBookingDetailRepository.findByVideoBooking(persistedBooking) isEqualTo null
  }

  @Test
  fun `should create a probation booking for team not listed`() {
    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    nomisMappingApi().stubGetNomisLocationMappingBy(birminghamLocation, 1)
    locationsInsidePrisonApi().stubGetLocationById(birminghamLocation)
    prisonApi().stubGetScheduledAppointments(BIRMINGHAM, tomorrow(), 1)
    activitiesAppointmentsClient.stub { on { isAppointmentsRolledOutAt(BIRMINGHAM) } doReturn true }

    val bookingId = webTestClient.createBooking(teamNotListedProbationBookingRequest, PRISON_USER_BIRMINGHAM)

    waitUntil {
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        startDate = tomorrow(),
        startTime = LocalTime.of(16, 0),
        endTime = LocalTime.of(16, 30),
        dpsLocationId = birminghamLocation.id,
        comments = "psr integration test probation prisoner notes",
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    videoBookingRepository
      .findById(bookingId)
      .orElseThrow()
      .hasBookingType(BookingType.PROBATION)
      .hasMeetingType(ProbationMeetingType.PSR)
      .hasNotesForStaff("psr integration test probation staff notes")
      .hasNotesForPrisoner("psr integration test probation prisoner notes")
      .hasCreatedBy(PRISON_USER_BIRMINGHAM)
      .hasCreatedTimeCloseTo(LocalDateTime.now())
      .hasCreatedByPrison(true)
      .also { it.probationTeam?.code isEqualTo TEAM_NOT_LISTED }

    prisonAppointmentRepository
      .findByVideoBooking(persistedBooking)
      .single()
      .hasPrisonCode(BIRMINGHAM)
      .hasPrisonerNumber("123456")
      .hasAppointmentTypeProbation()
      .hasAppointmentDate(tomorrow())
      .hasStartTime(LocalTime.of(16, 0))
      .hasEndTime(LocalTime.of(16, 30))
      .hasLocation(birminghamLocation)
      .hasNotesForStaff("psr integration test probation staff notes")
      .hasNotesForPrisoner("psr integration test probation prisoner notes")

    bookingHistoryRepository
      .findAllByVideoBookingIdOrderByCreatedTime(persistedBooking.videoBookingId)
      .first()
      .hasHistoryType(HistoryType.CREATE)
      .hasProbationMeetingType(ProbationMeetingType.PSR)
      .hasProbationTeam(persistedBooking.probationTeam!!)
      .also { it.appointments() hasSize 1 }

    additionalBookingDetailRepository
      .findByVideoBooking(persistedBooking)!!
      .also { it.contactName isEqualTo "psr probation contact" }
      .also { it.contactEmail isEqualTo "psr_probation_contact@email.com" }
      .also { it.contactNumber isEqualTo null }
  }

  private fun appointmentSearchResult(date: LocalDate, startTime: LocalTime, endTime: LocalTime, prisonCode: String, prisonerNumber: String, locationId: Long, appointmentType: SupportedAppointmentTypes.Type) = run {
    AppointmentSearchResult(
      appointmentType = AppointmentSearchResult.AppointmentType.INDIVIDUAL,
      startDate = date,
      startTime = startTime.toHourMinuteStyle(),
      endTime = endTime.toHourMinuteStyle(),
      isCancelled = false,
      isExpired = false,
      isEdited = false,
      appointmentId = 99,
      appointmentSeriesId = 1,
      appointmentName = "appointment name",
      attendees = listOf(AppointmentAttendeeSearchResult(1, prisonerNumber, 1)),
      category = AppointmentCategorySummary(appointmentType.code, "video link booking"),
      inCell = false,
      isRepeat = false,
      maxSequenceNumber = 1,
      prisonCode = prisonCode,
      sequenceNumber = 1,
      internalLocation = AppointmentLocationSummary(
        locationId,
        prisonCode,
        "VIDEO LINK",
      ),
      timeSlot = AppointmentSearchResult.TimeSlot.AM,
      createdTime = LocalDateTime.now(),
    )
  }
}
