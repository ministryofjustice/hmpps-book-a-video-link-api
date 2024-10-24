package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoLinkBookingEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.cancelledAt
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.cancelledBy
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.createdAt
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.updatedAt
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.updatedBy
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.APPOINTMENT_TYPE_COURT_MAIN
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.APPOINTMENT_TYPE_COURT_POST
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.APPOINTMENT_TYPE_COURT_PRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.APPOINTMENT_TYPE_PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UNKNOWN_COURT_CODE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UNKNOWN_PROBATION_TEAM_CODE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

@Service
@Deprecated(message = "Can be removed when migration is completed")
class MigrateVideoBookingService(
  private val mappingService: MigrateMappingService,
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonRepository: PrisonRepository,
  private val bookingHistoryRepository: BookingHistoryRepository,
) {
  @Transactional
  fun migrate(booking: VideoBookingMigrateResponse): VideoBooking {
    return if (booking.probation) {
      migrateProbationTeam(booking)
    } else {
      migrateCourt(booking)
    }
  }

  private fun migrateCourt(bookingToMigrate: VideoBookingMigrateResponse): VideoBooking {
    val prisonerNumber = mappingService.mapBookingIdToPrisonerNumber(bookingToMigrate.offenderBookingId)
      ?: throw MigrationException("Unable to find prisoner number for offender booking ID ${bookingToMigrate.offenderBookingId} for court booking ${bookingToMigrate.videoBookingId}")

    val preLocation = bookingToMigrate.pre?.let {
      mappingService.mapInternalLocationIdToLocation(it.locationId) ?: throw NullPointerException(
        "Pre location not found for internal location ID ${bookingToMigrate.main.locationId} for court booking ${bookingToMigrate.videoBookingId}",
      )
    }

    val mainLocation =
      mappingService.mapInternalLocationIdToLocation(bookingToMigrate.main.locationId) ?: throw NullPointerException(
        "Main location not found for internal location ID ${bookingToMigrate.main.locationId} for court booking ${bookingToMigrate.videoBookingId}",
      )

    val postLocation = bookingToMigrate.post?.let {
      mappingService.mapInternalLocationIdToLocation(it.locationId) ?: throw NullPointerException(
        "Post location not found for internal location ID ${bookingToMigrate.main.locationId} for court booking ${bookingToMigrate.videoBookingId}",
      )
    }

    val court = if (bookingToMigrate.courtCode != null && !bookingToMigrate.isOtherCourt()) {
      mappingService.mapCourtCodeToCourt(bookingToMigrate.courtCode)
        ?: throw MigrationException("Court not found for code ${bookingToMigrate.courtCode} for court booking ${bookingToMigrate.videoBookingId}")
    } else {
      mappingService.mapCourtCodeToCourt(UNKNOWN_COURT_CODE)!!
    }

    val prison = prisonRepository.findByCode(bookingToMigrate.prisonCode)
      ?: throw MigrationException("Prison not found for code ${bookingToMigrate.prisonCode} for court booking ${bookingToMigrate.videoBookingId}")

    val migratedBooking = VideoBooking.migratedCourtBooking(
      court = court,
      comments = bookingToMigrate.comment,
      createdBy = bookingToMigrate.createdByUsername,
      createdTime = bookingToMigrate.createdAt(),
      createdByPrison = bookingToMigrate.madeByTheCourt.not(),
      migratedVideoBookingId = bookingToMigrate.videoBookingId,
      cancelledBy = bookingToMigrate.cancelledBy(),
      cancelledAt = bookingToMigrate.cancelledAt(),
      updatedBy = bookingToMigrate.updatedBy(),
      updatedAt = bookingToMigrate.updatedAt(),
      migratedDescription = bookingToMigrate.courtName,
    ).apply {
      if (bookingToMigrate.pre != null) {
        addAppointment(
          prison = prison,
          prisonerNumber = prisonerNumber,
          appointmentType = APPOINTMENT_TYPE_COURT_PRE,
          date = bookingToMigrate.pre.date,
          startTime = bookingToMigrate.pre.startTime,
          endTime = bookingToMigrate.pre.endTime,
          locationKey = preLocation!!.key,
        )
      }

      addAppointment(
        prison = prison,
        prisonerNumber = prisonerNumber,
        appointmentType = APPOINTMENT_TYPE_COURT_MAIN,
        date = bookingToMigrate.main.date,
        startTime = bookingToMigrate.main.startTime,
        endTime = bookingToMigrate.main.endTime,
        locationKey = mainLocation.key,
      )

      if (bookingToMigrate.post != null) {
        addAppointment(
          prison = prison,
          prisonerNumber = prisonerNumber,
          appointmentType = APPOINTMENT_TYPE_COURT_POST,
          date = bookingToMigrate.post.date,
          startTime = bookingToMigrate.post.startTime,
          endTime = bookingToMigrate.post.endTime,
          locationKey = postLocation!!.key,
        )
      }
    }.let(videoBookingRepository::saveAndFlush)

    createHistoryFor(bookingToMigrate.events, migratedBooking)

    return migratedBooking
  }

  private fun migrateProbationTeam(bookingToMigrate: VideoBookingMigrateResponse): VideoBooking {
    val prisonerNumber = mappingService.mapBookingIdToPrisonerNumber(bookingToMigrate.offenderBookingId)
      ?: throw MigrationException("Unable to find prisoner number for offender booking ID ${bookingToMigrate.offenderBookingId} for probation booking ${bookingToMigrate.videoBookingId}")

    val mainLocation =
      mappingService.mapInternalLocationIdToLocation(bookingToMigrate.main.locationId) ?: throw NullPointerException(
        "Main location not found for internal location ID ${bookingToMigrate.main.locationId} for probation booking ${bookingToMigrate.videoBookingId}",
      )

    val probationTeam = if (bookingToMigrate.courtCode != null && !bookingToMigrate.isOtherCourt()) {
      mappingService.mapProbationTeamCodeToProbationTeam(bookingToMigrate.courtCode)
        ?: throw MigrationException("Probation team not found for code ${bookingToMigrate.courtCode} for probation booking ${bookingToMigrate.videoBookingId}")
    } else {
      mappingService.mapProbationTeamCodeToProbationTeam(UNKNOWN_PROBATION_TEAM_CODE)!!
    }

    val prison = prisonRepository.findByCode(bookingToMigrate.prisonCode)
      ?: throw MigrationException("Prison not found for code ${bookingToMigrate.prisonCode} for probation booking ${bookingToMigrate.videoBookingId}")

    val migratedBooking = VideoBooking.migratedProbationBooking(
      probationTeam = probationTeam,
      comments = bookingToMigrate.comment,
      createdBy = bookingToMigrate.createdByUsername,
      createdTime = bookingToMigrate.createdAt(),
      createdByPrison = bookingToMigrate.madeByTheCourt.not(),
      migratedVideoBookingId = bookingToMigrate.videoBookingId,
      cancelledBy = bookingToMigrate.cancelledBy(),
      cancelledAt = bookingToMigrate.cancelledAt(),
      updatedBy = bookingToMigrate.updatedBy(),
      updatedAt = bookingToMigrate.updatedAt(),
      migratedDescription = bookingToMigrate.courtName,
    ).addAppointment(
      prison = prison,
      prisonerNumber = prisonerNumber,
      appointmentType = APPOINTMENT_TYPE_PROBATION,
      date = bookingToMigrate.main.date,
      startTime = bookingToMigrate.main.startTime,
      endTime = bookingToMigrate.main.endTime,
      locationKey = mainLocation.key,
    ).let(videoBookingRepository::saveAndFlush)

    createHistoryFor(bookingToMigrate.events, migratedBooking)

    return migratedBooking
  }

  private fun createHistoryFor(events: List<VideoBookingMigrateEvent>, migratedBooking: VideoBooking) {
    events.sortedBy { it.eventId }.forEach { event -> migrateBookingHistory(event, migratedBooking) }
  }

  private fun migrateBookingHistory(event: VideoBookingMigrateEvent, migratedBooking: VideoBooking) {
    val historyType = when (event.eventType) {
      VideoLinkBookingEventType.CREATE -> HistoryType.CREATE
      VideoLinkBookingEventType.UPDATE -> HistoryType.AMEND
      VideoLinkBookingEventType.DELETE -> HistoryType.CANCEL
    }

    BookingHistory(
      videoBookingId = migratedBooking.videoBookingId,
      historyType = historyType,
      courtId = migratedBooking.court?.courtId.takeIf { migratedBooking.isCourtBooking() },
      hearingType = migratedBooking.hearingType.takeIf { migratedBooking.isCourtBooking() },
      probationTeamId = migratedBooking.probationTeam?.probationTeamId.takeIf { migratedBooking.isProbationBooking() },
      probationMeetingType = migratedBooking.probationMeetingType.takeIf { migratedBooking.isProbationBooking() },
      // Max allowed length for comments is 1000 characters
      comments = event.comment?.take(1000),
      createdBy = event.createdByUsername,
      createdTime = event.eventTime,
    ).apply {
      addBookingHistoryAppointments(getAppointmentsForHistory(this, event, migratedBooking))
    }.also(bookingHistoryRepository::saveAndFlush)
  }

  private fun getAppointmentsForHistory(history: BookingHistory, event: VideoBookingMigrateEvent, migratedBooking: VideoBooking): List<BookingHistoryAppointment> {
    // If not null, this will only be present for a court booking
    val pre = event.pre?.let {
      BookingHistoryAppointment(
        bookingHistory = history,
        prisonCode = migratedBooking.prisonCode(),
        prisonerNumber = migratedBooking.prisoner(),
        appointmentDate = it.date,
        appointmentType = APPOINTMENT_TYPE_COURT_PRE,
        prisonLocKey = mappingService.mapInternalLocationIdToLocation(it.locationId)!!.key,
        startTime = it.startTime,
        endTime = it.endTime,
      )
    }

    val main = event.main.let {
      BookingHistoryAppointment(
        bookingHistory = history,
        prisonCode = migratedBooking.prisonCode(),
        prisonerNumber = migratedBooking.prisoner(),
        appointmentDate = it.date,
        appointmentType = if (migratedBooking.isCourtBooking()) APPOINTMENT_TYPE_COURT_MAIN else APPOINTMENT_TYPE_PROBATION,
        prisonLocKey = mappingService.mapInternalLocationIdToLocation(it.locationId)!!.key,
        startTime = it.startTime,
        endTime = it.endTime,
      )
    }

    // If not null, this will only be present for a court booking
    val post = event.post?.let {
      BookingHistoryAppointment(
        bookingHistory = history,
        prisonCode = migratedBooking.prisonCode(),
        prisonerNumber = migratedBooking.prisoner(),
        appointmentDate = it.date,
        appointmentType = APPOINTMENT_TYPE_COURT_POST,
        prisonLocKey = mappingService.mapInternalLocationIdToLocation(it.locationId)!!.key,
        startTime = it.startTime,
        endTime = it.endTime,
      )
    }

    return listOfNotNull(pre, main, post)
  }
}
