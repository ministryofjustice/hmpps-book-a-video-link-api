package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestedAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.UnknownPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonNoProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestUserEmail

@Service
class RequestBookingService(
  private val emailService: EmailService,
  private val contactsService: ContactsService,
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
  private val prisonRepository: PrisonRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
  private val notificationRepository: NotificationRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun request(request: RequestVideoBookingRequest, user: ExternalUser) {
    when (request.bookingType!!) {
      BookingType.COURT -> processCourtBookingRequest(request, user)
      BookingType.PROBATION -> processProbationBookingRequest(request, user)
    }
  }

  private fun processCourtBookingRequest(request: RequestVideoBookingRequest, user: User) {
    val prisoner = request.prisoner()
    checkCourtAppointments(prisoner.appointments, prisoner.prisonCode!!, user)

    val (pre, main, post) = getCourtAppointments(prisoner)

    val prison = fetchPrison(prisoner.prisonCode)
    val court = fetchCourt(request.courtCode!!)
    val hearingType = fetchReferenceCode("COURT_HEARING_TYPE", request.courtHearingType!!.toString())

    val contacts = contactsService.getContactsForCourtBookingRequest(court, prison, user).allContactsWithAnEmailAddress()

    sendEmails(contacts) { contact ->
      when (contact.contactType) {
        ContactType.USER -> createCourtUserEmail(contact, request, court, prison, hearingType, main, pre, post)
        ContactType.PRISON -> createCourtPrisonEmail(contact, request, court, prison, contacts, hearingType, main, pre, post)
        else -> null
      }
    }
  }

  private fun processProbationBookingRequest(request: RequestVideoBookingRequest, user: User) {
    val prisoner = request.prisoner()
    checkProbationAppointments(prisoner.appointments)

    val appointment = prisoner.appointments.single()
    val prison = fetchPrison(prisoner.prisonCode!!)
    val probationTeam = fetchProbationTeam(request.probationTeamCode!!)
    val meetingType = fetchReferenceCode("PROBATION_MEETING_TYPE", request.probationMeetingType!!.toString())

    val contacts = contactsService.getContactsForProbationBookingRequest(probationTeam, prison, user).allContactsWithAnEmailAddress()

    sendEmails(contacts) { contact ->
      when (contact.contactType) {
        ContactType.USER -> createProbationUserEmail(contact, request, probationTeam, prison, meetingType, appointment)
        ContactType.PRISON -> createProbationPrisonEmail(contact, request, probationTeam, prison, contacts, meetingType, appointment)
        else -> null
      }
    }
  }

  private fun checkCourtAppointments(appointments: List<RequestedAppointment>, prisonCode: String, user: User) {
    appointments.checkCourtAppointmentTypesOnly()
    appointments.checkSuppliedCourtAppointmentDateAndTimesDoNotOverlap()
  }

  private fun List<RequestedAppointment>.checkCourtAppointmentTypesOnly() {
    require(
      size <= 3 &&
        count { it.type == AppointmentType.VLB_COURT_PRE } <= 1 &&
        count { it.type == AppointmentType.VLB_COURT_POST } <= 1 &&
        count { it.type == AppointmentType.VLB_COURT_MAIN } == 1 &&
        all { it.type!!.isCourt },
    ) {
      "Court bookings can only have one pre hearing, one hearing and one post hearing."
    }
  }

  private fun List<RequestedAppointment>.checkSuppliedCourtAppointmentDateAndTimesDoNotOverlap() {
    val pre = singleOrNull { it.type == AppointmentType.VLB_COURT_PRE }
    val hearing = single { it.type == AppointmentType.VLB_COURT_MAIN }
    val post = singleOrNull { it.type == AppointmentType.VLB_COURT_POST }

    require((pre == null || pre.isBefore(hearing)) && (post == null || hearing.isBefore(post))) {
      "Requested court booking appointments must not overlap."
    }
  }

  private fun checkProbationAppointments(appointments: List<RequestedAppointment>) {
    with(appointments.single()) {
      require(type!!.isProbation) {
        "Appointment type $type is not valid for probation appointments"
      }
    }
  }

  private fun fetchPrison(prisonCode: String): Prison = prisonRepository.findByCode(prisonCode)
    ?.also { require(it.enabled) { "Prison with code ${it.code} is not enabled" } }
    ?: throw EntityNotFoundException("Prison with code $prisonCode not found")

  private fun fetchCourt(courtCode: String): Court = courtRepository.findByCode(courtCode)
    ?.also { require(it.enabled) { "Court with code ${it.code} is not enabled" } }
    ?: throw EntityNotFoundException("Court with code $courtCode not found")

  private fun fetchProbationTeam(probationTeamCode: String): ProbationTeam = probationTeamRepository.findByCode(probationTeamCode)
    ?.also { require(it.enabled) { "Probation team with code ${it.code} is not enabled" } }
    ?: throw EntityNotFoundException("Probation team with code $probationTeamCode not found")

  private fun fetchReferenceCode(groupCode: String, code: String): ReferenceCode = referenceCodeRepository.findByGroupCodeAndCode(groupCode, code)
    ?: throw EntityNotFoundException("$groupCode with code $code not found")

  private fun getCourtAppointments(prisoner: UnknownPrisonerDetails): Triple<RequestedAppointment?, RequestedAppointment, RequestedAppointment?> = prisoner.appointments.appointmentsForCourtHearing()

  private fun sendEmails(contacts: List<Contact>, emailGenerator: (Contact) -> Email?) {
    contacts.mapNotNull(emailGenerator).forEach { email ->
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
  }

  private fun createCourtUserEmail(
    contact: Contact,
    request: RequestVideoBookingRequest,
    court: Court,
    prison: Prison,
    hearingType: ReferenceCode,
    main: RequestedAppointment,
    pre: RequestedAppointment?,
    post: RequestedAppointment?,
  ) = CourtBookingRequestUserEmail(
    address = contact.email!!,
    userName = contact.name ?: "Book Video",
    prisonerFirstName = request.prisoner().firstName!!,
    prisonerLastName = request.prisoner().lastName!!,
    dateOfBirth = request.prisoner().dateOfBirth!!,
    court = court.description,
    prison = prison.name,
    date = main.date!!,
    hearingType = hearingType.description,
    preAppointmentInfo = pre?.appointmentInformation(),
    mainAppointmentInfo = main.appointmentInformation(),
    postAppointmentInfo = post?.appointmentInformation(),
    comments = request.comments,
    courtHearingLink = request.videoLinkUrl,
  )

  private fun createCourtPrisonEmail(
    contact: Contact,
    request: RequestVideoBookingRequest,
    court: Court,
    prison: Prison,
    contacts: Collection<Contact>,
    hearingType: ReferenceCode,
    main: RequestedAppointment,
    pre: RequestedAppointment?,
    post: RequestedAppointment?,
  ): Email {
    val primaryCourtContact = contacts.primaryContact(ContactType.COURT)
    return if (primaryCourtContact != null) {
      CourtBookingRequestPrisonCourtEmail(
        address = contact.email!!,
        prisonerFirstName = request.prisoner().firstName!!,
        prisonerLastName = request.prisoner().lastName!!,
        dateOfBirth = request.prisoner().dateOfBirth!!,
        court = court.description,
        courtEmailAddress = primaryCourtContact.email!!,
        prison = prison.name,
        date = main.date!!,
        hearingType = hearingType.description,
        preAppointmentInfo = pre?.appointmentInformation(),
        mainAppointmentInfo = main.appointmentInformation(),
        postAppointmentInfo = post?.appointmentInformation(),
        comments = request.comments,
        courtHearingLink = request.videoLinkUrl,
      )
    } else {
      CourtBookingRequestPrisonNoCourtEmail(
        address = contact.email!!,
        prisonerFirstName = request.prisoner().firstName!!,
        prisonerLastName = request.prisoner().lastName!!,
        dateOfBirth = request.prisoner().dateOfBirth!!,
        court = court.description,
        prison = prison.name,
        date = main.date!!,
        hearingType = hearingType.description,
        preAppointmentInfo = pre?.appointmentInformation(),
        mainAppointmentInfo = main.appointmentInformation(),
        postAppointmentInfo = post?.appointmentInformation(),
        comments = request.comments,
        courtHearingLink = request.videoLinkUrl,
      )
    }
  }

  private fun createProbationUserEmail(
    contact: Contact,
    request: RequestVideoBookingRequest,
    probationTeam: ProbationTeam,
    prison: Prison,
    meetingType: ReferenceCode,
    appointment: RequestedAppointment,
  ) = ProbationBookingRequestUserEmail(
    address = contact.email!!,
    userName = contact.name ?: "Book Video",
    prisonerFirstName = request.prisoner().firstName!!,
    prisonerLastName = request.prisoner().lastName!!,
    dateOfBirth = request.prisoner().dateOfBirth!!,
    probationTeam = probationTeam.description,
    prison = prison.name,
    date = appointment.date!!,
    meetingType = meetingType.description,
    appointmentInfo = appointment.appointmentInformation(),
    comments = request.comments,
  )

  private fun createProbationPrisonEmail(
    contact: Contact,
    request: RequestVideoBookingRequest,
    probationTeam: ProbationTeam,
    prison: Prison,
    contacts: Collection<Contact>,
    meetingType: ReferenceCode,
    appointment: RequestedAppointment,
  ): Email {
    val primaryProbationTeamContact = contacts.primaryContact(ContactType.PROBATION)
    return if (primaryProbationTeamContact != null) {
      ProbationBookingRequestPrisonProbationTeamEmail(
        address = contact.email!!,
        prisonerFirstName = request.prisoner().firstName!!,
        prisonerLastName = request.prisoner().lastName!!,
        dateOfBirth = request.prisoner().dateOfBirth!!,
        probationTeam = probationTeam.description,
        probationTeamEmailAddress = primaryProbationTeamContact.email!!,
        prison = prison.name,
        date = appointment.date!!,
        meetingType = meetingType.description,
        appointmentInfo = appointment.appointmentInformation(),
        comments = request.comments,
      )
    } else {
      ProbationBookingRequestPrisonNoProbationTeamEmail(
        address = contact.email!!,
        prisonerFirstName = request.prisoner().firstName!!,
        prisonerLastName = request.prisoner().lastName!!,
        dateOfBirth = request.prisoner().dateOfBirth!!,
        probationTeam = probationTeam.description,
        prison = prison.name,
        date = appointment.date!!,
        meetingType = meetingType.description,
        appointmentInfo = appointment.appointmentInformation(),
        comments = request.comments,
      )
    }
  }

  private fun Collection<Contact>.primaryContact(type: ContactType) = singleOrNull { it.contactType == type && it.primaryContact }

  private fun Collection<Contact>.allContactsWithAnEmailAddress() = filter { it.email != null }

  private fun Collection<RequestedAppointment>.appointmentsForCourtHearing() = Triple(pre(), main(), post())

  private fun Collection<RequestedAppointment>.pre() = singleOrNull { it.type == AppointmentType.VLB_COURT_PRE }

  private fun Collection<RequestedAppointment>.main() = single { it.type == AppointmentType.VLB_COURT_MAIN }

  private fun Collection<RequestedAppointment>.post() = singleOrNull { it.type == AppointmentType.VLB_COURT_POST }

  private fun RequestedAppointment.appointmentInformation() = "${startTime!!.toHourMinuteStyle()} to ${endTime!!.toHourMinuteStyle()}"

  private fun RequestedAppointment.isBefore(other: RequestedAppointment): Boolean = this.date!! <= other.date && this.endTime!! <= other.startTime

  // We will only be requesting appointments for one single prisoner as part of the initial rollout.
  private fun RequestVideoBookingRequest.prisoner() = prisoners.first()
}
