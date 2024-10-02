package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
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
    // Migrated bookings will not have the court hearing link field populated, and most probably will have the hearing link provided in the comments
    // Disabling this email for migrated bookings. This can be removed a few days after go-live once the majority of future bookings have taken place.
    val tomorrow = LocalDate.now().plusDays(1)
    prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(tomorrow)
      .map { it.videoBooking }
      .distinct()
      .filter { it.isCourtBooking() && !it.isMigrated() && it.videoUrl == null && it.court!!.enabled && it.prisonIsEnabledForSelfService() }
      .forEach { bookingFacade.courtHearingLinkReminder(it, getServiceAsUser()) }
  },
)
