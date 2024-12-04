package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser
import java.time.DayOfWeek

/**
 * This job is responsible for emailing the courts for future court bookings missing video links.
 *
 * For Monday to Thursday and Sunday, emails will be sent for bookings missing links on the following day.
 *
 * For Friday, emails will be sent for the bookings missing links on the following Monday.
 *
 * No emails are sent on Saturday.
 */
@Component
class CourtHearingLinkReminderJob(
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val bookingFacade: BookingFacade,
  private val timeSource: TimeSource,
) : JobDefinition(
  jobType = JobType.COURT_HEARING_LINK_REMINDER,
  block = {
    val today = timeSource.today()

    daysToRunOn[today.dayOfWeek]?.let { offset ->
      prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(today.plusDays(offset))
        .map { it.videoBooking }
        .distinct()
        // Migrated bookings will not have the court hearing link field populated, and most probably will have the hearing link provided in the comments
        // Disabling this email for migrated bookings. This can be removed a few days after go-live once the majority of future bookings have taken place.
        .filter { it.isCourtBooking() && !it.isMigrated() && it.videoUrl == null && it.court!!.enabled && it.prisonIsEnabledForSelfService() }
        .forEach { bookingFacade.courtHearingLinkReminder(it, getServiceAsUser()) }
    }
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
