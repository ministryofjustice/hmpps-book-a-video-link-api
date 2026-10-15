package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationOfficerAttendanceEmailReminderService
import java.time.LocalDate

@Component
class ProbationOfficerAttendanceReminderJob(
  private val service: ProbationOfficerAttendanceEmailReminderService,
) : JobDefinition(
  JobType.PROBATION_OFFICER_ATTENDANCE_REMINDER,
  block = { service.sendProbationOfficerAttendanceReminders(LocalDate.now().plusDays(2)) },
)
