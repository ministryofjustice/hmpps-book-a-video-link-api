package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser
import java.time.LocalDate

/**
 * This job will email the court for any court booking taking place tomorrow, which still does not have a court hearing link
 */
@Component
class CourtHearingLinkReminderJob(
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val bookingFacade: BookingFacade,
) : JobDefinition(
  jobType = JobType.COURT_HEARING_LINK_REMINDER,
  block = {
    val tomorrow = LocalDate.now().plusDays(1)
    prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(tomorrow)
      .map { it.videoBooking }
      .distinct()
      .filter { it.isCourtBooking() && it.videoUrl == null && it.court!!.enabled && it.prisonIsEnabledForSelfService() }
      .forEach { bookingFacade.courtHearingLinkReminder(it, getServiceAsUser()) }
  },
)

private fun VideoBooking.prisonIsEnabledForSelfService() = appointments().all { a -> a.prison.enabled }
