package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser

@Component
class ProbationOfficerDetailsReminderJob(
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val bookingFacade: BookingFacade,
  timeSource: TimeSource,
) : DailyJob(
  jobType = JobType.PROBATION_OFFICER_DETAILS_REMINDER,
  timeSource,
  { date ->
    prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(date, "VLB_PROBATION")
      .map { it.videoBooking }
      .bookingsWhichCanSelfServe()
      .missingOfficerDetails(additionalBookingDetailRepository)
  },
  { bookings -> bookings.forEach { bookingFacade.sendProbationOfficerDetailsReminder(it, getServiceAsUser()) } },
)

private fun Collection<VideoBooking>.bookingsWhichCanSelfServe() = filter { it.probationTeam!!.enabled && it.prisonIsEnabledForSelfService() }

private fun Collection<VideoBooking>.missingOfficerDetails(repository: AdditionalBookingDetailRepository) = filterNot { repository.existsByVideoBooking(it) }
