package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability.AvailabilityService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.CourtBookingAmendedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.CourtBookingCancelledTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.CourtBookingCreatedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.ProbationBookingAmendedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.ProbationBookingCancelledTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.ProbationBookingCreatedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType as RequestBookingType

/**
 * This facade exists to ensure all booking related transactions are fully committed prior to sending any events or emails.
 * That is to say, ideally calls to the facade should not be wrapped in transactions.
 */
@Component
class BookingFacade(
  private val videoBookingServiceDelegate: VideoBookingServiceDelegate,
  private val contactsService: ContactsService,
  private val prisonRepository: PrisonRepository,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
  private val outboundEventsService: OutboundEventsService,
  private val locationsService: LocationsService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val telemetryService: TelemetryService,
  private val availabilityService: AvailabilityService,
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun create(bookingRequest: CreateVideoBookingRequest, createdBy: User): Long {
    require(createdBy is PrisonUser || availabilityService.isAvailable(bookingRequest)) {
      if (bookingRequest.bookingType == RequestBookingType.COURT) {
        "Unable to create court booking, booking overlaps with an existing appointment."
      } else {
        "Unable to create probation booking, booking overlaps with an existing appointment."
      }
    }

    val (booking, prisoner) = videoBookingServiceDelegate.create(bookingRequest, createdBy)
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CREATED, booking.videoBookingId)
    sendBookingEmails(BookingAction.CREATE, booking, prisoner, createdBy)
    sendTelemetry(BookingAction.CREATE, booking, createdBy)
    return booking.videoBookingId
  }

  fun amend(videoBookingId: Long, bookingRequest: AmendVideoBookingRequest, amendedBy: User): Long {
    require(amendedBy is PrisonUser || availabilityService.isAvailable(videoBookingId, bookingRequest)) {
      if (bookingRequest.bookingType == RequestBookingType.COURT) {
        "Unable to amend court booking, booking overlaps with an existing appointment."
      } else {
        "Unable to amend probation booking, booking overlaps with an existing appointment."
      }
    }

    val (booking, prisoner) = videoBookingServiceDelegate.amend(videoBookingId, bookingRequest, amendedBy)
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_AMENDED, videoBookingId)
    sendBookingEmails(BookingAction.AMEND, booking, prisoner, amendedBy)
    sendTelemetry(BookingAction.AMEND, booking, amendedBy)
    return booking.videoBookingId
  }

  fun cancel(videoBookingId: Long, cancelledBy: PrisonUser) {
    cancelBooking(videoBookingId, cancelledBy)
  }

  fun cancel(videoBookingId: Long, cancelledBy: ExternalUser) {
    cancelBooking(videoBookingId, cancelledBy)
  }

  fun request(booking: RequestVideoBookingRequest, user: ExternalUser) {
    videoBookingServiceDelegate.request(booking, user)
  }

  private fun cancelBooking(videoBookingId: Long, cancelledBy: User) {
    val booking = videoBookingServiceDelegate.cancel(videoBookingId, cancelledBy)
    log.info("Video booking ${booking.videoBookingId} cancelled by user type ${cancelledBy::class.simpleName}")
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CANCELLED, videoBookingId)
    sendBookingEmails(BookingAction.CANCEL, booking, getPrisoner(booking.prisoner()), cancelledBy)
    sendTelemetry(BookingAction.CANCEL, booking, cancelledBy)
  }

  fun cancel(videoBookingId: Long, cancelledBy: ServiceUser) {
    // No events should be raised on the back of calling this function, this is by design.
    val booking = videoBookingServiceDelegate.cancel(videoBookingId, cancelledBy)
    log.info("Video booking ${booking.videoBookingId} cancelled by user type ${cancelledBy::class.simpleName}")
    sendBookingEmails(BookingAction.CANCEL, booking, getPrisoner(booking.prisoner()), cancelledBy)
    sendTelemetry(BookingAction.CANCEL, booking, cancelledBy)
  }

  fun courtHearingLinkReminder(videoBooking: VideoBooking, user: ServiceUser) {
    require(videoBooking.isBookingType(COURT) && videoBooking.isStatus(StatusCode.ACTIVE)) { "Video booking with id ${videoBooking.videoBookingId} must be an active court booking" }
    require(videoBooking.court!!.enabled) { "Video booking with id ${videoBooking.videoBookingId} is not with an enabled court" }
    require(videoBooking.appointments().any { it.appointmentDate > LocalDate.now() }) { "Video booking with id ${videoBooking.videoBookingId} must be after today" }
    require(videoBooking.videoUrl == null) { "Video booking with id ${videoBooking.videoBookingId} already has a court hearing link" }
    sendBookingEmails(BookingAction.COURT_HEARING_LINK_REMINDER, videoBooking, getPrisoner(videoBooking.prisoner()), user)
  }

  fun sendProbationOfficerDetailsReminder(videoBooking: VideoBooking, user: ServiceUser) {
    require(videoBooking.isBookingType(PROBATION) && videoBooking.isStatus(StatusCode.ACTIVE)) { "Video booking with id ${videoBooking.videoBookingId} must be an active probation booking" }
    require(videoBooking.probationTeam!!.enabled) { "Video booking with id ${videoBooking.videoBookingId} is not with an enabled probation team" }
    require(videoBooking.appointments().any { it.appointmentDate > LocalDate.now() }) { "Video booking with id ${videoBooking.videoBookingId} must be after today" }
    sendProbationBookingEmails(BookingAction.PROBATION_OFFICER_DETAILS_REMINDER, videoBooking, getPrisoner(videoBooking.prisoner()), user)
  }

  fun prisonerTransferred(videoBookingId: Long, user: ServiceUser) {
    val booking = videoBookingServiceDelegate.cancel(videoBookingId, user)
    log.info("Video booking ${booking.videoBookingId} cancelled due to transfer")
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CANCELLED, videoBookingId)
    sendBookingEmails(BookingAction.TRANSFERRED, booking, getReleasedOrTransferredPrisoner(booking.prisoner()), user)
    sendTelemetry(BookingAction.TRANSFERRED, booking, user)
  }

  fun prisonerReleased(videoBookingId: Long, user: ServiceUser) {
    val booking = videoBookingServiceDelegate.cancel(videoBookingId, user)
    log.info("Video booking ${booking.videoBookingId} cancelled due to release")
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CANCELLED, videoBookingId)
    sendBookingEmails(BookingAction.RELEASED, booking, getReleasedOrTransferredPrisoner(booking.prisoner()), user)
    sendTelemetry(BookingAction.RELEASED, booking, user)
  }

  private fun sendTelemetry(action: BookingAction, booking: VideoBooking, user: User) {
    when {
      action == BookingAction.CREATE && booking.isBookingType(COURT) -> CourtBookingCreatedTelemetryEvent(booking)
      action == BookingAction.CREATE && booking.isBookingType(PROBATION) -> ProbationBookingCreatedTelemetryEvent(booking)
      action == BookingAction.AMEND && booking.isBookingType(COURT) -> CourtBookingAmendedTelemetryEvent(booking, user)
      action == BookingAction.AMEND && booking.isBookingType(PROBATION) -> ProbationBookingAmendedTelemetryEvent(booking, user)
      action == BookingAction.CANCEL && booking.isBookingType(COURT) -> CourtBookingCancelledTelemetryEvent.user(booking, user)
      action == BookingAction.CANCEL && booking.isBookingType(PROBATION) -> ProbationBookingCancelledTelemetryEvent.user(booking, user)
      action == BookingAction.TRANSFERRED && booking.isBookingType(COURT) -> CourtBookingCancelledTelemetryEvent.transferred(booking)
      action == BookingAction.TRANSFERRED && booking.isBookingType(PROBATION) -> ProbationBookingCancelledTelemetryEvent.transferred(booking)
      action == BookingAction.RELEASED && booking.isBookingType(COURT) -> CourtBookingCancelledTelemetryEvent.released(booking)
      action == BookingAction.RELEASED && booking.isBookingType(PROBATION) -> ProbationBookingCancelledTelemetryEvent.released(booking)
      else -> null
    }?.let(telemetryService::track)
  }

  private fun getPrisoner(prisonerNumber: String) = prisonerSearchClient.getPrisoner(prisonerNumber)!!.let { Prisoner(it.prisonerNumber, it.prisonId!!, it.firstName, it.lastName, it.dateOfBirth) }

  private fun getReleasedOrTransferredPrisoner(prisonerNumber: String) = prisonerSearchClient.getPrisoner(prisonerNumber)!!.let { Prisoner(it.prisonerNumber, it.lastPrisonId!!, it.firstName, it.lastName, it.dateOfBirth) }

  private fun sendBookingEmails(action: BookingAction, booking: VideoBooking, prisoner: Prisoner, user: User) {
    when (booking.bookingType) {
      COURT -> sendCourtBookingEmails(action, booking, prisoner, user)
      PROBATION -> sendProbationBookingEmails(action, booking, prisoner, user)
    }
  }

  private fun sendCourtBookingEmails(eventType: BookingAction, booking: VideoBooking, prisoner: Prisoner, user: User) {
    val (pre, main, post) = booking.courtAppointments()
    val prison = prisonRepository.findByCode(booking.prisonCode())!!
    val contacts = contactsService.getBookingContacts(booking.videoBookingId, user).withAnEmailAddress()
    val locations = setOfNotNull(pre?.prisonLocationId, main.prisonLocationId, post?.prisonLocationId).mapNotNull { locationsService.getLocationById(it) }.associateBy { it.dpsLocationId }

    val emails = contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.USER -> CourtEmailFactory.user(contact, prisoner, booking, prison, main, pre, post, locations, eventType).takeIf { user is PrisonUser || user is ExternalUser }
        ContactType.COURT -> CourtEmailFactory.court(contact, prisoner, booking, prison, main, pre, post, locations, eventType).takeIf { user is PrisonUser || user is ServiceUser }
        ContactType.PRISON -> CourtEmailFactory.prison(contact, prisoner, booking, prison, contacts, main, pre, post, locations, eventType)
        else -> null
      }
    }

    emails.forEach { courtEmail -> sendEmailAndSaveNotification(courtEmail, booking, eventType) }
  }

  private fun sendProbationBookingEmails(eventType: BookingAction, booking: VideoBooking, prisoner: Prisoner, user: User) {
    val appointment = booking.appointments().single()
    val prison = prisonRepository.findByCode(booking.prisonCode())!!
    val contacts = contactsService.getBookingContacts(booking.videoBookingId, user).withAnEmailAddress()
    val location = locationsService.getLocationById(appointment.prisonLocationId)!!
    val additionalBookingDetail = additionalBookingDetailRepository.findByVideoBooking(booking)

    val emails = contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.USER -> ProbationEmailFactory.user(contact, prisoner, booking, prison, appointment, location, eventType, additionalBookingDetail).takeIf { user is PrisonUser || user is ExternalUser }
        ContactType.PROBATION -> ProbationEmailFactory.probation(contact, prisoner, booking, prison, appointment, location, eventType, additionalBookingDetail).takeIf { user is PrisonUser || user is ServiceUser }
        ContactType.PRISON -> ProbationEmailFactory.prison(contact, prisoner, booking, prison, appointment, location, eventType, contacts, additionalBookingDetail)
        else -> null
      }
    }

    emails.forEach { probationEmail -> sendEmailAndSaveNotification(probationEmail, booking, eventType) }
  }

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

  private fun VideoBooking.courtAppointments(): Triple<PrisonAppointment?, PrisonAppointment, PrisonAppointment?> = appointments().prisonAppointmentsForCourtHearing()

  private fun Collection<BookingContact>.withAnEmailAddress() = filter { it.email != null }

  private fun Collection<PrisonAppointment>.prisonAppointmentsForCourtHearing() = Triple(pre(), main(), post())

  private fun Collection<PrisonAppointment>.pre() = singleOrNull { it.isType("VLB_COURT_PRE") }

  private fun Collection<PrisonAppointment>.main() = single { it.isType("VLB_COURT_MAIN") }

  private fun Collection<PrisonAppointment>.post() = singleOrNull { it.isType("VLB_COURT_POST") }
}

enum class BookingAction {
  CREATE,
  AMEND,
  CANCEL,
  COURT_HEARING_LINK_REMINDER,
  PROBATION_OFFICER_DETAILS_REMINDER,
  RELEASED,
  TRANSFERRED,
}
