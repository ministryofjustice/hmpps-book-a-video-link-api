package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoLinkBookingEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.daysAgo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation3
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDate
import java.time.LocalTime

class MigrateVideoBookingServiceTest {
  private val migrateMappingService: MigrateMappingService = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val bookingHistoryRepository: BookingHistoryRepository = mock()
  private val service =
    MigrateVideoBookingService(migrateMappingService, videoBookingRepository, bookingHistoryRepository)
  private val videoBookingCaptor = argumentCaptor<VideoBooking>()
  private val bookingHistoryCaptor = argumentCaptor<BookingHistory>()

  @Test
  fun `should migrate booking to a court booking with main appointment only`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocation(1) } doReturn moorlandLocation
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking()

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = MOORLAND,
        courtCode = DERBY_JUSTICE_CENTRE,
        probation = false,
        createdBy = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comments = "Migrated court comments",
        main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
        cancelled = false,
        events = emptyList(),
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
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_MAIN"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.MIDNIGHT
      endTime isEqualTo LocalTime.MIDNIGHT.plusHours(1)
      prisonLocKey isEqualTo moorlandLocation.key
      comments isEqualTo "Migrated court comments"
    }
  }

  @Test
  fun `should migrate booking to a court booking with pre and main appointment`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocation(1) } doReturn moorlandLocation
      on { mapInternalLocationIdToLocation(2) } doReturn moorlandLocation2
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking()

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 2,
        offenderBookingId = 1,
        prisonCode = MOORLAND,
        courtCode = DERBY_JUSTICE_CENTRE,
        probation = false,
        createdBy = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comments = "Migrated court comments",
        pre = AppointmentLocationTimeSlot(1, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
        main = AppointmentLocationTimeSlot(2, today(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        cancelled = false,
        events = emptyList(),
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
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_PRE"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
      prisonLocKey isEqualTo moorlandLocation.key
      comments isEqualTo "Migrated court comments"
    }

    with(appointments.component2()) {
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_MAIN"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 0)
      prisonLocKey isEqualTo moorlandLocation2.key
      comments isEqualTo "Migrated court comments"
    }
  }

  @Test
  fun `should migrate booking to a court booking with pre, main and post appointment`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocation(1) } doReturn moorlandLocation
      on { mapInternalLocationIdToLocation(2) } doReturn moorlandLocation2
      on { mapInternalLocationIdToLocation(3) } doReturn moorlandLocation3
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking()

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 3,
        offenderBookingId = 1,
        prisonCode = MOORLAND,
        courtCode = DERBY_JUSTICE_CENTRE,
        probation = false,
        createdBy = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comments = "Migrated court comments",
        pre = AppointmentLocationTimeSlot(1, today(), LocalTime.of(8, 0), LocalTime.of(9, 0)),
        main = AppointmentLocationTimeSlot(2, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
        post = AppointmentLocationTimeSlot(3, today(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        cancelled = false,
        events = emptyList(),
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
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_PRE"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(8, 0)
      endTime isEqualTo LocalTime.of(9, 0)
      prisonLocKey isEqualTo moorlandLocation.key
      comments isEqualTo "Migrated court comments"
    }

    with(appointments.component2()) {
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_MAIN"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
      prisonLocKey isEqualTo moorlandLocation2.key
      comments isEqualTo "Migrated court comments"
    }

    with(appointments.component3()) {
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_POST"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 0)
      prisonLocKey isEqualTo moorlandLocation3.key
      comments isEqualTo "Migrated court comments"
    }
  }

  @Test
  fun `should migrate booking to a court booking with main and post appointment`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocation(1) } doReturn moorlandLocation
      on { mapInternalLocationIdToLocation(2) } doReturn moorlandLocation2
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking()

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 4,
        offenderBookingId = 1,
        prisonCode = MOORLAND,
        courtCode = DERBY_JUSTICE_CENTRE,
        probation = false,
        createdBy = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comments = "Migrated court comments",
        main = AppointmentLocationTimeSlot(1, today(), LocalTime.of(8, 0), LocalTime.of(9, 0)),
        post = AppointmentLocationTimeSlot(2, today(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
        cancelled = false,
        events = emptyList(),
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
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_MAIN"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(8, 0)
      endTime isEqualTo LocalTime.of(9, 0)
      prisonLocKey isEqualTo moorlandLocation.key
      comments isEqualTo "Migrated court comments"
    }

    with(appointments.component2()) {
      prisonCode isEqualTo MOORLAND
      prisonerNumber isEqualTo "ABC123"
      appointmentType isEqualTo "VLB_COURT_POST"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(10, 0)
      prisonLocKey isEqualTo moorlandLocation2.key
      comments isEqualTo "Migrated court comments"
    }
  }

  @Test
  fun `should migrate booking to a probation booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "DEF123"
      on { mapInternalLocationIdToLocation(1) } doReturn werringtonLocation
      on { mapProbationTeamCodeToProbationTeam(BLACKPOOL_MC_PPOC) } doReturn probationTeam(BLACKPOOL_MC_PPOC)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isProbationBooking() })) doReturn probationBooking()

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 5,
        offenderBookingId = 1,
        prisonCode = WERRINGTON,
        courtCode = BLACKPOOL_MC_PPOC,
        probation = true,
        createdBy = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comments = "Migrated probation comments",
        main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
        cancelled = false,
        events = emptyList(),
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
      prisonCode isEqualTo WERRINGTON
      prisonerNumber isEqualTo "DEF123"
      appointmentType isEqualTo "VLB_PROBATION"
      appointmentDate isEqualTo today()
      startTime isEqualTo LocalTime.MIDNIGHT
      endTime isEqualTo LocalTime.MIDNIGHT.plusHours(1)
      prisonLocKey isEqualTo werringtonLocation.key
      comments isEqualTo "Migrated probation comments"
    }
  }

  @Test
  fun `should migrate CREATE booking history for a court booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocation(1) } doReturn moorlandLocation
      on { mapInternalLocationIdToLocation(2) } doReturn moorlandLocation2
      on { mapInternalLocationIdToLocation(3) } doReturn moorlandLocation3
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking().withMainCourtPrisonAppointment(
      prisonCode = MOORLAND,
      prisonerNumber = "ABC123",
    )

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = MOORLAND,
        courtCode = DERBY_JUSTICE_CENTRE,
        probation = false,
        createdBy = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comments = "Migrated court comments",
        main = AppointmentLocationTimeSlot(2, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        cancelled = false,
        events = listOf(
          VideoBookingEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            preStartTime = yesterday().atTime(9, 0),
            preEndTime = yesterday().atTime(10, 0),
            mainStartTime = yesterday().atTime(10, 0),
            mainEndTime = yesterday().atTime(11, 0),
            postStartTime = yesterday().atTime(11, 0),
            postEndTime = yesterday().atTime(12, 0),
            prisonCode = MOORLAND,
            createdByUsername = "MIGRATION COURT CREATE USER",
            courtName = null,
            madeByTheCourt = true,
            preLocationId = 1,
            mainLocationId = 2,
            postLocationId = 3,
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

      appointments() hasSize 3

      appointments().component1()
        .isAtPrison(MOORLAND)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(9, 0))
        .endsAt(LocalTime.of(10, 0))
        .isForAppointmentType("VLB_COURT_PRE")
        .isAtLocation(moorlandLocation)

      appointments().component2()
        .isAtPrison(MOORLAND)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_COURT_MAIN")
        .isAtLocation(moorlandLocation2)

      appointments().component3()
        .isAtPrison(MOORLAND)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(11, 0))
        .endsAt(LocalTime.of(12, 0))
        .isForAppointmentType("VLB_COURT_POST")
        .isAtLocation(moorlandLocation3)
    }
  }

  @Test
  fun `should migrate CANCELLED booking history for a court booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocation(1) } doReturn moorlandLocation
      on { mapInternalLocationIdToLocation(2) } doReturn moorlandLocation2
      on { mapInternalLocationIdToLocation(3) } doReturn moorlandLocation3
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isCourtBooking() })) doReturn courtBooking().withMainCourtPrisonAppointment(
      prisonCode = MOORLAND,
      prisonerNumber = "ABC123",
    )

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = MOORLAND,
        courtCode = DERBY_JUSTICE_CENTRE,
        probation = false,
        createdBy = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comments = "Migrated court comments",
        main = AppointmentLocationTimeSlot(2, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        cancelled = false,
        events = listOf(
          VideoBookingEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            preStartTime = yesterday().atTime(9, 0),
            preEndTime = yesterday().atTime(10, 0),
            mainStartTime = yesterday().atTime(10, 0),
            mainEndTime = yesterday().atTime(11, 0),
            postStartTime = yesterday().atTime(11, 0),
            postEndTime = yesterday().atTime(12, 0),
            prisonCode = MOORLAND,
            createdByUsername = "MIGRATION COURT CREATE USER",
            courtName = null,
            madeByTheCourt = true,
            preLocationId = 1,
            mainLocationId = 2,
            postLocationId = 3,
          ),
          VideoBookingEvent(
            eventId = 2,
            eventTime = yesterday().atStartOfDay().plusHours(1),
            eventType = VideoLinkBookingEventType.DELETE,
            comment = "Court booking delete event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            preStartTime = yesterday().atTime(9, 0),
            preEndTime = yesterday().atTime(10, 0),
            mainStartTime = yesterday().atTime(10, 0),
            mainEndTime = yesterday().atTime(11, 0),
            postStartTime = yesterday().atTime(11, 0),
            postEndTime = yesterday().atTime(12, 0),
            prisonCode = MOORLAND,
            createdByUsername = "MIGRATION COURT DELETE USER",
            courtName = null,
            madeByTheCourt = true,
            preLocationId = 1,
            mainLocationId = 2,
            postLocationId = 3,
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
        .isAtPrison(MOORLAND)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(9, 0))
        .endsAt(LocalTime.of(10, 0))
        .isForAppointmentType("VLB_COURT_PRE")
        .isAtLocation(moorlandLocation)

      appointments().component2()
        .isAtPrison(MOORLAND)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_COURT_MAIN")
        .isAtLocation(moorlandLocation2)

      appointments().component3()
        .isAtPrison(MOORLAND)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(11, 0))
        .endsAt(LocalTime.of(12, 0))
        .isForAppointmentType("VLB_COURT_POST")
        .isAtLocation(moorlandLocation3)
    }

    with(bookingHistoryCaptor.allValues.component2()) {
      historyType isEqualTo HistoryType.CANCEL
      createdBy isEqualTo "MIGRATION COURT DELETE USER"
      createdTime isEqualTo yesterday().atStartOfDay().plusHours(1)

      appointments() hasSize 3

      appointments().component1()
        .isAtPrison(MOORLAND)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(9, 0))
        .endsAt(LocalTime.of(10, 0))
        .isForAppointmentType("VLB_COURT_PRE")
        .isAtLocation(moorlandLocation)

      appointments().component2()
        .isAtPrison(MOORLAND)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_COURT_MAIN")
        .isAtLocation(moorlandLocation2)

      appointments().component3()
        .isAtPrison(MOORLAND)
        .isForPrisonerNumber("ABC123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(11, 0))
        .endsAt(LocalTime.of(12, 0))
        .isForAppointmentType("VLB_COURT_POST")
        .isAtLocation(moorlandLocation3)
    }
  }

  @Test
  fun `should migrate CREATE booking history for a probation booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "DEF123"
      on { mapInternalLocationIdToLocation(1) } doReturn werringtonLocation
      on { mapProbationTeamCodeToProbationTeam(BLACKPOOL_MC_PPOC) } doReturn probationTeam(BLACKPOOL_MC_PPOC)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isProbationBooking() })) doReturn probationBooking().withMainCourtPrisonAppointment(
      prisonCode = WERRINGTON,
      prisonerNumber = "DEF123",
    )

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 5,
        offenderBookingId = 1,
        prisonCode = WERRINGTON,
        courtCode = BLACKPOOL_MC_PPOC,
        probation = true,
        createdBy = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comments = "Migrated probation comments",
        main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        cancelled = false,
        events = listOf(
          VideoBookingEvent(
            eventId = 1,
            eventTime = 2.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            preStartTime = null,
            preEndTime = null,
            mainStartTime = yesterday().atTime(10, 0),
            mainEndTime = yesterday().atTime(11, 0),
            postStartTime = null,
            postEndTime = null,
            prisonCode = WERRINGTON,
            createdByUsername = "MIGRATION PROBATION USER",
            courtName = null,
            madeByTheCourt = true,
            preLocationId = null,
            mainLocationId = 1,
            postLocationId = null,
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
        .isAtPrison(WERRINGTON)
        .isForPrisonerNumber("DEF123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(werringtonLocation)
    }
  }

  @Test
  fun `should migrate CANCELLED booking history for a probation booking`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "DEF123"
      on { mapInternalLocationIdToLocation(1) } doReturn werringtonLocation
      on { mapProbationTeamCodeToProbationTeam(BLACKPOOL_MC_PPOC) } doReturn probationTeam(BLACKPOOL_MC_PPOC)
    }

    whenever(videoBookingRepository.saveAndFlush(argThat { booking -> booking.isProbationBooking() })) doReturn probationBooking().withMainCourtPrisonAppointment(
      prisonCode = WERRINGTON,
      prisonerNumber = "DEF123",
    )

    service.migrate(
      VideoBookingMigrateResponse(
        videoBookingId = 5,
        offenderBookingId = 1,
        prisonCode = WERRINGTON,
        courtCode = BLACKPOOL_MC_PPOC,
        probation = true,
        createdBy = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comments = "Migrated probation comments",
        main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        cancelled = true,
        events = listOf(
          VideoBookingEvent(
            eventId = 1,
            eventTime = 2.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            preStartTime = null,
            preEndTime = null,
            mainStartTime = yesterday().atTime(10, 0),
            mainEndTime = yesterday().atTime(11, 0),
            postStartTime = null,
            postEndTime = null,
            prisonCode = WERRINGTON,
            createdByUsername = "MIGRATION PROBATION CREATE USER",
            courtName = null,
            madeByTheCourt = true,
            preLocationId = null,
            mainLocationId = 1,
            postLocationId = null,
          ),
          VideoBookingEvent(
            eventId = 2,
            eventTime = 1.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.DELETE,
            comment = "Probation booking delete event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            preStartTime = null,
            preEndTime = null,
            mainStartTime = yesterday().atTime(10, 0),
            mainEndTime = yesterday().atTime(11, 0),
            postStartTime = null,
            postEndTime = null,
            prisonCode = WERRINGTON,
            createdByUsername = "MIGRATION PROBATION DELETE USER",
            courtName = null,
            madeByTheCourt = true,
            preLocationId = null,
            mainLocationId = 1,
            postLocationId = null,
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
        .isAtPrison(WERRINGTON)
        .isForPrisonerNumber("DEF123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(werringtonLocation)
    }

    with(bookingHistoryCaptor.secondValue) {
      historyType isEqualTo HistoryType.CANCEL
      createdBy isEqualTo "MIGRATION PROBATION DELETE USER"
      createdTime isEqualTo 1.daysAgo().atStartOfDay()

      appointments().single()
        .isAtPrison(WERRINGTON)
        .isForPrisonerNumber("DEF123")
        .isOnDate(yesterday())
        .startsAt(LocalTime.of(10, 0))
        .endsAt(LocalTime.of(11, 0))
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(werringtonLocation)
    }
  }

  private fun BookingHistoryAppointment.isAtPrison(prisonCode: String) = also { it.prisonCode isEqualTo prisonCode }

  private fun BookingHistoryAppointment.isForPrisonerNumber(prisonerNumber: String) =
    also { it.prisonerNumber isEqualTo prisonerNumber }

  private fun BookingHistoryAppointment.isOnDate(appointmentDate: LocalDate) =
    also { it.appointmentDate isEqualTo appointmentDate }

  private fun BookingHistoryAppointment.startsAt(startTime: LocalTime) = also { it.startTime isEqualTo startTime }

  private fun BookingHistoryAppointment.endsAt(endTime: LocalTime) = also { it.endTime isEqualTo endTime }

  private fun BookingHistoryAppointment.isForAppointmentType(appointmentType: String) =
    also { it.appointmentType isEqualTo appointmentType }

  private fun BookingHistoryAppointment.isAtLocation(location: Location) =
    also { it.prisonLocKey isEqualTo location.key }

  // TODO more tests needed e.g. error conditions, history.
}
