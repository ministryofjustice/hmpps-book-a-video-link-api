package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser

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
  timeSource: TimeSource,
) : DailyJob(
  jobType = JobType.COURT_HEARING_LINK_REMINDER,
  timeSource,
  { date ->
    prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(date, "VLB_COURT_MAIN")
      .map { it.videoBooking }
      .filter { it.videoUrl == null && it.hmctsNumber == null && it.court!!.enabled && it.prisonIsEnabledForSelfService() }
  },
  { bookings -> bookings.forEach { bookingFacade.courtHearingLinkReminder(it, getServiceAsUser()) } },
)
