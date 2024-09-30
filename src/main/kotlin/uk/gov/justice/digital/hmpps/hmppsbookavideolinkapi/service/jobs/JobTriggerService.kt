package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType.COURT_HEARING_LINK_REMINDER

@Service
@Transactional
class JobTriggerService(
  private val jobRunner: JobRunner,
  private val courtHearingLinkReminderJob: CourtHearingLinkReminderJob,
) {
  fun run(job: JobType) = when (job) {
    COURT_HEARING_LINK_REMINDER -> jobRunner.runJob(courtHearingLinkReminderJob)
    else -> throw IllegalArgumentException("Unsupported job type ${job.javaClass.name}")
  }
}
