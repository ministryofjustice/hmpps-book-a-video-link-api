package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AdditionalBookingDetail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.VideoLinkBookingsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.BookingDetails
import java.time.LocalDate

@Service
class ProbationOfficerAttendanceEmailReminderService(
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository,
  private val videoLinkBookingsService: VideoLinkBookingsService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun sendProbationOfficerAttendanceReminders(forActiveBookingsOnDate: LocalDate) {
    val probationOfficerContacts = findAllCandidateContactsWithJusticeEmails(forActiveBookingsOnDate).ifEmpty { return }

    val prisoners = probationOfficerContacts.fetchAllPrisonersFromAssociatedBookings()

    val bookings = probationOfficerContacts.map { it.videoBooking }

    bookings.forEach { booking ->
      val bookingDetails = BookingDetails.probationOfficerAttendanceReminder(
        videoLinkBookingsService.getVideoLinkBookingById(booking.videoBookingId, UserService.getServiceAsUser()),
        prisoners[booking.prisoner()]!!,
      )
      val email = ProbationEmailFactory.probationOfficerAttendanceReminder(bookingDetails)

      emailService.send(email)
        .onSuccess { (govNotifyId, templateId) ->
          notificationRepository.saveAndFlush(
            Notification(
              videoBooking = booking,
              email = email.address,
              govNotifyNotificationId = govNotifyId,
              templateName = templateId,
              reason = bookingDetails.action.name,
            ),
          )
        }.onFailure {
          log.error("Failed to send probation officer attendance reminder email for booking ${booking.videoBookingId}")
        }
    }
  }

  private fun findAllCandidateContactsWithJusticeEmails(onDate: LocalDate) = additionalBookingDetailRepository
    .findAdditionalDetailsForAllActiveProbationMeetingsOnDate(onDate)
    .filter { it.isJusticeGovUkEmail() && it.videoBooking.prisonIsEnabledForSelfService() }

  private fun Collection<AdditionalBookingDetail>.fetchAllPrisonersFromAssociatedBookings() = getPrisoner(map { it.videoBooking.prisoner() }.toSet()).associateBy { it.prisonerNumber }

  private fun AdditionalBookingDetail.isJusticeGovUkEmail() = contactEmail.trim().endsWith("@JUSTICE.GOV.UK", ignoreCase = true)

  // Fetch all the prisoners in one API call.
  private fun getPrisoner(prisonerNumber: Set<String>) = prisonerSearchClient.findByPrisonerNumbers(prisonerNumber).map { Prisoner(it.prisonerNumber, it.prisonId!!, it.firstName, it.lastName, it.dateOfBirth) }
}
