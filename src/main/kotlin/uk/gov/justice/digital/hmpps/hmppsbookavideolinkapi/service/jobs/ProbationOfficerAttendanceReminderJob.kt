package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationOfficerAttendanceEmailReminderService
import java.time.DayOfWeek

/**
 * The purpose of this job is to send probation officers attendance reminders for bookings which are due to start soon.
 *
 * The job looks for bookings 2 days ahead when running on Monday, Tuesday, and Wednesday, and 4 days ahead when running
 * on Thursday and Friday. It does not run at the weekend.
 */
@Component
class ProbationOfficerAttendanceReminderJob(
  private val service: ProbationOfficerAttendanceEmailReminderService,
  private val timeSource: TimeSource,
) : JobDefinition(
  JobType.PROBATION_OFFICER_ATTENDANCE_REMINDER,
  block = {
    val today = timeSource.today()

    if (today.dayOfWeek in listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)) {
      // Always look 2 days ahead for attendance reminders on these days
      service.sendProbationOfficerAttendanceReminders(today.plusDays(2))
    }

    if (today.dayOfWeek in listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
      // Always look 4 days ahead for attendance reminders on these days
      service.sendProbationOfficerAttendanceReminders(today.plusDays(4))
    }
  },
)
