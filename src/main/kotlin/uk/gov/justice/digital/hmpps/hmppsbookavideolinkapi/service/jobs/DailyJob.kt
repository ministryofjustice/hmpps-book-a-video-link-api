package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.DayOfWeek
import java.time.LocalDate

abstract class DailyJob(
  jobType: JobType,
  private val timeSource: TimeSource,
  private val bookingsSupplier: (LocalDate) -> Collection<VideoBooking>,
  private val bookingsConsumer: (Collection<VideoBooking>) -> Unit,
) : JobDefinition(
  jobType,
  {
    val today = timeSource.today()

    daysToRunOn[today.dayOfWeek]?.let { offset -> bookingsConsumer(bookingsSupplier(today.plusDays(offset))) }
  },
)

private val daysToRunOn = mapOf(
  DayOfWeek.MONDAY to 1L,
  DayOfWeek.TUESDAY to 1L,
  DayOfWeek.WEDNESDAY to 1L,
  DayOfWeek.THURSDAY to 1L,
  DayOfWeek.FRIDAY to 3L,
  DayOfWeek.SUNDAY to 1L,
)
