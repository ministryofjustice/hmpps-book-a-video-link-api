package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType.COURT_HEARING_LINK_REMINDER

@Service
class JobTriggerService(
  private val jobRunner: SafeJobRunner,
  private val courtHearingLinkReminderJob: CourtHearingLinkReminderJob,
) {
  fun run(job: JobType) = when (job) {
    COURT_HEARING_LINK_REMINDER -> jobRunner.runJob(courtHearingLinkReminderJob)
    else -> throw IllegalArgumentException("Unsupported job type ${job.javaClass.name}")
  }
}
