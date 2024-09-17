package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.AppointmentLocationTimeSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoLinkBookingEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingEventRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.InboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.MigrateVideoBookingEvent
import java.time.LocalDate
import java.time.LocalDateTime
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
  fun `should migrate court booking`() {
    whereaboutsApi().stubGetVideoBookingToMigrate(
      1,
      VideoBookingMigrateResponse(
        videoBookingId = 1,
        offenderBookingId = 1,
        prisonCode = MOORLAND,
        courtCode = DERBY_JUSTICE_CENTRE,
        probation = false,
        createdBy = "MIGRATION COURT USER",
        madeByTheCourt = true,
        comments = "Migrated court comments",
        pre = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
        main = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        post = AppointmentLocationTimeSlot(1, yesterday(), LocalTime.of(11, 0), LocalTime.of(12, 0)),
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
            mainLocationId = 1,
            postLocationId = 1,
          ),
        ),
      ),
    )

    prisonSearchApi().stubPostGetPrisonerByBookingId(1, "ABC123", MOORLAND)

    UUID.randomUUID().also { dpsLocationId ->
      nomisMappingApi().stubGetNomisToDpsLocationMapping(1, dpsLocationId.toString())
      locationsInsidePrisonApi().stubGetLocationById(dpsLocationId, moorlandLocation)
    }

    videoBookingRepository.findAll().filter(VideoBooking::isCourtBooking) hasSize 0

    inboundEventsService.process(MigrateVideoBookingEvent(1))

    val migratedCourtBooking = videoBookingRepository.findAll().single(VideoBooking::isCourtBooking)

    with(migratedCourtBooking) {
      bookingType isEqualTo "COURT"
      statusCode isEqualTo StatusCode.ACTIVE
      hearingType isEqualTo "UNKNOWN"
      createdBy isEqualTo "MIGRATION COURT USER"
      createdTime isCloseTo LocalDateTime.now()
      createdByPrison isBool false
      migratedVideoBookingId isEqualTo 1
      // TODO if there is an update/amend should the amended attrs be set?
    }

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

    val migratedBookingHistory = bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(migratedCourtBooking.videoBookingId).single()

    val bookingHistoryAppointments = migratedBookingHistory.appointments()
      .sortedBy(BookingHistoryAppointment::startTime)
      .also { it hasSize 3 }
      .onEach { appointment ->
        appointment
          .isAtPrison(MOORLAND)
          .isForPrisoner("ABC123")
          .isAtLocation(moorlandLocation)
          .isOnDate(yesterday())
      }

    bookingHistoryAppointments.component1()
      .isForAppointmentType("VLB_COURT_PRE")
      .startsAt(9, 0)
      .endsAt(10, 0)

    bookingHistoryAppointments.component2()
      .isForAppointmentType("VLB_COURT_MAIN")
      .startsAt(10, 0)
      .endsAt(11, 0)

    bookingHistoryAppointments.component3()
      .isForAppointmentType("VLB_COURT_POST")
      .startsAt(11, 0)
      .endsAt(12, 0)

    val history = videoBookingEventRepository.findByMainDateBetween(true, yesterday(), yesterday()).toList().single()

    with(history) {
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
  }

  @Test
  @Transactional
  fun `should migrate probation booking`() {
    whereaboutsApi().stubGetVideoBookingToMigrate(
      2,
      VideoBookingMigrateResponse(
        videoBookingId = 2,
        offenderBookingId = 2,
        prisonCode = WERRINGTON,
        courtCode = BLACKPOOL_MC_PPOC,
        probation = true,
        createdBy = "MIGRATION PROBATION USER",
        madeByTheCourt = true,
        comments = "Migrated probation comments",
        main = AppointmentLocationTimeSlot(2, 2.daysAgo(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
        cancelled = false,
        events = listOf(
          VideoBookingEvent(
            eventId = 1,
            eventTime = 3.daysAgo().atStartOfDay(),
            eventType = VideoLinkBookingEventType.CREATE,
            comment = "Probation booking create event comments",
            courtCode = BLACKPOOL_MC_PPOC,
            preStartTime = null,
            preEndTime = null,
            mainStartTime = 2.daysAgo().atTime(10, 0),
            mainEndTime = 2.daysAgo().atTime(11, 0),
            postStartTime = null,
            postEndTime = null,
            prisonCode = WERRINGTON,
            createdByUsername = "MIGRATION PROBATION CREATE USER",
            courtName = null,
            madeByTheCourt = true,
            preLocationId = null,
            mainLocationId = 2,
            postLocationId = null,
          ),
        ),
      ),
    )

    prisonSearchApi().stubPostGetPrisonerByBookingId(2, "DEF123", WERRINGTON)

    UUID.randomUUID().also { dpsLocationId ->
      nomisMappingApi().stubGetNomisToDpsLocationMapping(2, dpsLocationId.toString())
      locationsInsidePrisonApi().stubGetLocationById(dpsLocationId, moorlandLocation2)
    }

    videoBookingRepository.findAll().filter(VideoBooking::isProbationBooking) hasSize 0

    inboundEventsService.process(MigrateVideoBookingEvent(2))

    val migratedProbationBooking = videoBookingRepository.findAll().single(VideoBooking::isProbationBooking)

    with(migratedProbationBooking) {
      bookingType isEqualTo "PROBATION"
      statusCode isEqualTo StatusCode.ACTIVE
      probationMeetingType isEqualTo "UNKNOWN"
      createdBy isEqualTo "MIGRATION PROBATION USER"
      createdTime isCloseTo LocalDateTime.now()
      createdByPrison isBool false
      migratedVideoBookingId isEqualTo 2
      // TODO if there is an update/amend should the amended attrs be set?
    }

    val migratedPrisonAppointment = migratedProbationBooking.appointments().sortedBy(PrisonAppointment::prisonAppointmentId).single()

    migratedPrisonAppointment
      .isAtPrison(WERRINGTON)
      .isForPrisoner("DEF123")
      .isAppointmentType("VLB_PROBATION")
      .isOnDate(2.daysAgo())
      .startsAt(10, 0)
      .endsAt(11, 0)
      .hasComments("Migrated probation comments")

    val migratedBookingHistory = bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(migratedProbationBooking.videoBookingId).single()

    migratedBookingHistory.appointments()
      .single()
      .isAtPrison(WERRINGTON)
      .isForPrisoner("DEF123")
      .isForAppointmentType("VLB_PROBATION")
      .isAtLocation(moorlandLocation2)
      .isOnDate(2.daysAgo())
      .startsAt(10, 0)
      .endsAt(11, 0)

    val history = videoBookingEventRepository.findByMainDateBetween(false, 2.daysAgo(), 2.daysAgo()).toList().single()

    with(history) {
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
      mainLocationKey isEqualTo moorlandLocation2.key
      mainDate isEqualTo 2.daysAgo()
      mainStartTime isEqualTo LocalTime.of(10, 0)
      mainEndTime isEqualTo LocalTime.of(11, 0)
      preLocationKey isEqualTo null
      postStartTime isEqualTo null
      postEndTime isEqualTo null
    }
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
