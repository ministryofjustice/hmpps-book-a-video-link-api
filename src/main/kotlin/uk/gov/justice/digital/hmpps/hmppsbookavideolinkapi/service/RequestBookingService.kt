package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.UnknownPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService

@Component
class RequestBookingService(
  private val courtRepository: CourtRepository,
  private val prisonRepository: PrisonRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
  private val emailService: EmailService,
  private val notificationRepository: NotificationRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun request(request: RequestVideoBookingRequest, username: String) {
    when (request.bookingType!!) {
      BookingType.COURT -> sendCourtRequestEmails(request)
      BookingType.PROBATION -> log.info("TODO - send probation request emails.")
    }
  }

  private fun sendCourtRequestEmails(request: RequestVideoBookingRequest) {
    val (pre, main, post) = getCourtAppointments(request.prisoner())

    val prison = prisonRepository.findByCode(request.prisoner().prisonCode!!)
      ?.also { require(it.enabled) { "Prison with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Prison with code ${request.prisoner().prisonCode} not found")

    val court = courtRepository.findByCode(request.courtCode!!)
      ?.also { require(it.enabled) { "Court with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Court with code ${request.courtCode} not found")

    val hearingType = referenceCodeRepository.findByGroupCodeAndCode("COURT_HEARING_TYPE", request.courtHearingType!!.toString())!!
    val locations = locationsInsidePrisonClient.getLocationsByKeys(setOfNotNull(pre?.locationKey, main.locationKey, post?.locationKey)).associateBy { it.key }

    contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.OWNER -> createCourtOwnerEmail(contact, request, court, prison, hearingType, main, pre, post, locations)
        ContactType.PRISON -> createCourtPrisonEmail(contact, prisoner, booking, prison, contacts, main, pre, post, locations, eventType)
        else -> log.info("BOOKINGS: No contacts found for video booking ID ${booking.videoBookingId}").let { null }
      }
    }.forEach { email ->
      sendEmailAndSaveNotification(email, request.prisoner())
    }
  }

  private fun getCourtAppointments(prisoner: UnknownPrisonerDetails): Triple<Appointment?, Appointment, Appointment?> {
    return prisoner.appointments.appointmentsForCourtHearing()
  }

  private fun sendEmailAndSaveNotification(email: Email, prisoner: UnknownPrisonerDetails) {
    emailService.send(email).onSuccess { (govNotifyId, templateId) ->
      notificationRepository.saveAndFlush(
        Notification(
          email = email.address,
          govNotifyNotificationId = govNotifyId,
          templateName = templateId,
          reason = "Booking request",
        ),
      )
    }.onFailure {
      log.info("BOOKINGS: Failed to send booking request email for ${prisoner.firstName.plus(" " + prisoner.lastName)}.")
    }
  }

  private fun createCourtOwnerEmail(
    contact: BookingContact,
    request: RequestVideoBookingRequest,
    court: Court,
    prison: Prison,
    hearingType: ReferenceCode,
    main: Appointment,
    pre: Appointment?,
    post: Appointment?,
    locations: Map<String, Location>,
  ) = CourtBookingRequestOwnerEmail(
        address = contact.email!!,
        userName = contact.name ?: "Book Video",
        prisonerFirstName = request.prisoner().firstName!!,
        prisonerLastName = request.prisoner().lastName!!,
        dateOfBirth = request.prisoner().dateOfBirth!!,
        court = court.description,
        prison = prison.name,
        date = main.date!!,
        hearingType = hearingType.description,
        preAppointmentInfo = pre?.appointmentInformation(locations),
        mainAppointmentInfo = main.appointmentInformation(locations),
        postAppointmentInfo = post?.appointmentInformation(locations),
        comments = request.comments,
    )

  private fun Collection<Appointment>.appointmentsForCourtHearing() = Triple(pre(), main(), post())

  private fun Collection<Appointment>.pre() = singleOrNull { it.type == AppointmentType.VLB_COURT_PRE }

  private fun Collection<Appointment>.main() = single { it.type == AppointmentType.VLB_COURT_MAIN }

  private fun Collection<Appointment>.post() = singleOrNull { it.type == AppointmentType.VLB_COURT_POST }

  private fun Appointment.appointmentInformation(locations: Map<String, Location>) =
    "${locations.room(locationKey!!)} - ${startTime!!.toHourMinuteStyle()} to ${endTime!!.toHourMinuteStyle()}"

  private fun Map<String, Location>.room(key: String) = this[key]?.localName ?: ""

  // We will only be requesting appointments for one single prisoner as part of the initial rollout.
  private fun RequestVideoBookingRequest.prisoner() = prisoners.first()
}
