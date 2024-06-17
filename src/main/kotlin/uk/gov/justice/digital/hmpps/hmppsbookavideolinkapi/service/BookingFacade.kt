package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
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

    when (bookingRequest.bookingType!!) {
      BookingType.COURT -> sendNewCourtBookingEmails(booking, prisoner)
      BookingType.PROBATION -> sendNewProbationBookingEmail(booking, prisoner)
    }

    return booking.videoBookingId
  }

  private fun sendNewCourtBookingEmails(booking: VideoBooking, prisoner: Prisoner) {
    val (pre, main, post) = prisonAppointmentRepository.findByVideoBooking(booking).prisonAppointmentsForCourtHearing()
    val prison = prisonRepository.findByCode(prisoner.prisonCode)!!
    val contacts = bookingContactsService.getBookingContacts(booking.videoBookingId).allContactsWithAnEmailAddress()
    val locations = locationsInsidePrisonClient.getLocationsByKeys(setOfNotNull(pre?.prisonLocKey, main.prisonLocKey, post?.prisonLocKey)).associateBy { it.key }

    contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.OWNER -> NewCourtBookingEmail(
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

        ContactType.PRISON -> {
          val primaryCourtContact = contacts.primaryCourtContact()

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

        else -> log.info("No contacts found for video booking ID ${booking.videoBookingId}").let { null }
      }
    }.forEach { email ->
      emailService.send(email).onSuccess { (govNotifyId, templateId) ->
        notificationRepository.saveAndFlush(
          Notification(
            videoBooking = booking,
            email = email.address,
            govNotifyNotificationId = govNotifyId,
            templateName = templateId,
            reason = "New court booking request",
          ),
        )
      }.onFailure { log.info("BOOKINGS: Failed to send new court booking email.") }
    }
  }

  private fun Collection<BookingContact>.primaryCourtContact() =
    allContactsWithAnEmailAddress().singleOrNull { it.contactType == ContactType.COURT && it.primaryContact }

  private fun sendNewProbationBookingEmail(booking: VideoBooking, prisoner: Prisoner) {
    log.info("TODO - send new probation booking email.")

    // Agreed with Tim will be using a ContactService to pull back the necessary contact information related to the booking just created.
    // Multiple contacts will result in multiple emails i.e. one email per contact.
  }

  private fun Collection<BookingContact>.allContactsWithAnEmailAddress() = filter { it.email != null }

  private fun Collection<PrisonAppointment>.prisonAppointmentsForCourtHearing() = Triple(pre(), main(), post())

  private fun Collection<PrisonAppointment>.pre() = singleOrNull { it.appointmentType == "VLB_COURT_PRE" }

  private fun Collection<PrisonAppointment>.main() = single { it.appointmentType == "VLB_COURT_MAIN" }

  private fun Collection<PrisonAppointment>.post() = singleOrNull { it.appointmentType == "VLB_COURT_POST" }

  private fun PrisonAppointment.appointmentInformation(locations: Map<String, Location>) =
    "${locations.room(prisonLocKey)} - ${startTime.toHourMinuteStyle()} to ${endTime.toHourMinuteStyle()}"

  private fun Map<String, Location>.room(key: String) = this[key]?.localName ?: ""
}
