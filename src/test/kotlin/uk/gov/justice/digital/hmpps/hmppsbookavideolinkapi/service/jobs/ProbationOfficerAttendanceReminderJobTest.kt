package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
  fun `should call probation officer attendance reminders on Mondays with a 2 day offset`() {
    runAndVerifyJobOn(MONDAY, 2)
  }

  @Test
  fun `should call probation officer attendance reminder on Tuesdays with a 2 day offset`() {
    runAndVerifyJobOn(TUESDAY, 2)
  }

  @Test
  fun `should call probation officer attendance reminder on Wednesdays with a 2 day offset`() {
    runAndVerifyJobOn(WEDNESDAY, 2)
  }

  @Test
  fun `should call probation officer attendance reminder on Thursdays with a 4 day offset`() {
    runAndVerifyJobOn(THURSDAY, 4)
  }

  @Test
  fun `should call probation officer attendance reminder on Fridays with a 4 day offset`() {
    runAndVerifyJobOn(FRIDAY, 4)
  }

  @Test
  fun `should be no-op on Saturdays`() {
    runJobOn(SATURDAY)

    verifyNoInteractions(attendanceEmailReminderService)
  }

  @Test
  fun `should be no-op on Sundays`() {
    runJobOn(SUNDAY)

    verifyNoInteractions(attendanceEmailReminderService)
  }

  private fun runAndVerifyJobOn(date: LocalDate, offset: Long) {
    runJobOn(date)

    verify(attendanceEmailReminderService).sendProbationOfficerAttendanceReminders(date.plusDays(offset))
  }

  private fun runJobOn(date: LocalDate) {
    ProbationOfficerAttendanceReminderJob(attendanceEmailReminderService) { date.atStartOfDay() }.runJob()
  }
}
