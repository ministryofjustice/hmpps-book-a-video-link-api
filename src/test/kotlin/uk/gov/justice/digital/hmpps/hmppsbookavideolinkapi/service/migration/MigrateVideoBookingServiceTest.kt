package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.AppointmentLocationTimeSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoLinkBookingEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UNKNOWN_COURT_CODE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UNKNOWN_PROBATION_TEAM_CODE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.daysAgo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation3
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDate
import java.time.LocalTime

class MigrateVideoBookingServiceTest {
  private val migrateMappingService: MigrateMappingService = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val bookingHistoryRepository: BookingHistoryRepository = mock()
  private val service = MigrateVideoBookingService(migrateMappingService, videoBookingRepository, prisonRepository, bookingHistoryRepository)
  private val videoBookingCaptor = argumentCaptor<VideoBooking>()
  private val bookingHistoryCaptor = argumentCaptor<BookingHistory>()

  @BeforeEach
  fun setup() {
    whenever(prisonRepository.findByCode(PENTONVILLE)) doReturn prison(prisonCode = PENTONVILLE)
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(prisonCode = WANDSWORTH)
  }

  @Test
  fun `should migrate booking to a court booking with main appointment only`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocationId(1) } doReturn wandsworthLocation.id.toString()
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking().withMainCourtPrisonAppointment(prisonCode = WANDSWORTH)

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = WANDSWORTH,
        courtCode = DERBY_JUSTICE_CENTRE,
        courtName = null,
        probation = false,
        createdByUsername = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comment = "Migrated court comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            courtName = null,
            pre = null,
            main = AppointmentLocationTimeSlot(1, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            post = null,
            prisonCode = WANDSWORTH,
            createdByUsername = "MIGRATION COURT CREATE USER",
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())

    with(videoBookingCaptor.firstValue) {
      bookingType isEqualTo "COURT"
      hearingType isEqualTo "UNKNOWN"
      court isEqualTo court(DERBY_JUSTICE_CENTRE)
      comments isEqualTo "Migrated court comments"
      createdBy isEqualTo "MIGRATION COURT USER"
      migratedVideoBookingId isEqualTo 1
    }

    with(videoBookingCaptor.firstValue.appointments().single()) {
      prisonCode() isEqualTo WANDSWORTH
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_MAIN"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.MIDNIGHT
      endTime isEqualTo LocalTime.MIDNIGHT.plusHours(1)
      prisonLocationId isEqualTo wandsworthLocation.id.toString()
      comments isEqualTo "Migrated court comments"
    }
  }

  @Test
  fun `should migrate booking to an unknown court booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocationId(1) } doReturn wandsworthLocation.id.toString()
      on { mapCourtCodeToCourt(UNKNOWN_COURT_CODE) } doReturn court(UNKNOWN_COURT_CODE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking().withMainCourtPrisonAppointment(prisonCode = WANDSWORTH)

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = WANDSWORTH,
        courtCode = null,
        courtName = "Unknown court",
        probation = false,
        createdByUsername = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comment = "Migrated court comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = null,
            courtName = "Unknown court",
            pre = null,
            main = AppointmentLocationTimeSlot(1, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            post = null,
            prisonCode = WANDSWORTH,
            createdByUsername = "MIGRATION COURT CREATE USER",
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())

    with(videoBookingCaptor.firstValue) {
      bookingType isEqualTo "COURT"
      hearingType isEqualTo "UNKNOWN"
      court isEqualTo court(UNKNOWN_COURT_CODE)
      comments isEqualTo "Migrated court comments"
      createdBy isEqualTo "MIGRATION COURT USER"
      migratedVideoBookingId isEqualTo 1
      migratedDescription isEqualTo "Unknown court"
    }

    with(videoBookingCaptor.firstValue.appointments().single()) {
      prisonCode() isEqualTo WANDSWORTH
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_MAIN"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.MIDNIGHT
      endTime isEqualTo LocalTime.MIDNIGHT.plusHours(1)
      prisonLocationId isEqualTo wandsworthLocation.id.toString()
      comments isEqualTo "Migrated court comments"
    }
  }

  @Test
  fun `should migrate (other court) booking to an unknown court booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocationId(1) } doReturn wandsworthLocation.id.toString()
      on { mapCourtCodeToCourt(UNKNOWN_COURT_CODE) } doReturn court(UNKNOWN_COURT_CODE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking().withMainCourtPrisonAppointment(prisonCode = WANDSWORTH)

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = WANDSWORTH,
        courtCode = "other",
        courtName = "Free text court name",
        probation = false,
        createdByUsername = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comment = "Migrated court comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = "other",
            courtName = "Free text court name",
            pre = null,
            main = AppointmentLocationTimeSlot(1, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            post = null,
            prisonCode = WANDSWORTH,
            createdByUsername = "MIGRATION COURT CREATE USER",
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())

    with(videoBookingCaptor.firstValue) {
      bookingType isEqualTo "COURT"
      hearingType isEqualTo "UNKNOWN"
      court isEqualTo court(UNKNOWN_COURT_CODE)
      comments isEqualTo "Migrated court comments"
      createdBy isEqualTo "MIGRATION COURT USER"
      migratedVideoBookingId isEqualTo 1
      migratedDescription isEqualTo "Free text court name"
    }

    with(videoBookingCaptor.firstValue.appointments().single()) {
      prisonCode() isEqualTo WANDSWORTH
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_MAIN"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.MIDNIGHT
      endTime isEqualTo LocalTime.MIDNIGHT.plusHours(1)
      prisonLocationId isEqualTo wandsworthLocation.id.toString()
      comments isEqualTo "Migrated court comments"
    }
  }

  @Test
  fun `should migrate booking to a court booking with pre and main appointment`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocationId(1) } doReturn wandsworthLocation.id.toString()
      on { mapInternalLocationIdToLocationId(2) } doReturn wandsworthLocation2.id.toString()
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking().withMainCourtPrisonAppointment(prisonCode = WANDSWORTH)

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 2,
        offenderBookingId = 1,
        prisonCode = WANDSWORTH,
        courtCode = DERBY_JUSTICE_CENTRE,
        courtName = null,
        probation = false,
        createdByUsername = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comment = "Migrated court comments",
        pre = AppointmentLocationTimeSlot(1, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
        main = AppointmentLocationTimeSlot(2, today(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            courtName = null,
            pre = AppointmentLocationTimeSlot(1, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            main = AppointmentLocationTimeSlot(2, today(), LocalTime.of(10, 0), LocalTime.of(10, 0)),
            post = null,
            prisonCode = WANDSWORTH,
            createdByUsername = "MIGRATION COURT CREATE USER",
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())

    with(videoBookingCaptor.firstValue) {
      bookingType isEqualTo "COURT"
      hearingType isEqualTo "UNKNOWN"
      court isEqualTo court(DERBY_JUSTICE_CENTRE)
      comments isEqualTo "Migrated court comments"
      createdBy isEqualTo "MIGRATION COURT USER"
      migratedVideoBookingId isEqualTo 2
    }

    val appointments = videoBookingCaptor.firstValue.appointments().also { it hasSize 2 }

    with(appointments.component1()) {
      prisonCode() isEqualTo WANDSWORTH
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_PRE"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
      prisonLocationId isEqualTo wandsworthLocation.id.toString()
      comments isEqualTo "Migrated court comments"
    }

    with(appointments.component2()) {
      prisonCode() isEqualTo WANDSWORTH
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_MAIN"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 0)
      prisonLocationId isEqualTo wandsworthLocation2.id.toString()
      comments isEqualTo "Migrated court comments"
    }
  }

  @Test
  fun `should migrate booking to a court booking with pre, main and post appointment`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocationId(1) } doReturn wandsworthLocation.id.toString()
      on { mapInternalLocationIdToLocationId(2) } doReturn wandsworthLocation2.id.toString()
      on { mapInternalLocationIdToLocationId(3) } doReturn wandsworthLocation3.id.toString()
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking().withMainCourtPrisonAppointment(prisonCode = WANDSWORTH)

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 3,
        offenderBookingId = 1,
        prisonCode = WANDSWORTH,
        courtCode = DERBY_JUSTICE_CENTRE,
        courtName = null,
        probation = false,
        createdByUsername = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comment = "Migrated court comments",
        pre = AppointmentLocationTimeSlot(1, today(), LocalTime.of(8, 0), LocalTime.of(9, 0)),
        main = AppointmentLocationTimeSlot(2, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
        post = AppointmentLocationTimeSlot(3, today(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            courtName = null,
            pre = AppointmentLocationTimeSlot(1, today(), LocalTime.of(8, 0), LocalTime.of(9, 0)),
            main = AppointmentLocationTimeSlot(2, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            post = AppointmentLocationTimeSlot(3, today(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            prisonCode = WANDSWORTH,
            createdByUsername = "MIGRATION COURT CREATE USER",
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())

    with(videoBookingCaptor.firstValue) {
      bookingType isEqualTo "COURT"
      hearingType isEqualTo "UNKNOWN"
      court isEqualTo court(DERBY_JUSTICE_CENTRE)
      comments isEqualTo "Migrated court comments"
      createdBy isEqualTo "MIGRATION COURT USER"
      migratedVideoBookingId isEqualTo 3
    }

    val appointments = videoBookingCaptor.firstValue.appointments().also { it hasSize 3 }

    with(appointments.component1()) {
      prisonCode() isEqualTo WANDSWORTH
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_PRE"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(8, 0)
      endTime isEqualTo LocalTime.of(9, 0)
      prisonLocationId isEqualTo wandsworthLocation.id.toString()
      comments isEqualTo "Migrated court comments"
    }

    with(appointments.component2()) {
      prisonCode() isEqualTo WANDSWORTH
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_MAIN"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
      prisonLocationId isEqualTo wandsworthLocation2.id.toString()
      comments isEqualTo "Migrated court comments"
    }

    with(appointments.component3()) {
      prisonCode() isEqualTo WANDSWORTH
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_POST"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 0)
      prisonLocationId isEqualTo wandsworthLocation3.id.toString()
      comments isEqualTo "Migrated court comments"
    }
  }

  @Test
  fun `should migrate booking to a court booking with main and post appointment`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocationId(1) } doReturn wandsworthLocation.id.toString()
      on { mapInternalLocationIdToLocationId(2) } doReturn wandsworthLocation2.id.toString()
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking().withMainCourtPrisonAppointment(prisonCode = WANDSWORTH)

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 4,
        offenderBookingId = 1,
        prisonCode = WANDSWORTH,
        courtCode = DERBY_JUSTICE_CENTRE,
        courtName = null,
        probation = false,
        createdByUsername = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comment = "Migrated court comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, today(), LocalTime.of(8, 0), LocalTime.of(9, 0)),
        post = AppointmentLocationTimeSlot(2, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            pre = null,
            main = AppointmentLocationTimeSlot(1, today(), LocalTime.of(8, 0), LocalTime.of(9, 0)),
            post = AppointmentLocationTimeSlot(2, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            prisonCode = WANDSWORTH,
            createdByUsername = "MIGRATION COURT CREATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())

    with(videoBookingCaptor.firstValue) {
      bookingType isEqualTo "COURT"
      hearingType isEqualTo "UNKNOWN"
      court isEqualTo court(DERBY_JUSTICE_CENTRE)
      comments isEqualTo "Migrated court comments"
      createdBy isEqualTo "MIGRATION COURT USER"
      migratedVideoBookingId isEqualTo 4
    }

    val appointments = videoBookingCaptor.firstValue.appointments().also { it hasSize 2 }

    with(appointments.component1()) {
      prisonCode() isEqualTo WANDSWORTH
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_MAIN"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(8, 0)
      endTime isEqualTo LocalTime.of(9, 0)
      prisonLocationId isEqualTo wandsworthLocation.id.toString()
      comments isEqualTo "Migrated court comments"
    }

    with(appointments.component2()) {
      prisonCode() isEqualTo WANDSWORTH
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_POST"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
      prisonLocationId isEqualTo wandsworthLocation2.id.toString()
      comments isEqualTo "Migrated court comments"
    }
  }

  @Test
  fun `should migrate booking to a probation booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "DEF123"
      on { mapInternalLocationIdToLocationId(1) } doReturn pentonvilleLocation.id.toString()
      on { mapProbationTeamCodeToProbationTeam(BLACKPOOL_MC_PPOC) } doReturn probationTeam(BLACKPOOL_MC_PPOC)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isProbationBooking() })) doReturn probationBooking().withProbationPrisonAppointment(prisonCode = PENTONVILLE)

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 5,
        offenderBookingId = 1,
        prisonCode = PENTONVILLE,
        courtCode = BLACKPOOL_MC_PPOC,
        courtName = null,
        probation = true,
        createdByUsername = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comment = "Migrated probation comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            pre = null,
            main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
            post = null,
            prisonCode = PENTONVILLE,
            createdByUsername = "MIGRATION PROBATION CREATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())

    with(videoBookingCaptor.firstValue) {
      bookingType isEqualTo "PROBATION"
      probationMeetingType isEqualTo "UNKNOWN"
      probationTeam isEqualTo probationTeam(BLACKPOOL_MC_PPOC)
      comments isEqualTo "Migrated probation comments"
      createdBy isEqualTo "MIGRATION PROBATION USER"
      migratedVideoBookingId isEqualTo 5
    }

    with(videoBookingCaptor.firstValue.appointments().single()) {
      prisonCode() isEqualTo PENTONVILLE
      prisonerNumber isEqualTo "DEF123"
      appointmentType isEqualTo "VLB_PROBATION"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.MIDNIGHT
      endTime isEqualTo LocalTime.MIDNIGHT.plusHours(1)
      prisonLocationId isEqualTo pentonvilleLocation.id.toString()
      comments isEqualTo "Migrated probation comments"
    }
  }

  @Test
  fun `should migrate booking to an unknown probation booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "DEF123"
      on { mapInternalLocationIdToLocationId(1) } doReturn pentonvilleLocation.id.toString()
      on { mapProbationTeamCodeToProbationTeam(UNKNOWN_PROBATION_TEAM_CODE) } doReturn probationTeam(UNKNOWN_PROBATION_TEAM_CODE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isProbationBooking() })) doReturn probationBooking().withProbationPrisonAppointment(prisonCode = PENTONVILLE)

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 5,
        offenderBookingId = 1,
        prisonCode = PENTONVILLE,
        courtCode = null,
        courtName = "Unknown probation team",
        probation = true,
        createdByUsername = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comment = "Migrated probation comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = null,
            pre = null,
            main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
            post = null,
            prisonCode = PENTONVILLE,
            createdByUsername = "MIGRATION PROBATION CREATE USER",
            courtName = "Unknown probation team",
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())

    with(videoBookingCaptor.firstValue) {
      bookingType isEqualTo "PROBATION"
      probationMeetingType isEqualTo "UNKNOWN"
      probationTeam isEqualTo probationTeam(UNKNOWN_PROBATION_TEAM_CODE)
      comments isEqualTo "Migrated probation comments"
      createdBy isEqualTo "MIGRATION PROBATION USER"
      migratedVideoBookingId isEqualTo 5
      migratedDescription isEqualTo "Unknown probation team"
    }

    with(videoBookingCaptor.firstValue.appointments().single()) {
      prisonCode() isEqualTo PENTONVILLE
      prisonerNumber isEqualTo "DEF123"
      appointmentType isEqualTo "VLB_PROBATION"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.MIDNIGHT
      endTime isEqualTo LocalTime.MIDNIGHT.plusHours(1)
      prisonLocationId isEqualTo pentonvilleLocation.id.toString()
      comments isEqualTo "Migrated probation comments"
    }
  }

  @Test
  fun `should migrate (other court) booking to an unknown probation booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "DEF123"
      on { mapInternalLocationIdToLocationId(1) } doReturn pentonvilleLocation.id.toString()
      on { mapProbationTeamCodeToProbationTeam(UNKNOWN_PROBATION_TEAM_CODE) } doReturn probationTeam(UNKNOWN_PROBATION_TEAM_CODE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isProbationBooking() })) doReturn probationBooking().withProbationPrisonAppointment(prisonCode = PENTONVILLE)

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 5,
        offenderBookingId = 1,
        prisonCode = PENTONVILLE,
        courtCode = "OTHER",
        courtName = "Free text probation team name",
        probation = true,
        createdByUsername = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comment = "Migrated probation comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = "OTHER",
            pre = null,
            main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
            post = null,
            prisonCode = PENTONVILLE,
            createdByUsername = "MIGRATION PROBATION CREATE USER",
            courtName = "Free text probation team name",
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())

    with(videoBookingCaptor.firstValue) {
      bookingType isEqualTo "PROBATION"
      probationMeetingType isEqualTo "UNKNOWN"
      probationTeam isEqualTo probationTeam(UNKNOWN_PROBATION_TEAM_CODE)
      comments isEqualTo "Migrated probation comments"
      createdBy isEqualTo "MIGRATION PROBATION USER"
      migratedVideoBookingId isEqualTo 5
      migratedDescription isEqualTo "Free text probation team name"
    }

    with(videoBookingCaptor.firstValue.appointments().single()) {
      prisonCode() isEqualTo PENTONVILLE
      prisonerNumber isEqualTo "DEF123"
      appointmentType isEqualTo "VLB_PROBATION"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.MIDNIGHT
      endTime isEqualTo LocalTime.MIDNIGHT.plusHours(1)
      prisonLocationId isEqualTo pentonvilleLocation.id.toString()
      comments isEqualTo "Migrated probation comments"
    }
  }

  @Test
  fun `should migrate CREATE booking history for a court booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocationId(1) } doReturn wandsworthLocation.id.toString()
      on { mapInternalLocationIdToLocationId(2) } doReturn wandsworthLocation2.id.toString()
      on { mapInternalLocationIdToLocationId(3) } doReturn wandsworthLocation3.id.toString()
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking().withMainCourtPrisonAppointment(
      prisonCode = WANDSWORTH,
      prisonerNumber = "ABC123",
    )

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = WANDSWORTH,
        courtCode = DERBY_JUSTICE_CENTRE,
        courtName = null,
        probation = false,
        createdByUsername = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comment = "Migrated court comments",
        pre = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
        main = AppointmentLocationTimeSlot(2, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = AppointmentLocationTimeSlot(3, yesterday(), LocalTime.of(11, 0), LocalTime.of(12, 0)),
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "c".repeat(2000),
            courtCode = DERBY_JUSTICE_CENTRE,
            pre = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            main = AppointmentLocationTimeSlot(2, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = AppointmentLocationTimeSlot(3, yesterday(), LocalTime.of(11, 0), LocalTime.of(12, 0)),
            prisonCode = WANDSWORTH,
            createdByUsername = "MIGRATION COURT CREATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    inOrder(videoBookingRepository, bookingHistoryRepository) {
      verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())
      verify(bookingHistoryRepository).saveAndFlush(bookingHistoryCaptor.capture())
    }

    with(bookingHistoryCaptor.allValues.single()) {
      historyType isEqualTo HistoryType.CREATE
      createdBy isEqualTo "MIGRATION COURT CREATE USER"
      createdTime isEqualTo yesterday().atStartOfDay()
      // history comments should be truncated to 1000 chars
      comments isEqualTo "c".repeat(1000)

      appointments() hasSize 3

      appointments().component1()
        .isAtPrison(WANDSWORTH)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(9, 0))
        .endsAt(LocalTime.of(10, 0))
        .isForAppointmentType("VLB_COURT_PRE")
        .isAtLocation(wandsworthLocation)

      appointments().component2()
        .isAtPrison(WANDSWORTH)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_COURT_MAIN")
        .isAtLocation(wandsworthLocation2)

      appointments().component3()
        .isAtPrison(WANDSWORTH)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(11, 0))
        .endsAt(LocalTime.of(12, 0))
        .isForAppointmentType("VLB_COURT_POST")
        .isAtLocation(wandsworthLocation3)
    }
  }

  @Test
  fun `should migrate CANCELLED booking history for a court booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocationId(1) } doReturn wandsworthLocation.id.toString()
      on { mapInternalLocationIdToLocationId(2) } doReturn wandsworthLocation2.id.toString()
      on { mapInternalLocationIdToLocationId(3) } doReturn wandsworthLocation3.id.toString()
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking().withMainCourtPrisonAppointment(
      prisonCode = WANDSWORTH,
      prisonerNumber = "ABC123",
    )

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = WANDSWORTH,
        courtCode = DERBY_JUSTICE_CENTRE,
        courtName = null,
        probation = false,
        createdByUsername = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comment = "Migrated court comments",
        pre = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
        main = AppointmentLocationTimeSlot(2, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = AppointmentLocationTimeSlot(3, yesterday(), LocalTime.of(11, 0), LocalTime.of(12, 0)),
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            pre = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            main = AppointmentLocationTimeSlot(2, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = AppointmentLocationTimeSlot(3, yesterday(), LocalTime.of(11, 0), LocalTime.of(12, 0)),
            prisonCode = WANDSWORTH,
            createdByUsername = "MIGRATION COURT CREATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
          VideoBookingMigrateEvent(
            eventId = 2,
            eventTime = yesterday().atStartOfDay().plusHours(1),
            eventType = VideoLinkBookingEventType.DELETE,
            comment = "Court booking delete event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            pre = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            main = AppointmentLocationTimeSlot(2, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = AppointmentLocationTimeSlot(3, yesterday(), LocalTime.of(11, 0), LocalTime.of(12, 0)),
            prisonCode = WANDSWORTH,
            createdByUsername = "MIGRATION COURT DELETE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    inOrder(videoBookingRepository, bookingHistoryRepository) {
      verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())
      verify(bookingHistoryRepository, times(2)).saveAndFlush(bookingHistoryCaptor.capture())
    }

    bookingHistoryCaptor.allValues hasSize 2

    with(bookingHistoryCaptor.allValues.component1()) {
      historyType isEqualTo HistoryType.CREATE
      createdBy isEqualTo "MIGRATION COURT CREATE USER"
      createdTime isEqualTo yesterday().atStartOfDay()

      appointments() hasSize 3

      appointments().component1()
        .isAtPrison(WANDSWORTH)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(9, 0))
        .endsAt(LocalTime.of(10, 0))
        .isForAppointmentType("VLB_COURT_PRE")
        .isAtLocation(wandsworthLocation)

      appointments().component2()
        .isAtPrison(WANDSWORTH)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_COURT_MAIN")
        .isAtLocation(wandsworthLocation2)

      appointments().component3()
        .isAtPrison(WANDSWORTH)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(11, 0))
        .endsAt(LocalTime.of(12, 0))
        .isForAppointmentType("VLB_COURT_POST")
        .isAtLocation(wandsworthLocation3)
    }

    with(bookingHistoryCaptor.allValues.component2()) {
      historyType isEqualTo HistoryType.CANCEL
      createdBy isEqualTo "MIGRATION COURT DELETE USER"
      createdTime isEqualTo yesterday().atStartOfDay().plusHours(1)

      appointments() hasSize 3

      appointments().component1()
        .isAtPrison(WANDSWORTH)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(9, 0))
        .endsAt(LocalTime.of(10, 0))
        .isForAppointmentType("VLB_COURT_PRE")
        .isAtLocation(wandsworthLocation)

      appointments().component2()
        .isAtPrison(WANDSWORTH)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_COURT_MAIN")
        .isAtLocation(wandsworthLocation2)

      appointments().component3()
        .isAtPrison(WANDSWORTH)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(11, 0))
        .endsAt(LocalTime.of(12, 0))
        .isForAppointmentType("VLB_COURT_POST")
        .isAtLocation(wandsworthLocation3)
    }
  }

  @Test
  fun `should migrate CREATE booking history for a probation booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "DEF123"
      on { mapInternalLocationIdToLocationId(1) } doReturn pentonvilleLocation.id.toString()
      on { mapProbationTeamCodeToProbationTeam(BLACKPOOL_MC_PPOC) } doReturn probationTeam(BLACKPOOL_MC_PPOC)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isProbationBooking() })) doReturn probationBooking().withMainCourtPrisonAppointment(
      prisonCode = PENTONVILLE,
      prisonerNumber = "DEF123",
    )

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 5,
        offenderBookingId = 1,
        prisonCode = PENTONVILLE,
        courtCode = BLACKPOOL_MC_PPOC,
        courtName = null,
        probation = true,
        createdByUsername = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comment = "Migrated probation comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = 2.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            pre = null,
            main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = null,
            prisonCode = PENTONVILLE,
            createdByUsername = "MIGRATION PROBATION USER",
            courtName = null,
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    inOrder(videoBookingRepository, bookingHistoryRepository) {
      verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())
      verify(bookingHistoryRepository).saveAndFlush(bookingHistoryCaptor.capture())
    }

    with(bookingHistoryCaptor.allValues.single()) {
      historyType isEqualTo HistoryType.CREATE
      createdBy isEqualTo "MIGRATION PROBATION USER"
      createdTime isEqualTo 2.daysAgo().atStartOfDay()

      appointments().single()
        .isAtPrison(PENTONVILLE)
        .isForPrisonerNumber("DEF123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(pentonvilleLocation)
    }
  }

  @Test
  fun `should migrate AMENDED booking history for a probation booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "DEF123"
      on { mapInternalLocationIdToLocationId(1) } doReturn pentonvilleLocation.id.toString()
      on { mapProbationTeamCodeToProbationTeam(BLACKPOOL_MC_PPOC) } doReturn probationTeam(BLACKPOOL_MC_PPOC)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isProbationBooking() })) doReturn probationBooking().withMainCourtPrisonAppointment(
      prisonCode = PENTONVILLE,
      prisonerNumber = "DEF123",
    )

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 5,
        offenderBookingId = 1,
        prisonCode = PENTONVILLE,
        courtCode = BLACKPOOL_MC_PPOC,
        courtName = null,
        probation = true,
        createdByUsername = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comment = "Migrated probation comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = 2.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            pre = null,
            main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = null,
            prisonCode = PENTONVILLE,
            createdByUsername = "MIGRATION PROBATION CREATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
          VideoBookingMigrateEvent(
            eventId = 2,
            eventTime = 1.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.UPDATE,
            comment = "Probation booking updated event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            pre = null,
            main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = null,
            prisonCode = PENTONVILLE,
            createdByUsername = "MIGRATION PROBATION UPDATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    inOrder(videoBookingRepository, bookingHistoryRepository) {
      verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())
      verify(bookingHistoryRepository, times(2)).saveAndFlush(bookingHistoryCaptor.capture())
    }

    bookingHistoryCaptor.allValues hasSize 2

    with(bookingHistoryCaptor.firstValue) {
      historyType isEqualTo HistoryType.CREATE
      createdBy isEqualTo "MIGRATION PROBATION CREATE USER"
      createdTime isEqualTo 2.daysAgo().atStartOfDay()

      appointments().single()
        .isAtPrison(PENTONVILLE)
        .isForPrisonerNumber("DEF123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(pentonvilleLocation)
    }

    with(bookingHistoryCaptor.secondValue) {
      historyType isEqualTo HistoryType.AMEND
      createdBy isEqualTo "MIGRATION PROBATION UPDATE USER"
      createdTime isEqualTo 1.daysAgo().atStartOfDay()

      appointments().single()
        .isAtPrison(PENTONVILLE)
        .isForPrisonerNumber("DEF123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(pentonvilleLocation)
    }
  }

  @Test
  fun `should migrate CANCELLED booking history for a probation booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "DEF123"
      on { mapInternalLocationIdToLocationId(1) } doReturn pentonvilleLocation.id.toString()
      on { mapProbationTeamCodeToProbationTeam(BLACKPOOL_MC_PPOC) } doReturn probationTeam(BLACKPOOL_MC_PPOC)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isProbationBooking() })) doReturn probationBooking().withMainCourtPrisonAppointment(
      prisonCode = PENTONVILLE,
      prisonerNumber = "DEF123",
    )

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 5,
        offenderBookingId = 1,
        prisonCode = PENTONVILLE,
        courtCode = BLACKPOOL_MC_PPOC,
        courtName = null,
        probation = true,
        createdByUsername = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comment = "Migrated probation comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = null,
        cancelled = true,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = 2.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            pre = null,
            main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = null,
            prisonCode = PENTONVILLE,
            createdByUsername = "MIGRATION PROBATION CREATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
          VideoBookingMigrateEvent(
            eventId = 2,
            eventTime = 1.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.DELETE,
            comment = "Probation booking delete event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            pre = null,
            main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = null,
            prisonCode = PENTONVILLE,
            createdByUsername = "MIGRATION PROBATION DELETE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    inOrder(videoBookingRepository, bookingHistoryRepository) {
      verify(videoBookingRepository).saveAndFlush(videoBookingCaptor.capture())
      verify(bookingHistoryRepository, times(2)).saveAndFlush(bookingHistoryCaptor.capture())
    }

    bookingHistoryCaptor.allValues hasSize 2

    with(bookingHistoryCaptor.firstValue) {
      historyType isEqualTo HistoryType.CREATE
      createdBy isEqualTo "MIGRATION PROBATION CREATE USER"
      createdTime isEqualTo 2.daysAgo().atStartOfDay()

      appointments().single()
        .isAtPrison(PENTONVILLE)
        .isForPrisonerNumber("DEF123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(pentonvilleLocation)
    }

    with(bookingHistoryCaptor.secondValue) {
      historyType isEqualTo HistoryType.CANCEL
      createdBy isEqualTo "MIGRATION PROBATION DELETE USER"
      createdTime isEqualTo 1.daysAgo().atStartOfDay()

      appointments().single()
        .isAtPrison(PENTONVILLE)
        .isForPrisonerNumber("DEF123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(pentonvilleLocation)
    }
  }

  private fun BookingHistoryAppointment.isAtPrison(prisonCode: String) = also { it.prisonCode isEqualTo prisonCode }
  private fun BookingHistoryAppointment.isForPrisonerNumber(prisonerNumber: String) = also { it.prisonerNumber isEqualTo prisonerNumber }
  private fun BookingHistoryAppointment.isOnDate(appointmentDate: LocalDate) = also { it.appointmentDate isEqualTo appointmentDate }
  private fun BookingHistoryAppointment.startsAt(startTime: LocalTime) = also { it.startTime isEqualTo startTime }
  private fun BookingHistoryAppointment.endsAt(endTime: LocalTime) = also { it.endTime isEqualTo endTime }
  private fun BookingHistoryAppointment.isForAppointmentType(appointmentType: String) = also { it.appointmentType isEqualTo appointmentType }
  private fun BookingHistoryAppointment.isAtLocation(location: Location) = also { it.prisonLocationId isEqualTo location.id.toString() }

  // TODO more tests needed e.g. error conditions, history.
}
