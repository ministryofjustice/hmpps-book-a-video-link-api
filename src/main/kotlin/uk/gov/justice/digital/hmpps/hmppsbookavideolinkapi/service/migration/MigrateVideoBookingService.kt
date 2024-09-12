package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

@Service
class MigrateVideoBookingService(
  private val mappingService: MigrateMappingService,
  private val videoBookingRepository: VideoBookingRepository,
  private val bookingHistoryRepository: BookingHistoryRepository,
) {
  @Transactional
  fun migrate(booking: VideoBookingMigrateResponse) {
    if (booking.probation) {
      migrateProbationTeam(booking)
    } else {
      migrateCourt(booking)
    }
  }

  private fun migrateCourt(booking: VideoBookingMigrateResponse) {
    val prisonerNumber = mappingService.mapBookingIdToPrisonerNumber(booking.offenderBookingId)
      ?: throw NullPointerException("Unable to find prisoner number for booking ${booking.videoBookingId}")

    val preLocation = booking.pre?.let {
      mappingService.mapInternalLocationIdToLocation(it.locationId) ?: throw NullPointerException(
        "Pre location not found for internal location ID ${booking.main.locationId} for court booking ${booking.videoBookingId}",
      )
    }

    val mainLocation =
      mappingService.mapInternalLocationIdToLocation(booking.main.locationId) ?: throw NullPointerException(
        "Main location not found for internal location ID ${booking.main.locationId} for court booking ${booking.videoBookingId}",
      )

    val postLocation = booking.post?.let {
      mappingService.mapInternalLocationIdToLocation(it.locationId) ?: throw NullPointerException(
        "Post location not found for internal location ID ${booking.main.locationId} for court booking ${booking.videoBookingId}",
      )
    }

    // TODO still need to migrate if court not found
    val court = mappingService.mapCourtCodeToCourt(booking.courtCode)
      ?: throw NullPointerException("Court not found for code ${booking.courtCode} for court booking ${booking.videoBookingId}")

    VideoBooking.migratedCourtBooking(
      court = court,
      comments = booking.comments,
      createdBy = booking.createdBy,
      createdByPrison = booking.madeByTheCourt.not(),
      migratedVideoBookingId = booking.videoBookingId,
    ).apply {
      if (booking.pre != null) {
        addAppointment(
          prisonCode = booking.prisonCode,
          prisonerNumber = prisonerNumber,
          appointmentType = "VLB_COURT_PRE",
          date = booking.pre.date,
          startTime = booking.pre.startTime,
          endTime = booking.pre.endTime,
          locationKey = preLocation!!.key,
        )
      }

      addAppointment(
        prisonCode = booking.prisonCode,
        prisonerNumber = prisonerNumber,
        appointmentType = "VLB_COURT_MAIN",
        date = booking.main.date,
        startTime = booking.main.startTime,
        endTime = booking.main.endTime,
        locationKey = mainLocation.key,
      )

      if (booking.post != null) {
        addAppointment(
          prisonCode = booking.prisonCode,
          prisonerNumber = prisonerNumber,
          appointmentType = "VLB_COURT_POST",
          date = booking.post.date,
          startTime = booking.post.startTime,
          endTime = booking.post.endTime,
          locationKey = postLocation!!.key,
        )
      }
    }.let(videoBookingRepository::saveAndFlush)

    // TODO apply the history
  }

  private fun migrateProbationTeam(booking: VideoBookingMigrateResponse) {
    val prisonerNumber = mappingService.mapBookingIdToPrisonerNumber(booking.offenderBookingId)
      ?: throw NullPointerException("Unable to find prisoner number for booking ${booking.videoBookingId}")

    val mainLocation =
      mappingService.mapInternalLocationIdToLocation(booking.main.locationId) ?: throw NullPointerException(
        "Main location not found for internal location ID ${booking.main.locationId} for probation booking ${booking.videoBookingId}",
      )

    // TODO still need to migrate if probation not found
    val probationTeam = mappingService.mapProbationTeamCodeToProbationTeam(booking.courtCode)
      ?: throw NullPointerException("Probation team not found for code ${booking.courtCode} for probation booking ${booking.videoBookingId}")

    VideoBooking.migratedProbationBooking(
      probationTeam = probationTeam,
      comments = booking.comments,
      createdBy = booking.createdBy,
      createdByPrison = booking.madeByTheCourt.not(),
      migratedVideoBookingId = booking.videoBookingId,
    ).addAppointment(
      prisonCode = booking.prisonCode,
      prisonerNumber = prisonerNumber,
      appointmentType = "VLB_PROBATION",
      date = booking.main.date,
      startTime = booking.main.startTime,
      endTime = booking.main.endTime,
      locationKey = mainLocation.key,
    ).let(videoBookingRepository::saveAndFlush)

    // TODO apply the history
  }
}
