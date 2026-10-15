package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationOfficerAttendanceEmailReminderService
import java.time.DayOfWeek

/**
 * The purpose of this job is to send probation officers attendance reminders for bookings which are due to start soon.
 */
@Component
class ProbationOfficerAttendanceReminderJob(
  private val service: ProbationOfficerAttendanceEmailReminderService,
  private val timeSource: TimeSource,
) : JobDefinition(
  JobType.PROBATION_OFFICER_ATTENDANCE_REMINDER,
  block = {
    val today = timeSource.today()

    // Always look 2 days ahead for attendance reminders for every day of the week
    service.sendProbationOfficerAttendanceReminders(today.plusDays(2))

    // Also look at the following Monday and Tuesday as well for Thursdays and Fridays for attendance reminders
    if (today.dayOfWeek in listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
      service.sendProbationOfficerAttendanceReminders(today.plusDays(4))
    }
  },
)
