package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType.COURT_HEARING_LINK_REMINDER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType.NEW_PRISON_VIDEO_ROOM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType.PROBATION_OFFICER_DETAILS_REMINDER

class JobTriggerServiceTest {

  private val jobRunner: JobRunner = mock()
  private val courtHearingLinkReminderJob: CourtHearingLinkReminderJob = mock()
  private val probationOfficerDetailsReminderJob: ProbationOfficerDetailsReminderJob = mock()
  private val newPrisonVideoRoomsJob: NewPrisonVideoRoomsJob = mock()
  private val jobTriggerService: JobTriggerService = JobTriggerService(jobRunner, courtHearingLinkReminderJob, probationOfficerDetailsReminderJob, newPrisonVideoRoomsJob)

  @Test
  fun `should run court hearing link reminder job when job type is COURT_HEARING_LINK_REMINDER`() {
    jobTriggerService.run(COURT_HEARING_LINK_REMINDER)
    verify(jobRunner).runJob(courtHearingLinkReminderJob)
  }

  @Test
  fun `should run probation officer details reminder job when job type is PROBATION_OFFICER_DETAILS_REMINDER`() {
    jobTriggerService.run(PROBATION_OFFICER_DETAILS_REMINDER)
    verify(jobRunner).runJob(probationOfficerDetailsReminderJob)
  }

  @Test
  fun `should run new video room job when job type is NEW_VIDEO_ROOMS`() {
    jobTriggerService.run(NEW_PRISON_VIDEO_ROOM)
    verify(jobRunner).runJob(newPrisonVideoRoomsJob)
  }
}
