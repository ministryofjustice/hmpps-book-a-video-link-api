package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ChangeType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ContactsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.DeliusUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ServiceUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.BookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReplacementCourtEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReplacementProbationEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType as ModelBookingType

@Service
class ReplacementEmailFacade(
  private val contactsService: ContactsService,
  private val locationsService: LocationsService,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
  private val videoBookingRepository: VideoBookingRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun sendEmails(bookingDetails: BookingDetails, user: User, changeType: ChangeType = ChangeType.GLOBAL) {
    when (bookingDetails.bookingType) {
      ModelBookingType.COURT -> sendCourtEmails(bookingDetails, user, changeType)
      ModelBookingType.PROBATION -> sendProbationEmails(bookingDetails, user, changeType)
    }
  }

  private fun sendCourtEmails(bookingDetails: BookingDetails, user: User, changeType: ChangeType) {
    val (pre, main, post) = Triple(bookingDetails.preHearing, bookingDetails.mainHearing!!, bookingDetails.postHearing)

    // Get the locations - including room decorations - associated with this court booking
    val locations = setOfNotNull(
      pre?.dpsLocationId,
      main.dpsLocationId,
      post?.dpsLocationId,
    ).mapNotNull { locationsService.getLocationById(it) }.associateBy { it.dpsLocationId }

    // Get the contacts who should be notified of this court booking action
    val contacts = contactsService.getCourtBookingContacts(
      action = bookingDetails.action,
      videoBookingId = bookingDetails.videoLinkBookingId,
      locations = locations.values.toList(),
      user,
    ).withAnEmailAddress()

    // Create the emails based on recipient types
    val emails = contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.USER -> ReplacementCourtEmailFactory.user(bookingDetails, contact)
          .takeIf { user is PrisonUser || user is ExternalUser }

        ContactType.COURT -> ReplacementCourtEmailFactory.court(bookingDetails, contact)
          .takeIf { (user is PrisonUser || user is ServiceUser) && changeType in setOf(ChangeType.GLOBAL) }

        ContactType.PRISON -> ReplacementCourtEmailFactory.prison(bookingDetails, contact, contacts).takeIf {
          changeType in setOf(ChangeType.GLOBAL, ChangeType.PRISON)
        }

        else -> null
      }
    }

    // Send emails and save in the record of notifications
    emails.forEach { courtEmail -> sendEmailAndSaveNotification(courtEmail, bookingDetails) }
  }

  private fun sendProbationEmails(bookingDetails: BookingDetails, user: User, changeType: ChangeType) {
    val appointment = bookingDetails.mainMeeting!!

    // Get the location detail - including room decoration - where this booking takes place
    val location = locationsService.getLocationById(appointment.dpsLocationId)!!

    // Get the contacts who should be notified of this probation booking action
    val contacts = contactsService.getProbationBookingContacts(
      action = bookingDetails.action,
      videoBookingId = bookingDetails.videoLinkBookingId,
      location = location,
      user,
    ).withAnEmailAddress()

    // Create the emails based on recipient types
    val emails = contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.USER -> ReplacementProbationEmailFactory.user(bookingDetails, contact)
          .takeIf { user is PrisonUser || user is ExternalUser || user is DeliusUser }

        ContactType.PROBATION -> ReplacementProbationEmailFactory.probation(bookingDetails, contact)
          .takeIf { (user is PrisonUser || user is ServiceUser) && changeType in setOf(ChangeType.GLOBAL) }

        ContactType.PRISON -> ReplacementProbationEmailFactory.prison(bookingDetails, contact, contacts)
          .takeIf { changeType in setOf(ChangeType.GLOBAL, ChangeType.PRISON) }

        else -> null
      }
    }

    // Send the emails and record the notification sent
    emails.forEach { probationEmail -> sendEmailAndSaveNotification(probationEmail, bookingDetails) }
  }

  private fun Collection<BookingContact>.withAnEmailAddress() = filter { it.email != null }

  private fun sendEmailAndSaveNotification(email: VideoBookingEmail, bookingDetails: BookingDetails) {
    emailService.send(email).onSuccess { (govNotifyId, templateId) ->
      notificationRepository.saveAndFlush(
        Notification(
          videoBooking = videoBookingRepository.findById(bookingDetails.videoLinkBookingId).orElseThrow(),
          email = email.address,
          govNotifyNotificationId = govNotifyId,
          templateName = templateId,
          reason = bookingDetails.action.name,
        ),
      )
    }.onFailure {
      log.info("BOOKINGS: Failed to send ${bookingDetails.action.name} email for video booking ID ${bookingDetails.videoLinkBookingId}.")
    }
  }
}
