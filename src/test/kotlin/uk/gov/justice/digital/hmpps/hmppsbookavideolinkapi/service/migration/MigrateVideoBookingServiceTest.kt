package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.AppointmentLocationTimeSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation3
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalTime

class MigrateVideoBookingServiceTest {
  private val migrateMappingService: MigrateMappingService = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val bookingHistoryRepository: BookingHistoryRepository = mock()
  private val service = MigrateVideoBookingService(migrateMappingService, videoBookingRepository, bookingHistoryRepository)
  private val videoBookingCaptor = argumentCaptor<VideoBooking>()

  @Test
  fun `should migrate booking to a court booking with main appointment only`() {
    migrateMappingService.stub {
      on { mapBookingIdToPrisonerNumber(1) } doReturn "ABC123"
      on { mapInternalLocationIdToLocation(1) } doReturn moorlandLocation
      on { mapCourtCodeToCourt(DERBY_JUSTICE_CENTRE) } doReturn court(DERBY_JUSTICE_CENTRE)
    }

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

  // TODO more tests needed e.g. error conditions, history.
}
