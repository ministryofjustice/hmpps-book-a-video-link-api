package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService

/**
 * This facade exists to ensure booking related transactions are fully committed prior to sending any emails.
 */
@Component
class BookingFacade(
  private val createVideoBookingService: CreateVideoBookingService,
  private val amendVideoBookingService: AmendVideoBookingService,
  private val bookingContactsService: BookingContactsService,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val prisonRepository: PrisonRepository,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
  private val outboundEventsService: OutboundEventsService,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun create(bookingRequest: CreateVideoBookingRequest, username: String): Long {
    val (booking, prisoner) = createVideoBookingService.create(bookingRequest, username)
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CREATED, booking.videoBookingId)
    sendBookingEmails(BookingAction.CREATE, bookingRequest.bookingType!!, booking, prisoner)
    return booking.videoBookingId
  }

  fun amend(videoBookingId: Long, bookingRequest: AmendVideoBookingRequest, username: String): Long {
    val (booking, prisoner) = amendVideoBookingService.amend(videoBookingId, bookingRequest, username)
    // TODO: Emit VIDEO_BOOKING_AMENDED domain event
    sendBookingEmails(BookingAction.AMEND, bookingRequest.bookingType!!, booking, prisoner)
    return booking.videoBookingId
  }

  private fun sendBookingEmails(action: BookingAction, bookingType: BookingType, booking: VideoBooking, prisoner: Prisoner) {
    when (bookingType) {
      BookingType.COURT -> sendCourtBookingEmails(action, booking, prisoner)
      BookingType.PROBATION -> sendProbationBookingEmails(action, booking, prisoner)
    }
  }

  private fun sendCourtBookingEmails(eventType: BookingAction, booking: VideoBooking, prisoner: Prisoner) {
    val (pre, main, post) = getCourtAppointments(booking)
    val prison = prisonRepository.findByCode(prisoner.prisonCode)!!
    val contacts = bookingContactsService.getBookingContacts(booking.videoBookingId).allContactsWithAnEmailAddress()
    val locations = locationsInsidePrisonClient.getLocationsByKeys(setOfNotNull(pre?.prisonLocKey, main.prisonLocKey, post?.prisonLocKey)).associateBy { it.key }

    contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.OWNER -> createCourtOwnerEmail(contact, prisoner, booking, prison, main, pre, post, locations, eventType)
        ContactType.PRISON -> createCourtPrisonEmail(contact, prisoner, booking, prison, contacts, main, pre, post, locations, eventType)
        else -> log.info("BOOKINGS: No contacts found for video booking ID ${booking.videoBookingId}").let { null }
      }
    }.forEach { email ->
      sendEmailAndSaveNotification(email, booking, eventType)
    }
  }

  private fun sendProbationBookingEmails(action: BookingAction, booking: VideoBooking, prisoner: Prisoner) {
    log.info("TODO - send probation booking emails.")
  }

  private fun sendEmailAndSaveNotification(email: Email, booking: VideoBooking, action: BookingAction) {
    val reason = when (action) {
      BookingAction.CREATE -> "New court booking request"
      BookingAction.AMEND -> "Amended court booking request"
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

  private fun createCourtOwnerEmail(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    main: PrisonAppointment,
    pre: PrisonAppointment?,
    post: PrisonAppointment?,
    locations: Map<String, Location>,
    action: BookingAction,
  ): Email {
    return when (action) {
      BookingAction.CREATE -> NewCourtBookingEmail(
        address = contact.email!!,
        userName = contact.name ?: "Book Video",
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prisonerNumber = prisoner.prisonerNumber,
        court = booking.court!!.description,
        prison = prison.name,
        date = main.appointmentDate,
        preAppointmentInfo = pre?.appointmentInformation(locations),
        mainAppointmentInfo = main.appointmentInformation(locations),
        postAppointmentInfo = post?.appointmentInformation(locations),
        comments = booking.comments,
      )
      BookingAction.AMEND -> AmendedCourtBookingEmail(
        address = contact.email!!,
        userName = contact.name ?: "Book Video",
        prisonerFirstName = prisoner.firstName,
        prisonerLastName = prisoner.lastName,
        prisonerNumber = prisoner.prisonerNumber,
        court = booking.court!!.description,
        date = main.appointmentDate,
        preAppointmentInfo = pre?.appointmentInformation(locations),
        mainAppointmentInfo = main.appointmentInformation(locations),
        postAppointmentInfo = post?.appointmentInformation(locations),
        comments = booking.comments,
      )
    }
  }

  private fun createCourtPrisonEmail(
    contact: BookingContact,
    prisoner: Prisoner,
    booking: VideoBooking,
    prison: Prison,
    contacts: Collection<BookingContact>,
    main: PrisonAppointment,
    pre: PrisonAppointment?,
    post: PrisonAppointment?,
    locations: Map<String, Location>,
    action: BookingAction,
  ): Email {
    val primaryCourtContact = contacts.primaryCourtContact()
    return when (action) {
      BookingAction.CREATE -> {
        if (primaryCourtContact != null) {
          NewCourtBookingPrisonCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            courtEmailAddress = primaryCourtContact.email!!,
            prison = prison.name,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
          )
        } else {
          NewCourtBookingPrisonNoCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            prison = prison.name,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
          )
        }
      }
      BookingAction.AMEND -> {
        if (primaryCourtContact != null) {
          AmendedCourtBookingPrisonCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            courtEmailAddress = primaryCourtContact.email!!,
            prison = prison.name,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
          )
        } else {
          AmendedCourtBookingPrisonNoCourtEmail(
            address = contact.email!!,
            prisonerFirstName = prisoner.firstName,
            prisonerLastName = prisoner.lastName,
            prisonerNumber = prisoner.prisonerNumber,
            court = booking.court!!.description,
            prison = prison.name,
            date = main.appointmentDate,
            preAppointmentInfo = pre?.appointmentInformation(locations),
            mainAppointmentInfo = main.appointmentInformation(locations),
            postAppointmentInfo = post?.appointmentInformation(locations),
            comments = booking.comments,
          )
        }
      }
    }
  }

  private fun getCourtAppointments(booking: VideoBooking): Triple<PrisonAppointment?, PrisonAppointment, PrisonAppointment?> {
    return prisonAppointmentRepository.findByVideoBooking(booking).prisonAppointmentsForCourtHearing()
  }

  private fun Collection<BookingContact>.primaryCourtContact() = singleOrNull { it.contactType == ContactType.COURT && it.primaryContact }

  private fun Collection<BookingContact>.allContactsWithAnEmailAddress() = filter { it.email != null }

  private fun Collection<PrisonAppointment>.prisonAppointmentsForCourtHearing() = Triple(pre(), main(), post())

  private fun Collection<PrisonAppointment>.pre() = singleOrNull { it.appointmentType == "VLB_COURT_PRE" }

  private fun Collection<PrisonAppointment>.main() = single { it.appointmentType == "VLB_COURT_MAIN" }

  private fun Collection<PrisonAppointment>.post() = singleOrNull { it.appointmentType == "VLB_COURT_POST" }

  private fun PrisonAppointment.appointmentInformation(locations: Map<String, Location>) =
    "${locations.room(prisonLocKey)} - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"

  private fun Map<String, Location>.room(key: String) = this[key]?.localName ?: ""
}

enum class BookingAction {
  CREATE,
  AMEND,
}
