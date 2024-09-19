package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.AppointmentLocationTimeSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoLinkBookingEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.daysAgo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation3
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingEventRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.InboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.MigrateVideoBookingEvent
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Deprecated(message = "Can be removed when migration is completed")
class MigrateBookingIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var inboundEventsService: InboundEventsService

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var bookingHistoryRepository: BookingHistoryRepository

  @Autowired
  private lateinit var videoBookingEventRepository: VideoBookingEventRepository

  @Test
  @Transactional
  fun `should migrate court booking with an update event`() {
    whereaboutsApi().stubGetVideoBookingToMigrate(
      1,
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = MOORLAND,
        courtCode = DERBY_JUSTICE_CENTRE,
        courtName = null,
        probation = false,
        createdByUsername = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comment = "Migrated court comments",
        pre = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
        main = AppointmentLocationTimeSlot(2, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(11, 0), LocalTime.of(12, 0)),
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            pre = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(11, 0), LocalTime.of(12, 0)),
            prisonCode = MOORLAND,
            createdByUsername = "MIGRATION COURT CREATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
          VideoBookingMigrateEvent(
            eventId = 2,
            eventTime = yesterday().atTime(4, 0),
            eventType = VideoLinkBookingEventType.UPDATE,
            comment = "Court main meeting changed event comments",
            courtCode = DERBY_JUSTICE_CENTRE,
            pre = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
            main = AppointmentLocationTimeSlot(2, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(11, 0), LocalTime.of(12, 0)),
            prisonCode = MOORLAND,
            createdByUsername = "MIGRATION COURT UPDATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    prisonSearchApi().stubPostGetPrisonerByBookingId(1, "ABC123", MOORLAND)

    // Mapping NOMIS internal location ID's to locations to ultimately extract the location business keys
    UUID.randomUUID().also { dpsLocationId ->
      nomisMappingApi().stubGetNomisToDpsLocationMapping(1, dpsLocationId.toString())
      locationsInsidePrisonApi().stubGetLocationById(dpsLocationId, moorlandLocation)
    }

    // Mapping NOMIS internal location ID's to locations to ultimately extract the location business keys
    UUID.randomUUID().also { dpsLocationId ->
      nomisMappingApi().stubGetNomisToDpsLocationMapping(2, dpsLocationId.toString())
      locationsInsidePrisonApi().stubGetLocationById(dpsLocationId, moorlandLocation2)
    }

    videoBookingRepository.findAll().filter(VideoBooking::isCourtBooking) hasSize 0

    inboundEventsService.process(MigrateVideoBookingEvent(1))

    val migratedCourtBooking = videoBookingRepository.findAll().single(VideoBooking::isCourtBooking)

    // Assertions on the migrated probation booking
    with(migratedCourtBooking) {
      bookingType isEqualTo "COURT"
      court?.code isEqualTo DERBY_JUSTICE_CENTRE
      statusCode isEqualTo StatusCode.ACTIVE
      hearingType isEqualTo "UNKNOWN"
      createdBy isEqualTo "MIGRATION COURT USER"
      createdTime isEqualTo yesterday().atStartOfDay()
      createdByPrison isBool false
      migratedVideoBookingId isEqualTo 1
      amendedBy isEqualTo "MIGRATION COURT UPDATE USER"
      amendedTime isEqualTo yesterday().atTime(4, 0)
    }

    // Assertions on the migrated court bookings appointments
    val migratedPrisonAppointments = migratedCourtBooking.appointments().sortedBy(PrisonAppointment::prisonAppointmentId).also { it hasSize 3 }

    migratedPrisonAppointments.component1()
      .isAtPrison(MOORLAND)
      .isForPrisoner("ABC123")
      .isAppointmentType("VLB_COURT_PRE")
      .isOnDate(yesterday())
      .startsAt(9, 0)
      .endsAt(10, 0)
      .hasComments("Migrated court comments")

    migratedPrisonAppointments.component2()
      .isAtPrison(MOORLAND)
      .isForPrisoner("ABC123")
      .isAppointmentType("VLB_COURT_MAIN")
      .isOnDate(yesterday())
      .startsAt(10, 0)
      .endsAt(11, 0)
      .hasComments("Migrated court comments")

    migratedPrisonAppointments.component3()
      .isAtPrison(MOORLAND)
      .isForPrisoner("ABC123")
      .isAppointmentType("VLB_COURT_POST")
      .isOnDate(yesterday())
      .startsAt(11, 0)
      .endsAt(12, 0)
      .hasComments("Migrated court comments")

    val migratedBookingHistory = bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(migratedCourtBooking.videoBookingId).also { it hasSize 2 }

    with(migratedBookingHistory.component1()) {
      historyType isEqualTo HistoryType.CREATE
      comments isEqualTo "Court booking create event comments"

      appointments()
        .also { it hasSize 3 }
        .onEach { appointment ->
          appointment
            .isAtPrison(MOORLAND)
            .isForPrisoner("ABC123")
            .isAtLocation(moorlandLocation)
            .isOnDate(yesterday())
        }

      appointments().component1()
        .isForAppointmentType("VLB_COURT_PRE")
        .startsAt(9, 0)
        .endsAt(10, 0)

      appointments().component2()
        .isForAppointmentType("VLB_COURT_MAIN")
        .startsAt(10, 0)
        .endsAt(11, 0)

      appointments().component3()
        .isForAppointmentType("VLB_COURT_POST")
        .startsAt(11, 0)
        .endsAt(12, 0)
    }

    with(migratedBookingHistory.component2()) {
      historyType isEqualTo HistoryType.AMEND
      comments isEqualTo "Court main meeting changed event comments"

      appointments()
        .also { it hasSize 3 }
        .onEach { appointment ->
          appointment
            .isAtPrison(MOORLAND)
            .isForPrisoner("ABC123")
            .isOnDate(yesterday())
        }

      appointments().component1()
        .isForAppointmentType("VLB_COURT_PRE")
        .isAtLocation(moorlandLocation)
        .startsAt(9, 0)
        .endsAt(10, 0)

      appointments().component2()
        .isForAppointmentType("VLB_COURT_MAIN")
        .isAtLocation(moorlandLocation2)
        .startsAt(10, 0)
        .endsAt(11, 0)

      appointments().component3()
        .isForAppointmentType("VLB_COURT_POST")
        .isAtLocation(moorlandLocation)
        .startsAt(11, 0)
        .endsAt(12, 0)
    }

    // Assertions on the migrated event history taken from the booking that has been migrated
    val createHistoryEvent = videoBookingEventRepository.findById(1).orElseThrow()

    with(createHistoryEvent) {
      timestamp isEqualTo yesterday().atStartOfDay()
      videoBookingId isEqualTo migratedCourtBooking.videoBookingId
      eventType isEqualTo "CREATE"
      prisonCode isEqualTo MOORLAND
      courtDescription isEqualTo "Derby Justice Centre"
      courtCode isEqualTo DERBY_JUSTICE_CENTRE
      courtBooking isBool true
      createdByPrison isBool false
      preLocationKey isEqualTo moorlandLocation.key
      preStartTime isEqualTo LocalTime.of(9, 0)
      preEndTime isEqualTo LocalTime.of(10, 0)
      mainLocationKey isEqualTo moorlandLocation.key
      mainDate isEqualTo yesterday()
      mainStartTime isEqualTo LocalTime.of(10, 0)
      mainEndTime isEqualTo LocalTime.of(11, 0)
      preLocationKey isEqualTo moorlandLocation.key
      postStartTime isEqualTo LocalTime.of(11, 0)
      postEndTime isEqualTo LocalTime.of(12, 0)
    }

    val amendHistoryEvent = videoBookingEventRepository.findById(2).orElseThrow()

    with(amendHistoryEvent) {
      timestamp isEqualTo yesterday().atTime(4, 0)
      videoBookingId isEqualTo migratedCourtBooking.videoBookingId
      eventType isEqualTo "AMEND"
      prisonCode isEqualTo MOORLAND
      courtDescription isEqualTo "Derby Justice Centre"
      courtCode isEqualTo DERBY_JUSTICE_CENTRE
      courtBooking isBool true
      createdByPrison isBool false
      preLocationKey isEqualTo moorlandLocation.key
      preStartTime isEqualTo LocalTime.of(9, 0)
      preEndTime isEqualTo LocalTime.of(10, 0)
      mainLocationKey isEqualTo moorlandLocation2.key
      mainDate isEqualTo yesterday()
      mainStartTime isEqualTo LocalTime.of(10, 0)
      mainEndTime isEqualTo LocalTime.of(11, 0)
      preLocationKey isEqualTo moorlandLocation.key
      postStartTime isEqualTo LocalTime.of(11, 0)
      postEndTime isEqualTo LocalTime.of(12, 0)
    }
  }

  @Test
  @Transactional
  fun `should migrate an unknown court booking`() {
    whereaboutsApi().stubGetVideoBookingToMigrate(
      1,
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = MOORLAND,
        courtCode = null,
        courtName = "Unknown court name",
        probation = false,
        createdByUsername = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comment = "Migrated court comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = yesterday().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Court booking create event comments",
            courtCode = null,
            courtName = "Unknown court name",
            pre = null,
            main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = null,
            prisonCode = MOORLAND,
            createdByUsername = "MIGRATION COURT CREATE USER",
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    prisonSearchApi().stubPostGetPrisonerByBookingId(1, "ABC123", MOORLAND)

    // Mapping NOMIS internal location ID's to locations to ultimately extract the location business keys
    UUID.randomUUID().also { dpsLocationId ->
      nomisMappingApi().stubGetNomisToDpsLocationMapping(1, dpsLocationId.toString())
      locationsInsidePrisonApi().stubGetLocationById(dpsLocationId, moorlandLocation)
    }

    videoBookingRepository.findAll().filter(VideoBooking::isCourtBooking) hasSize 0

    inboundEventsService.process(MigrateVideoBookingEvent(1))

    val migratedCourtBooking = videoBookingRepository.findAll().single(VideoBooking::isCourtBooking)

    // Assertions on the migrated probation booking
    with(migratedCourtBooking) {
      bookingType isEqualTo "COURT"
      court?.code isEqualTo "UNKNOWN"
      statusCode isEqualTo StatusCode.ACTIVE
      hearingType isEqualTo "UNKNOWN"
      createdBy isEqualTo "MIGRATION COURT USER"
      createdTime isEqualTo yesterday().atStartOfDay()
      createdByPrison isBool false
      migratedVideoBookingId isEqualTo 1
    }

    // Assertions on the migrated court bookings appointments
    val migratedPrisonAppointment = migratedCourtBooking.appointments().sortedBy(PrisonAppointment::prisonAppointmentId).single()

    migratedPrisonAppointment
      .isAtPrison(MOORLAND)
      .isForPrisoner("ABC123")
      .isAppointmentType("VLB_COURT_MAIN")
      .isOnDate(yesterday())
      .startsAt(10, 0)
      .endsAt(11, 0)
      .hasComments("Migrated court comments")

    val migratedBookingHistory = bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(migratedCourtBooking.videoBookingId).single()

    with(migratedBookingHistory) {
      historyType isEqualTo HistoryType.CREATE
      comments isEqualTo "Court booking create event comments"

      appointments().single()
        .isAtPrison(MOORLAND)
        .isForPrisoner("ABC123")
        .isAtLocation(moorlandLocation)
        .isOnDate(yesterday())
        .isForAppointmentType("VLB_COURT_MAIN")
        .startsAt(10, 0)
        .endsAt(11, 0)
    }

    // Assertions on the migrated event history taken from the booking that has been migrated
    // TODO what to do about event history. Events are only available for enabled courts, the hidden court is disabled.
  }

  @Test
  @Transactional
  fun `should migrate probation booking with an update event`() {
    whereaboutsApi().stubGetVideoBookingToMigrate(
      2,
      VideoBookingMigrateResponse(
        videoBookingId = 2,
        offenderBookingId = 2,
        prisonCode = WERRINGTON,
        courtCode = BLACKPOOL_MC_PPOC,
        courtName = null,
        probation = true,
        createdByUsername = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comment = "Migrated probation comments",
        pre = null,
        main = AppointmentLocationTimeSlot(3, 2.daysAgo(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = 3.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            pre = null,
            main = AppointmentLocationTimeSlot(1, 2.daysAgo(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = null,
            prisonCode = WERRINGTON,
            createdByUsername = "MIGRATION PROBATION CREATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
          VideoBookingMigrateEvent(
            eventId = 2,
            eventTime = 3.daysAgo().atTime(7, 0),
            eventType = VideoLinkBookingEventType.UPDATE,
            comment = "Probation room changed event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            pre = null,
            main = AppointmentLocationTimeSlot(3, 2.daysAgo(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = null,
            prisonCode = WERRINGTON,
            createdByUsername = "MIGRATION PROBATION UPDATE USER",
            courtName = null,
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    prisonSearchApi().stubPostGetPrisonerByBookingId(2, "DEF123", WERRINGTON)

    // Mapping NOMIS internal location ID's to locations to ultimately extract the location business keys
    UUID.randomUUID().also { dpsLocationId ->
      nomisMappingApi().stubGetNomisToDpsLocationMapping(1, dpsLocationId.toString())
      locationsInsidePrisonApi().stubGetLocationById(dpsLocationId, moorlandLocation)
    }

    // Mapping NOMIS internal location ID's to locations to ultimately extract the location business keys
    UUID.randomUUID().also { dpsLocationId ->
      nomisMappingApi().stubGetNomisToDpsLocationMapping(3, dpsLocationId.toString())
      locationsInsidePrisonApi().stubGetLocationById(dpsLocationId, moorlandLocation3)
    }

    videoBookingRepository.findAll().filter(VideoBooking::isProbationBooking) hasSize 0

    inboundEventsService.process(MigrateVideoBookingEvent(2))

    val migratedProbationBooking = videoBookingRepository.findAll().single(VideoBooking::isProbationBooking)

    // Assertions on the migrated probation booking
    with(migratedProbationBooking) {
      bookingType isEqualTo "PROBATION"
      probationTeam?.code isEqualTo BLACKPOOL_MC_PPOC
      statusCode isEqualTo StatusCode.ACTIVE
      probationMeetingType isEqualTo "UNKNOWN"
      createdBy isEqualTo "MIGRATION PROBATION USER"
      createdTime isEqualTo 3.daysAgo().atStartOfDay()
      createdByPrison isBool false
      migratedVideoBookingId isEqualTo 2
      amendedBy isEqualTo "MIGRATION PROBATION UPDATE USER"
      amendedTime isEqualTo 3.daysAgo().atTime(7, 0)
    }

    val migratedPrisonAppointment = migratedProbationBooking.appointments().sortedBy(PrisonAppointment::prisonAppointmentId).single()

    // Assertions on the migrated probation bookings appointment
    migratedPrisonAppointment
      .isAtPrison(WERRINGTON)
      .isForPrisoner("DEF123")
      .isAppointmentType("VLB_PROBATION")
      .isOnDate(2.daysAgo())
      .startsAt(10, 0)
      .endsAt(11, 0)
      .hasComments("Migrated probation comments")

    // Assertions on the migrated booking history and booking history appointment entries
    val migratedBookingHistory = bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(migratedProbationBooking.videoBookingId).also { it hasSize 2 }

    with(migratedBookingHistory.component1()) {
      historyType isEqualTo HistoryType.CREATE
      comments isEqualTo "Probation booking create event comments"

      appointments()
        .single()
        .isAtPrison(WERRINGTON)
        .isForPrisoner("DEF123")
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(moorlandLocation)
        .isOnDate(2.daysAgo())
        .startsAt(10, 0)
        .endsAt(11, 0)
    }

    with(migratedBookingHistory.component2()) {
      historyType isEqualTo HistoryType.AMEND
      comments isEqualTo "Probation room changed event comments"

      appointments()
        .single()
        .isAtPrison(WERRINGTON)
        .isForPrisoner("DEF123")
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(moorlandLocation3)
        .isOnDate(2.daysAgo())
        .startsAt(10, 0)
        .endsAt(11, 0)
    }

    val createHistoryEvent = videoBookingEventRepository.findById(1).orElseThrow()

    // Assertions on the migrated event history taken from the booking that has been migrated
    with(createHistoryEvent) {
      timestamp isEqualTo 3.daysAgo().atStartOfDay()
      videoBookingId isEqualTo migratedProbationBooking.videoBookingId
      eventType isEqualTo "CREATE"
      prisonCode isEqualTo WERRINGTON
      probationTeamDescription isEqualTo "Blackpool MC (PPOC)"
      probationTeamCode isEqualTo BLACKPOOL_MC_PPOC
      courtBooking isBool false
      createdByPrison isBool false
      preLocationKey isEqualTo null
      preStartTime isEqualTo null
      preEndTime isEqualTo null
      mainLocationKey isEqualTo moorlandLocation.key
      mainDate isEqualTo 2.daysAgo()
      mainStartTime isEqualTo LocalTime.of(10, 0)
      mainEndTime isEqualTo LocalTime.of(11, 0)
      preLocationKey isEqualTo null
      postStartTime isEqualTo null
      postEndTime isEqualTo null
    }

    val amendHistoryEvent = videoBookingEventRepository.findById(2).orElseThrow()

    with(amendHistoryEvent) {
      timestamp isEqualTo 3.daysAgo().atTime(7, 0)
      videoBookingId isEqualTo migratedProbationBooking.videoBookingId
      eventType isEqualTo "AMEND"
      prisonCode isEqualTo WERRINGTON
      probationTeamDescription isEqualTo "Blackpool MC (PPOC)"
      probationTeamCode isEqualTo BLACKPOOL_MC_PPOC
      courtBooking isBool false
      createdByPrison isBool false
      preLocationKey isEqualTo null
      preStartTime isEqualTo null
      preEndTime isEqualTo null
      mainLocationKey isEqualTo moorlandLocation3.key
      mainDate isEqualTo 2.daysAgo()
      mainStartTime isEqualTo LocalTime.of(10, 0)
      mainEndTime isEqualTo LocalTime.of(11, 0)
      preLocationKey isEqualTo null
      postStartTime isEqualTo null
      postEndTime isEqualTo null
    }
  }

  @Test
  @Transactional
  fun `should migrate an unknown probation booking`() {
    whereaboutsApi().stubGetVideoBookingToMigrate(
      2,
      VideoBookingMigrateResponse(
        videoBookingId = 2,
        offenderBookingId = 2,
        prisonCode = WERRINGTON,
        courtCode = null,
        courtName = "Unknown probation team",
        probation = true,
        createdByUsername = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comment = "Migrated probation comments",
        pre = null,
        main = AppointmentLocationTimeSlot(1, 2.daysAgo(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = null,
        cancelled = false,
        events = listOf(
          VideoBookingMigrateEvent(
            eventId = 1,
            eventTime = 3.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = null,
            courtName = "Unknown probation team",
            pre = null,
            main = AppointmentLocationTimeSlot(1, 2.daysAgo(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
            post = null,
            prisonCode = WERRINGTON,
            createdByUsername = "MIGRATION PROBATION CREATE USER",
            madeByTheCourt = true,
          ),
        ),
      ),
    )

    prisonSearchApi().stubPostGetPrisonerByBookingId(2, "DEF123", WERRINGTON)

    // Mapping NOMIS internal location ID's to locations to ultimately extract the location business keys
    UUID.randomUUID().also { dpsLocationId ->
      nomisMappingApi().stubGetNomisToDpsLocationMapping(1, dpsLocationId.toString())
      locationsInsidePrisonApi().stubGetLocationById(dpsLocationId, moorlandLocation)
    }

    videoBookingRepository.findAll().filter(VideoBooking::isProbationBooking) hasSize 0

    inboundEventsService.process(MigrateVideoBookingEvent(2))

    val migratedProbationBooking = videoBookingRepository.findAll().single(VideoBooking::isProbationBooking)

    // Assertions on the migrated probation booking
    with(migratedProbationBooking) {
      bookingType isEqualTo "PROBATION"
      probationTeam?.code isEqualTo "UNKNOWN"
      statusCode isEqualTo StatusCode.ACTIVE
      probationMeetingType isEqualTo "UNKNOWN"
      createdBy isEqualTo "MIGRATION PROBATION USER"
      createdTime isEqualTo 3.daysAgo().atStartOfDay()
      createdByPrison isBool false
      migratedVideoBookingId isEqualTo 2
    }

    val migratedPrisonAppointment = migratedProbationBooking.appointments().sortedBy(PrisonAppointment::prisonAppointmentId).single()

    // Assertions on the migrated probation bookings appointment
    migratedPrisonAppointment
      .isAtPrison(WERRINGTON)
      .isForPrisoner("DEF123")
      .isAppointmentType("VLB_PROBATION")
      .isOnDate(2.daysAgo())
      .startsAt(10, 0)
      .endsAt(11, 0)
      .hasComments("Migrated probation comments")

    // Assertions on the migrated booking history and booking history appointment entries
    val migratedBookingHistory = bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(migratedProbationBooking.videoBookingId).single()

    with(migratedBookingHistory) {
      historyType isEqualTo HistoryType.CREATE
      comments isEqualTo "Probation booking create event comments"

      appointments()
        .single()
        .isAtPrison(WERRINGTON)
        .isForPrisoner("DEF123")
        .isForAppointmentType("VLB_PROBATION")
        .isAtLocation(moorlandLocation)
        .isOnDate(2.daysAgo())
        .startsAt(10, 0)
        .endsAt(11, 0)
    }

    // Assertions on the migrated event history taken from the booking that has been migrated
    // TODO what to do about event history. Events are only available for enabled probation teams, the hidden court is disabled.
  }

  private fun PrisonAppointment.isAtPrison(prison: String) = also { it.prisonCode isEqualTo prison }
  private fun PrisonAppointment.isForPrisoner(prisoner: String) = also { it.prisonerNumber isEqualTo prisoner }
  private fun PrisonAppointment.isAppointmentType(type: String) = also { it.appointmentType isEqualTo type }
  private fun PrisonAppointment.isOnDate(appointmentDate: LocalDate) = also { it.appointmentDate isEqualTo appointmentDate }
  private fun PrisonAppointment.startsAt(hour: Int, minute: Int) = also { it.startTime isEqualTo LocalTime.of(hour, minute) }
  private fun PrisonAppointment.endsAt(hour: Int, minute: Int) = also { it.endTime isEqualTo LocalTime.of(hour, minute) }
  private fun PrisonAppointment.hasComments(comments: String) = also { it.comments isEqualTo comments }

  private fun BookingHistoryAppointment.isAtPrison(prisonCode: String) = also { it.prisonCode isEqualTo prisonCode }
  private fun BookingHistoryAppointment.isForPrisoner(prisoner: String) = also { it.prisonerNumber isEqualTo prisoner }
  private fun BookingHistoryAppointment.isOnDate(appointmentDate: LocalDate) = also { it.appointmentDate isEqualTo appointmentDate }
  private fun BookingHistoryAppointment.startsAt(hour: Int, minute: Int) = also { it.startTime isEqualTo LocalTime.of(hour, minute) }
  private fun BookingHistoryAppointment.endsAt(hour: Int, minute: Int) = also { it.endTime isEqualTo LocalTime.of(hour, minute) }
  private fun BookingHistoryAppointment.isForAppointmentType(appointmentType: String) = also { it.appointmentType isEqualTo appointmentType }
  private fun BookingHistoryAppointment.isAtLocation(location: Location) = also { it.prisonLocKey isEqualTo location.key }
}
