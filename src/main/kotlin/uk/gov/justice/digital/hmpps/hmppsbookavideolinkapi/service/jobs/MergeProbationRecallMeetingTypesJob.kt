package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingHistoryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService

/**
 * Technically, this job is short-lived. It should only really need to be run once (possibly more due to timings).
 *
 * Its purpose is to merge any future probation meeting types FTR56 and RR to one single meeting type RECALL.
 *
 * Audit records of any merged meeting types will be kept. Only future meetings are merged, historic ones are ignored.
 */
@Component
class MergeProbationRecallMeetingTypesJob(
  private val videoBookingRepository: VideoBookingRepository,
  private val bookingHistoryService: BookingHistoryService,
  private val timeSource: TimeSource,
) : JobDefinition(
  JobType.MERGE_PROBATION_RECALL_MEETING_TYPES,
  block = {
    val now = timeSource.now()
    var anyMeetingTypesMerged = false
    val legacyRecallMeetingTypes = listOf(ProbationMeetingType.FTR56.name, ProbationMeetingType.RR.name)

    val bookings = videoBookingRepository
      .findByProbationMeetingTypesOnOrAfterDate(legacyRecallMeetingTypes, now.toLocalDate())
      .filter { probationMeeting -> probationMeeting.isStatus(StatusCode.ACTIVE) && probationMeeting.overallStartDateTime()?.isAfter(now) == true }
      .onEach { probationMeeting ->
        probationMeeting.apply {
          probationMeetingType = ProbationMeetingType.RECALL.name
          amendedBy = UserService.getServiceAsUser().username
          amendedTime = now
        }

        anyMeetingTypesMerged = true
      }

    if (anyMeetingTypesMerged) {
      videoBookingRepository.saveAllAndFlush(bookings)
      bookings.forEach { booking -> bookingHistoryService.createBookingHistory(HistoryType.AMEND, booking) }

      log.info("Migrated recall meeting type for booking IDs ${bookings.map { it.videoBookingId }}")
    } else {
      log.info("No bookings were migrated for recall meeting type.")
    }
  },
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
