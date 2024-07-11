package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.CourtEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService

/**
 * This facade exists to ensure booking related transactions are fully committed prior to sending any emails.
 */
@Component
class BookingFacade(
  private val createVideoBookingService: CreateVideoBookingService,
  private val amendVideoBookingService: AmendVideoBookingService,
  private val cancelVideoBookingService: CancelVideoBookingService,
  private val contactsService: ContactsService,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val prisonRepository: PrisonRepository,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
  private val outboundEventsService: OutboundEventsService,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val prisonerSearchClient: PrisonerSearchClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun create(bookingRequest: CreateVideoBookingRequest, username: String): Long {
    val (booking, prisoner) = createVideoBookingService.create(bookingRequest, username)
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CREATED, booking.videoBookingId)
    sendBookingEmails(BookingAction.CREATE, booking, prisoner, username)
    return booking.videoBookingId
  }

  fun amend(videoBookingId: Long, bookingRequest: AmendVideoBookingRequest, username: String): Long {
    val (booking, prisoner) = amendVideoBookingService.amend(videoBookingId, bookingRequest, username)
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_AMENDED, booking.videoBookingId)
    sendBookingEmails(BookingAction.AMEND, booking, prisoner, username)
    return booking.videoBookingId
  }

  fun cancel(videoBookingId: Long, cancelledBy: String) {
    val booking = cancelVideoBookingService.cancel(videoBookingId, cancelledBy)
    log.info("Video booking ${booking.videoBookingId} cancelled by user")
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CANCELLED, booking.videoBookingId)
    sendBookingEmails(BookingAction.CANCEL, booking, getPrisoner(booking.prisoner()), cancelledBy)
  }

  fun prisonerTransferred(videoBookingId: Long, username: String) {
    val booking = cancelVideoBookingService.cancel(videoBookingId, username)
    log.info("Video booking ${booking.videoBookingId} cancelled due to transfer")
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CANCELLED, booking.videoBookingId)
    sendBookingEmails(BookingAction.TRANSFERRED, booking, getReleasedOrTransferredPrisoner(booking.prisoner()))
  }

  fun prisonerReleased(videoBookingId: Long, username: String) {
    val booking = cancelVideoBookingService.cancel(videoBookingId, username)
    log.info("Video booking ${booking.videoBookingId} cancelled due to release")
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CANCELLED, booking.videoBookingId)
    sendBookingEmails(BookingAction.RELEASED, booking, getReleasedOrTransferredPrisoner(booking.prisoner()))
  }

  private fun getPrisoner(prisonerNumber: String) =
    prisonerSearchClient.getPrisoner(prisonerNumber)!!.let { Prisoner(it.prisonerNumber, it.prisonId!!, it.firstName, it.lastName, it.dateOfBirth) }

  private fun getReleasedOrTransferredPrisoner(prisonerNumber: String) =
    prisonerSearchClient.getPrisoner(prisonerNumber)!!.let { Prisoner(it.prisonerNumber, it.lastPrisonId!!, it.firstName, it.lastName, it.dateOfBirth) }

  private fun VideoBooking.bookingType() = if (isCourtBooking()) BookingType.COURT else BookingType.PROBATION

  private fun sendBookingEmails(action: BookingAction, booking: VideoBooking, prisoner: Prisoner, username: String? = null) {
    when (booking.bookingType()) {
      BookingType.COURT -> sendCourtBookingEmails(action, booking, prisoner, username)
      BookingType.PROBATION -> sendProbationBookingEmails(action, booking, prisoner, username)
    }
  }

  private fun sendCourtBookingEmails(eventType: BookingAction, booking: VideoBooking, prisoner: Prisoner, username: String?) {
    val (pre, main, post) = getCourtAppointments(booking)
    val prison = prisonRepository.findByCode(prisoner.prisonCode)!!
    val contacts = contactsService.getPrimaryBookingContacts(booking.videoBookingId, username).allContactsWithAnEmailAddress()
    val locations = locationsInsidePrisonClient.getLocationsByKeys(setOfNotNull(pre?.prisonLocKey, main.prisonLocKey, post?.prisonLocKey)).associateBy { it.key }

    contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.USER -> CourtEmailFactory.user(contact, prisoner, booking, prison, main, pre, post, locations, eventType)
        ContactType.COURT -> CourtEmailFactory.court(contact, prisoner, booking, prison, main, pre, post, locations, eventType)
        ContactType.PRISON -> CourtEmailFactory.prison(contact, prisoner, booking, prison, contacts, main, pre, post, locations, eventType)
        else -> null
      }
    }.forEach { email ->
      sendEmailAndSaveNotification(email, booking, eventType)
    }
  }

  private fun sendProbationBookingEmails(action: BookingAction, booking: VideoBooking, prisoner: Prisoner, username: String?) {
    log.info("TODO - send probation booking emails.")
  }

  private fun sendEmailAndSaveNotification(email: Email, booking: VideoBooking, action: BookingAction) {
    val reason = when (action) {
      BookingAction.CREATE -> "New court booking"
      BookingAction.AMEND -> "Amended court booking"
      BookingAction.CANCEL -> "Cancelled court booking"
      BookingAction.RELEASED -> "Cancelled court booking due to release"
      BookingAction.TRANSFERRED -> "Cancelled court booking due to transfer"
    }
    emailService.send(email).onSuccess { (govNotifyId, templateId) ->
      notificationRepository.saveAndFlush(
        Notification(
          videoBooking = booking,
          email = email.address,
          govNotifyNotificationId = govNotifyId,
          templateName = templateId,
          reason = reason,
        ),
      )
    }.onFailure {
      log.info("BOOKINGS: Failed to send ${reason.lowercase()} email for video booking ID ${booking.videoBookingId}.")
    }
  }

  private fun getCourtAppointments(booking: VideoBooking): Triple<PrisonAppointment?, PrisonAppointment, PrisonAppointment?> {
    return prisonAppointmentRepository.findByVideoBooking(booking).prisonAppointmentsForCourtHearing()
  }

  private fun Collection<BookingContact>.allContactsWithAnEmailAddress() = filter { it.email != null }

  private fun Collection<PrisonAppointment>.prisonAppointmentsForCourtHearing() = Triple(pre(), main(), post())

  private fun Collection<PrisonAppointment>.pre() = singleOrNull { it.appointmentType == "VLB_COURT_PRE" }

  private fun Collection<PrisonAppointment>.main() = single { it.appointmentType == "VLB_COURT_MAIN" }

  private fun Collection<PrisonAppointment>.post() = singleOrNull { it.appointmentType == "VLB_COURT_POST" }
}

enum class BookingAction {
  CREATE,
  AMEND,
  CANCEL,
  RELEASED,
  TRANSFERRED,
}
