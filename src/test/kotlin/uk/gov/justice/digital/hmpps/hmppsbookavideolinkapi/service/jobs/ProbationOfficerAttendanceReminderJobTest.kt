package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationOfficerAttendanceEmailReminderService
import java.time.DayOfWeek
import java.time.LocalDate

class ProbationOfficerAttendanceReminderJobTest {
  companion object {
    val MONDAY = LocalDate.of(2024, 12, 2).also { require(it.dayOfWeek == DayOfWeek.MONDAY) { "Not Monday" } }
    val TUESDAY = MONDAY.plusDays(1)
    val WEDNESDAY = TUESDAY.plusDays(1)
    val THURSDAY = WEDNESDAY.plusDays(1)
    val FRIDAY = THURSDAY.plusDays(1)
    val SATURDAY = FRIDAY.plusDays(1)
    val SUNDAY = SATURDAY.plusDays(1)
  }

  private val attendanceEmailReminderService: ProbationOfficerAttendanceEmailReminderService = mock()

  @Test
  fun `should call probation officer attendance reminders`() {
    runAndVerifyJobOn(MONDAY, 2)
  }

  @Test
  fun `should call probation officer attendance reminder on Tuesdays`() {
    runAndVerifyJobOn(TUESDAY, 2)
  }

  @Test
  fun `should call probation officer attendance reminder on Wednesdays`() {
    runAndVerifyJobOn(WEDNESDAY, 2)
  }

  @Test
  fun `should call probation officer attendance reminder twice on Thursdays`() {
    runAndVerifyJobOn(THURSDAY, 2, 4)
  }

  @Test
  fun `should call probation officer attendance reminder twice on Fridays`() {
    runAndVerifyJobOn(FRIDAY, 2, 4)
  }

  @Test
  fun `should call probation officer attendance reminder on Saturdays`() {
    runAndVerifyJobOn(SATURDAY, 2)
  }

  @Test
  fun `should call probation officer attendance reminder on Sundays`() {
    runAndVerifyJobOn(SUNDAY, 2)
  }

  private fun runAndVerifyJobOn(date: LocalDate, vararg offset: Long) {
    runJobOn(date)

    offset.forEach { verify(attendanceEmailReminderService).sendProbationOfficerAttendanceReminders(date.plusDays(it)) }
  }

  private fun runJobOn(date: LocalDate) {
    ProbationOfficerAttendanceReminderJob(attendanceEmailReminderService) { date.atStartOfDay() }.runJob()
  }
}
