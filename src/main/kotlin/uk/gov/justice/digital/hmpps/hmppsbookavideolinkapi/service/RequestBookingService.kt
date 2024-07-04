package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.UnknownPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository

@Service
class RequestBookingService(
  private val emailService: EmailService,
  private val contactsService: ContactsService,
  private val appointmentsService: AppointmentsService,
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
  private val prisonRepository: PrisonRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
  private val notificationRepository: NotificationRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun request(request: RequestVideoBookingRequest, username: String) {
    when (request.bookingType!!) {
      BookingType.COURT -> sendCourtRequestEmails(request, username)
      BookingType.PROBATION -> sendProbationRequestEmails(request, username)
    }
  }

  private fun sendCourtRequestEmails(request: RequestVideoBookingRequest, username: String) {
    appointmentsService.checkCourtAppointments(request.prisoner().appointments, request.prisoner().prisonCode!!)

    val (pre, main, post) = getCourtAppointments(request.prisoner())

    val prison = prisonRepository.findByCode(request.prisoner().prisonCode!!)
      ?.also { require(it.enabled) { "Prison with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Prison with code ${request.prisoner().prisonCode} not found")

    val court = courtRepository.findByCode(request.courtCode!!)
      ?.also { require(it.enabled) { "Court with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Court with code ${request.courtCode} not found")

    val hearingType = referenceCodeRepository.findByGroupCodeAndCode("COURT_HEARING_TYPE", request.courtHearingType!!.toString())
      ?: throw EntityNotFoundException("Court hearing type with code ${request.courtHearingType} not found")

    val locations = locationsInsidePrisonClient.getLocationsByKeys(setOfNotNull(pre?.locationKey, main.locationKey, post?.locationKey)).associateBy { it.key }
    val contacts = contactsService.getContactsForCourtBookingRequest(court, prison, username).allContactsWithAnEmailAddress()

    contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.OWNER -> createCourtOwnerEmail(contact, request, court, prison, hearingType, main, pre, post, locations)
        ContactType.PRISON -> createCourtPrisonEmail(contact, request, court, prison, contacts, hearingType, main, pre, post, locations)
        else -> null
      }
    }.forEach { email ->
      sendEmailAndSaveNotification(email)
    }
  }

  private fun sendProbationRequestEmails(request: RequestVideoBookingRequest, username: String) {
    appointmentsService.checkProbationAppointments(request.prisoner().appointments, request.prisoner().prisonCode!!)

    val appointment = request.prisoner().appointments.single()

    val prison = prisonRepository.findByCode(request.prisoner().prisonCode!!)
      ?.also { require(it.enabled) { "Prison with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Prison with code ${request.prisoner().prisonCode} not found")

    val probationTeam = probationTeamRepository.findByCode(request.probationTeamCode!!)
      ?.also { require(it.enabled) { "Probation team with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Probation team with code ${request.probationTeamCode} not found")

    val meetingType = referenceCodeRepository.findByGroupCodeAndCode("PROBATION_MEETING_TYPE", request.probationMeetingType!!.toString())
      ?: throw EntityNotFoundException("Probation meeting type with code ${request.probationMeetingType} not found")

    val locations = locationsInsidePrisonClient.getLocationsByKeys(setOf(appointment.locationKey!!)).associateBy { it.key }
    val contacts = contactsService.getContactsForProbationBookingRequest(probationTeam, prison, username).allContactsWithAnEmailAddress()

    contacts.mapNotNull { contact ->
      when (contact.contactType) {
        ContactType.OWNER -> createProbationOwnerEmail(contact, request, probationTeam, prison, meetingType, appointment, locations)
        ContactType.PRISON -> createProbationPrisonEmail(contact, request, probationTeam, prison, contacts, meetingType, appointment, locations)
        else -> null
      }
    }.forEach { email ->
      sendEmailAndSaveNotification(email)
    }
  }

  private fun getCourtAppointments(prisoner: UnknownPrisonerDetails): Triple<Appointment?, Appointment, Appointment?> {
    return prisoner.appointments.appointmentsForCourtHearing()
  }

  private fun sendEmailAndSaveNotification(email: Email) {
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
      log.info("BOOKINGS: Failed to send booking request email for prisoner.")
    }
  }

  private fun createCourtOwnerEmail(
    contact: Contact,
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

  private fun createCourtPrisonEmail(
    contact: Contact,
    request: RequestVideoBookingRequest,
    court: Court,
    prison: Prison,
    contacts: Collection<Contact>,
    hearingType: ReferenceCode,
    main: Appointment,
    pre: Appointment?,
    post: Appointment?,
    locations: Map<String, Location>,
  ): Email {
    val primaryCourtContact = contacts.primaryCourtContact()
    if (primaryCourtContact != null) {
      return CourtBookingRequestPrisonCourtEmail(
        address = contact.email!!,
        prisonerFirstName = request.prisoner().firstName!!,
        prisonerLastName = request.prisoner().lastName!!,
        dateOfBirth = request.prisoner().dateOfBirth!!,
        court = court.description,
        courtEmailAddress = primaryCourtContact.email!!,
        prison = prison.name,
        date = main.date!!,
        hearingType = hearingType.description,
        preAppointmentInfo = pre?.appointmentInformation(locations),
        mainAppointmentInfo = main.appointmentInformation(locations),
        postAppointmentInfo = post?.appointmentInformation(locations),
        comments = request.comments,
      )
    } else {
      return CourtBookingRequestPrisonNoCourtEmail(
        address = contact.email!!,
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
    }
  }

  private fun createProbationOwnerEmail(
    contact: Contact,
    request: RequestVideoBookingRequest,
    probationTeam: ProbationTeam,
    prison: Prison,
    meetingType: ReferenceCode,
    appointment: Appointment,
    locations: Map<String, Location>,
  ) = ProbationBookingRequestOwnerEmail(
    address = contact.email!!,
    userName = contact.name ?: "Book Video",
    prisonerFirstName = request.prisoner().firstName!!,
    prisonerLastName = request.prisoner().lastName!!,
    dateOfBirth = request.prisoner().dateOfBirth!!,
    probationTeam = probationTeam.description,
    prison = prison.name,
    date = appointment.date!!,
    meetingType = meetingType.description,
    appointmentInfo = appointment.appointmentInformation(locations),
    comments = request.comments,
  )

  private fun createProbationPrisonEmail(
    contact: Contact,
    request: RequestVideoBookingRequest,
    probationTeam: ProbationTeam,
    prison: Prison,
    contacts: Collection<Contact>,
    meetingType: ReferenceCode,
    appointment: Appointment,
    locations: Map<String, Location>,
  ): Email {
    val primaryProbationTeamContact = contacts.primaryProbationTeamContact()
    if (primaryProbationTeamContact != null) {
      return ProbationBookingRequestPrisonProbationTeamEmail(
        address = contact.email!!,
        prisonerFirstName = request.prisoner().firstName!!,
        prisonerLastName = request.prisoner().lastName!!,
        dateOfBirth = request.prisoner().dateOfBirth!!,
        probationTeam = probationTeam.description,
        probationTeamEmailAddress = primaryProbationTeamContact.email!!,
        prison = prison.name,
        date = appointment.date!!,
        meetingType = meetingType.description,
        appointmentInfo = appointment.appointmentInformation(locations),
        comments = request.comments,
      )
    } else {
      return ProbationBookingRequestPrisonNoProbationTeamEmail(
        address = contact.email!!,
        prisonerFirstName = request.prisoner().firstName!!,
        prisonerLastName = request.prisoner().lastName!!,
        dateOfBirth = request.prisoner().dateOfBirth!!,
        probationTeam = probationTeam.description,
        prison = prison.name,
        date = appointment.date!!,
        meetingType = meetingType.description,
        appointmentInfo = appointment.appointmentInformation(locations),
        comments = request.comments,
      )
    }
  }

  private fun Collection<Contact>.primaryCourtContact() = singleOrNull { it.contactType == ContactType.COURT && it.primaryContact }

  private fun Collection<Contact>.primaryProbationTeamContact() = singleOrNull { it.contactType == ContactType.PROBATION && it.primaryContact }

  private fun Collection<Contact>.allContactsWithAnEmailAddress() = filter { it.email != null }

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
