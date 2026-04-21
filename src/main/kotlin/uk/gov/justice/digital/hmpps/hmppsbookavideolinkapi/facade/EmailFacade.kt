package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ChangeType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ContactsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ServiceUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService

@Service
class EmailFacade(
  private val prisonRepository: PrisonRepository,
  private val contactsService: ContactsService,
  private val locationsService: LocationsService,
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun sendEmails(
    action: BookingAction,
    booking: VideoBooking,
    prisoner: Prisoner,
    user: User,
    changeType: ChangeType = ChangeType.GLOBAL,
  ) {
    when (booking.bookingType) {
      BookingType.COURT -> sendCourtEmails(action, booking, prisoner, user, changeType)
      BookingType.PROBATION -> sendProbationEmails(action, booking, prisoner, user, changeType)
    }
  }

  private fun sendCourtEmails(
    eventType: BookingAction,
    booking: VideoBooking,
    prisoner: Prisoner,
    user: User,
    changeType: ChangeType = ChangeType.GLOBAL,
  ) {
    val (pre, main, post) = Triple(booking.preHearing(), booking.mainHearing()!!, booking.postHearing())
    val prison = prisonRepository.findByCode(booking.prisonCode())!!
    val contacts = contactsService.getBookingContacts(booking.videoBookingId, user).withAnEmailAddress()
    val locations = setOfNotNull(
      pre?.prisonLocationId,
      main.prisonLocationId,
      post?.prisonLocationId,
    ).mapNotNull { locationsService.getLocationById(it) }.associateBy { it.dpsLocationId }

    val emails = contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.USER -> CourtEmailFactory.user(
          contact,
          prisoner,
          booking,
          prison,
          main,
          pre,
          post,
          locations,
          eventType,
        ).takeIf { user is PrisonUser || user is ExternalUser }

        ContactType.COURT -> CourtEmailFactory.court(
          contact,
          prisoner,
          booking,
          prison,
          main,
          pre,
          post,
          locations,
          eventType,
        ).takeIf {
          (user is PrisonUser || user is ServiceUser) && changeType in setOf(ChangeType.GLOBAL)
        }

        ContactType.PRISON -> CourtEmailFactory.prison(
          contact,
          prisoner,
          booking,
          prison,
          contacts,
          main,
          pre,
          post,
          locations,
          eventType,
        ).takeIf {
          changeType in setOf(ChangeType.GLOBAL, ChangeType.PRISON)
        }

        else -> null
      }
    }

    emails.forEach { courtEmail -> sendEmailAndSaveNotification(courtEmail, booking, eventType) }
  }

  private fun sendProbationEmails(
    eventType: BookingAction,
    booking: VideoBooking,
    prisoner: Prisoner,
    user: User,
    changeType: ChangeType = ChangeType.GLOBAL,
  ) {
    val appointment = booking.appointments().single()
    val prison = prisonRepository.findByCode(booking.prisonCode())!!
    val contacts = contactsService.getBookingContacts(booking.videoBookingId, user).withAnEmailAddress()
    val location = locationsService.getLocationById(appointment.prisonLocationId)!!
    val additionalBookingDetail = additionalBookingDetailRepository.findByVideoBooking(booking)

    val emails = contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.USER -> ProbationEmailFactory.user(
          contact,
          prisoner,
          booking,
          prison,
          appointment,
          location,
          eventType,
          additionalBookingDetail,
        ).takeIf { user is PrisonUser || user is ExternalUser }

        ContactType.PROBATION -> ProbationEmailFactory.probation(
          contact,
          prisoner,
          booking,
          prison,
          appointment,
          location,
          eventType,
          additionalBookingDetail,
        ).takeIf {
          (user is PrisonUser || user is ServiceUser) && changeType in setOf(ChangeType.GLOBAL)
        }

        ContactType.PRISON -> ProbationEmailFactory.prison(
          contact,
          prisoner,
          booking,
          prison,
          appointment,
          location,
          eventType,
          contacts,
          additionalBookingDetail,
        ).takeIf {
          changeType in setOf(ChangeType.GLOBAL, ChangeType.PRISON)
        }

        else -> null
      }
    }

    emails.forEach { probationEmail -> sendEmailAndSaveNotification(probationEmail, booking, eventType) }
  }

  private fun Collection<BookingContact>.withAnEmailAddress() = filter { it.email != null }

  private fun sendEmailAndSaveNotification(email: VideoBookingEmail, booking: VideoBooking, action: BookingAction) {
    emailService.send(email).onSuccess { (govNotifyId, templateId) ->
      notificationRepository.saveAndFlush(
        Notification(
          videoBooking = booking,
          email = email.address,
          govNotifyNotificationId = govNotifyId,
          templateName = templateId,
          reason = action.name,
        ),
      )
    }.onFailure {
      log.info("BOOKINGS: Failed to send ${action.name} email for video booking ID ${booking.videoBookingId}.")
    }
  }
}
