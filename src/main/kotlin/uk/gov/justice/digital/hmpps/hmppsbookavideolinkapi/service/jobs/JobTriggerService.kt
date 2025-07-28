package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType.COURT_HEARING_LINK_REMINDER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType.NEW_PRISON_VIDEO_ROOM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType.PROBATION_OFFICER_DETAILS_REMINDER

@Service
@Transactional
class JobTriggerService(
  private val jobRunner: JobRunner,
  private val courtHearingLinkReminderJob: CourtHearingLinkReminderJob,
  private val probationOfficerDetailsReminderJob: ProbationOfficerDetailsReminderJob,
  private val newPrisonVideoRoomsJob: NewPrisonVideoRoomsJob,
) {
  fun run(job: JobType) = when (job) {
    COURT_HEARING_LINK_REMINDER -> jobRunner.runJob(courtHearingLinkReminderJob)
    PROBATION_OFFICER_DETAILS_REMINDER -> jobRunner.runJob(probationOfficerDetailsReminderJob)
    NEW_PRISON_VIDEO_ROOM -> jobRunner.runJob(newPrisonVideoRoomsJob)
  }
}
