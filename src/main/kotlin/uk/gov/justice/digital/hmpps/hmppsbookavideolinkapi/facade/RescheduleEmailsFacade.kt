package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ChangeType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ContactsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.DeliusUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ServiceUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.RescheduledCourtEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.RescheduledProbationEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService

@Component
class RescheduleEmailsFacade(
  private val prisonRepository: PrisonRepository,
  private val contactsService: ContactsService,
  private val rescheduledCourtEmailFactory: RescheduledCourtEmailFactory,
  private val rescheduledProbationEmailFactory: RescheduledProbationEmailFactory,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
  private val locationsService: LocationsService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Will throw a runtime exception if the booking IDs do not match.
   */
  fun isConsideredRescheduled(originalBooking: VideoLinkBooking, amendedBooking: VideoBooking) = run {
    require(originalBooking.videoLinkBookingId == amendedBooking.videoBookingId) {
      "Original and amended bookings must have the same video booking ID"
    }

    originalBooking.overallStartDateTime() != amendedBooking.overallStartDateTime() || originalBooking.overallEndDateTime() != amendedBooking.overallEndDateTime()
  }

  @Transactional
  fun sendEmails(
    oldBooking: VideoLinkBooking,
    amendedBooking: VideoBooking,
    changeType: ChangeType,
    prisoner: Prisoner,
    user: User,
  ) {
    require(isConsideredRescheduled(oldBooking, amendedBooking)) { "Booking with ID ${oldBooking.videoLinkBookingId} is not rescheduled" }
    val prison = prisonRepository.findByCode(prisoner.prisonCode)!!

    when (amendedBooking.bookingType) {
      BookingType.COURT -> {
        val (pre, main, post) = Triple(amendedBooking.preHearing(), amendedBooking.mainHearing()!!, amendedBooking.postHearing())
        val locations = setOfNotNull(pre?.prisonLocationId, main.prisonLocationId, post?.prisonLocationId)
          .mapNotNull { locationsService.getLocationById(it) }
        val contacts = contactsService.getCourtBookingContacts(BookingAction.AMEND, amendedBooking.videoBookingId, locations, user).withAnEmailAddress()
        sendCourtEmails(oldBooking, amendedBooking, changeType, prison, prisoner, contacts, user)
      }
      BookingType.PROBATION -> {
        val location = locationsService.getLocationById(amendedBooking.probationMeeting()!!.prisonLocationId)
        val contacts = contactsService.getProbationBookingContacts(BookingAction.AMEND, amendedBooking.videoBookingId, location!!, user).withAnEmailAddress()
        sendProbationEmails(oldBooking, amendedBooking, changeType, prison, prisoner, contacts, user)
      }
    }
  }

  private fun sendCourtEmails(
    oldBooking: VideoLinkBooking,
    amendedBooking: VideoBooking,
    changeType: ChangeType,
    prison: Prison,
    prisoner: Prisoner,
    contacts: Collection<BookingContact>,
    user: User,
  ) {
    val emails = contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.USER -> rescheduledCourtEmailFactory.user(oldBooking, amendedBooking, prison, prisoner, contact).takeIf { user is PrisonUser || user is ExternalUser }
        ContactType.COURT -> rescheduledCourtEmailFactory.court(oldBooking, amendedBooking, prison, prisoner, contact).takeIf { (user is PrisonUser || user is ServiceUser) && changeType in setOf(ChangeType.GLOBAL) }
        ContactType.PRISON -> rescheduledCourtEmailFactory.prison(oldBooking, amendedBooking, prison, prisoner, contact, contacts).takeIf { changeType in setOf(ChangeType.GLOBAL, ChangeType.PRISON) }
        else -> null
      }
    }

    emails.forEach { courtEmail -> sendEmailAndSaveNotification(courtEmail, amendedBooking) }
  }

  private fun sendProbationEmails(
    oldBooking: VideoLinkBooking,
    amendedBooking: VideoBooking,
    changeType: ChangeType,
    prison: Prison,
    prisoner: Prisoner,
    contacts: Collection<BookingContact>,
    user: User,
  ) {
    val emails = contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.USER -> rescheduledProbationEmailFactory.user(oldBooking, amendedBooking, contact, prisoner, prison).takeIf { user is PrisonUser || user is ExternalUser || user is DeliusUser }
        ContactType.PROBATION -> rescheduledProbationEmailFactory.probation(oldBooking, amendedBooking, contact, prisoner, prison).takeIf { (user is PrisonUser || user is ServiceUser) && changeType in setOf(ChangeType.GLOBAL) }
        ContactType.PRISON -> rescheduledProbationEmailFactory.prison(oldBooking, amendedBooking, contact, prisoner, prison, contacts).takeIf { changeType in setOf(ChangeType.GLOBAL, ChangeType.PRISON) }
        else -> null
      }
    }

    emails.forEach { probationEmail -> sendEmailAndSaveNotification(probationEmail, amendedBooking) }
  }

  private fun Collection<BookingContact>.withAnEmailAddress() = filter { it.email != null }

  private fun sendEmailAndSaveNotification(email: VideoBookingEmail, booking: VideoBooking) {
    emailService.send(email).onSuccess { (govNotifyId, templateId) ->
      notificationRepository.saveAndFlush(
        Notification(
          videoBooking = booking,
          email = email.address,
          govNotifyNotificationId = govNotifyId,
          templateName = templateId,
          reason = "RESCHEDULED",
        ),
      )
    }.onFailure {
      log.info("BOOKINGS: Failed to send RESCHEDULED email for video booking ID ${booking.videoBookingId}.")
    }
  }
}
