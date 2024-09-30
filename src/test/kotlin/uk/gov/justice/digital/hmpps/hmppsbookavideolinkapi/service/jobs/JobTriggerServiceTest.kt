package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType.COURT_HEARING_LINK_REMINDER

class JobTriggerServiceTest {

  private val jobRunner: JobRunner = mock()
  private val courtHearingLinkReminderJob: CourtHearingLinkReminderJob = mock()
  private val jobTriggerService: JobTriggerService = JobTriggerService(jobRunner, courtHearingLinkReminderJob)

  @Test
  fun `should run court hearing link reminder job when job type is COURT_HEARING_LINK_REMINDER`() {
    jobTriggerService.run(COURT_HEARING_LINK_REMINDER)
    verify(jobRunner).runJob(courtHearingLinkReminderJob)
  }
}
