package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.requestCourtVideoLinkRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.requestProbationVideoLinkRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonNoProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestUserEmail
import java.time.LocalTime
import java.util.UUID

class RequestBookingServiceTest {
  private val emailCaptor = argumentCaptor<Email>()
  private val notificationCaptor = argumentCaptor<Notification>()
  private val emailService: EmailService = mock()
  private val contactsService: ContactsService = mock()
  private val appointmentsService: AppointmentsService = mock()
  private val courtRepository: CourtRepository = mock()
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val referenceCodeRepository: ReferenceCodeRepository = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val service = RequestBookingService(
    emailService,
    contactsService,
    appointmentsService,
    courtRepository,
    probationTeamRepository,
    prisonRepository,
    referenceCodeRepository,
    notificationRepository,
    locationsInsidePrisonClient,
  )

  @BeforeEach
  fun before() {
    whenever(courtRepository.findByCode(DERBY_JUSTICE_CENTRE)) doReturn court(DERBY_JUSTICE_CENTRE)
    whenever(probationTeamRepository.findByCode(BLACKPOOL_MC_PPOC)) doReturn probationTeam(BLACKPOOL_MC_PPOC)
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(referenceCodeRepository.findByGroupCodeAndCode(eq("COURT_HEARING_TYPE"), any())) doReturn courtHearingType("Tribunal")
    whenever(referenceCodeRepository.findByGroupCodeAndCode(eq("PROBATION_MEETING_TYPE"), any())) doReturn courtHearingType("Pre-sentence report")
    whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(wandsworthLocation.key))) doReturn listOf(wandsworthLocation)
    whenever(contactsService.getContactsForCourtBookingRequest(any(), any(), any())) doReturn listOf(
      contact(contactType = ContactType.USER, email = "jon@somewhere.com", name = "Jon"),
      contact(contactType = ContactType.PRISON, email = "jon@prison.com", name = "Jon"),
    )
    whenever(contactsService.getContactsForProbationBookingRequest(any(), any(), any())) doReturn listOf(
      contact(contactType = ContactType.USER, email = "jon@somewhere.com", name = "Jon"),
      contact(contactType = ContactType.PRISON, email = "jon@prison.com", name = "Jon"),
    )
  }

  @Test
  fun `should send emails to the requester and to the prison on a court booking request`() {
    val bookingRequest = requestCourtVideoLinkRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val notificationId = UUID.randomUUID()

    whenever(emailService.send(any<CourtBookingRequestUserEmail>())) doReturn Result.success(notificationId to "court template id")
    whenever(emailService.send(any<CourtBookingRequestPrisonNoCourtEmail>())) doReturn Result.success(notificationId to "prison template id")

    service.request(bookingRequest, courtUser("court user"))

    inOrder(emailService, notificationRepository) {
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2
    with(emailCaptor.firstValue) {
      this isInstanceOf CourtBookingRequestUserEmail::class.java
      address isEqualTo "jon@somewhere.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "userName" to "Jon",
        "court" to DERBY_JUSTICE_CENTRE,
        "prison" to "Wandsworth",
        "prisonerName" to "John Smith",
        "dateOfBirth" to "1 Jan 1970",
        "date" to tomorrow().toMediumFormatStyle(),
        "hearingType" to "Tribunal",
        "preAppointmentInfo" to "Not required",
        "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
        "postAppointmentInfo" to "Not required",
        "comments" to "court booking comments",
      )
    }
    with(emailCaptor.secondValue) {
      this isInstanceOf CourtBookingRequestPrisonNoCourtEmail::class.java
      address isEqualTo "jon@prison.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "court" to DERBY_JUSTICE_CENTRE,
        "prison" to "Wandsworth",
        "prisonerName" to "John Smith",
        "dateOfBirth" to "1 Jan 1970",
        "date" to tomorrow().toMediumFormatStyle(),
        "hearingType" to "Tribunal",
        "preAppointmentInfo" to "Not required",
        "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
        "postAppointmentInfo" to "Not required",
        "comments" to "court booking comments",
      )
    }

    notificationCaptor.allValues hasSize 2
    with(notificationCaptor.firstValue) {
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "court template id"
      govNotifyNotificationId isEqualTo notificationId
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo notificationId
    }
  }

  @Test
  fun `should throw error if the requested court is not enabled`() {
    whenever(courtRepository.findByCode(DERBY_JUSTICE_CENTRE)) doReturn court(DERBY_JUSTICE_CENTRE, enabled = false)

    val bookingRequest = requestCourtVideoLinkRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val error = assertThrows<IllegalArgumentException> { service.request(bookingRequest, courtUser("court user")) }
    error.message isEqualTo "Court with code $DERBY_JUSTICE_CENTRE is not enabled"

    verify(emailService, never()).send(any())
    verify(notificationRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should throw error if the requested court is not found`() {
    whenever(courtRepository.findByCode(DERBY_JUSTICE_CENTRE)) doReturn null

    val bookingRequest = requestCourtVideoLinkRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val error = assertThrows<EntityNotFoundException> { service.request(bookingRequest, courtUser("court user")) }
    error.message isEqualTo "Court with code $DERBY_JUSTICE_CENTRE not found"

    verify(emailService, never()).send(any())
    verify(notificationRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should throw error if the requested prison is not enabled during court booking request`() {
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH, enabled = false)

    val bookingRequest = requestCourtVideoLinkRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val error = assertThrows<IllegalArgumentException> { service.request(bookingRequest, courtUser("court user")) }
    error.message isEqualTo "Prison with code $WANDSWORTH is not enabled"

    verify(emailService, never()).send(any())
    verify(notificationRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should throw error if the requested prison is not found during court booking request`() {
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn null

    val bookingRequest = requestCourtVideoLinkRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val error = assertThrows<EntityNotFoundException> { service.request(bookingRequest, courtUser("court user")) }
    error.message isEqualTo "Prison with code $WANDSWORTH not found"

    verify(emailService, never()).send(any())
    verify(notificationRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should throw error if the requested court hearing type is not found`() {
    whenever(referenceCodeRepository.findByGroupCodeAndCode(eq("COURT_HEARING_TYPE"), any())) doReturn null

    val bookingRequest = requestCourtVideoLinkRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val error = assertThrows<EntityNotFoundException> { service.request(bookingRequest, courtUser("court user")) }
    error.message isEqualTo "COURT_HEARING_TYPE with code TRIBUNAL not found"

    verify(emailService, never()).send(any())
    verify(notificationRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should send emails to the requester and to the prison on a probation booking request`() {
    val bookingRequest = requestProbationVideoLinkRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val notificationId = UUID.randomUUID()

    whenever(emailService.send(any<ProbationBookingRequestUserEmail>())) doReturn Result.success(notificationId to "probation template id")
    whenever(emailService.send(any<ProbationBookingRequestPrisonNoProbationTeamEmail>())) doReturn Result.success(notificationId to "prison template id")

    service.request(bookingRequest, probationUser("probation user"))

    inOrder(emailService, notificationRepository) {
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2
    with(emailCaptor.firstValue) {
      this isInstanceOf ProbationBookingRequestUserEmail::class.java
      address isEqualTo "jon@somewhere.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "userName" to "Jon",
        "probationTeam" to "probation team description",
        "prison" to "Wandsworth",
        "prisonerName" to "John Smith",
        "dateOfBirth" to "1 Jan 1970",
        "date" to tomorrow().toMediumFormatStyle(),
        "meetingType" to "Pre-sentence report",
        "appointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
        "comments" to "probation booking comments",
      )
    }
    with(emailCaptor.secondValue) {
      this isInstanceOf ProbationBookingRequestPrisonNoProbationTeamEmail::class.java
      address isEqualTo "jon@prison.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "probationTeam" to "probation team description",
        "prison" to "Wandsworth",
        "prisonerName" to "John Smith",
        "dateOfBirth" to "1 Jan 1970",
        "date" to tomorrow().toMediumFormatStyle(),
        "meetingType" to "Pre-sentence report",
        "appointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
        "comments" to "probation booking comments",
      )
    }

    notificationCaptor.allValues hasSize 2
    with(notificationCaptor.firstValue) {
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "probation template id"
      govNotifyNotificationId isEqualTo notificationId
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo notificationId
    }
  }

  @Test
  fun `should throw error if the requested probation team is not enabled`() {
    whenever(probationTeamRepository.findByCode(BLACKPOOL_MC_PPOC)) doReturn probationTeam(BLACKPOOL_MC_PPOC, enabled = false)

    val bookingRequest = requestProbationVideoLinkRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val error = assertThrows<IllegalArgumentException> { service.request(bookingRequest, probationUser("probation user")) }
    error.message isEqualTo "Probation team with code $BLACKPOOL_MC_PPOC is not enabled"

    verify(emailService, never()).send(any())
    verify(notificationRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should throw error if the requested probation team is not found`() {
    whenever(probationTeamRepository.findByCode(BLACKPOOL_MC_PPOC)) doReturn null

    val bookingRequest = requestProbationVideoLinkRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val error = assertThrows<EntityNotFoundException> { service.request(bookingRequest, probationUser("probation user")) }
    error.message isEqualTo "Probation team with code $BLACKPOOL_MC_PPOC not found"

    verify(emailService, never()).send(any())
    verify(notificationRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should throw error if the requested prison is not enabled during probation booking request`() {
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH, enabled = false)

    val bookingRequest = requestProbationVideoLinkRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val error = assertThrows<IllegalArgumentException> { service.request(bookingRequest, probationUser("probation user")) }
    error.message isEqualTo "Prison with code $WANDSWORTH is not enabled"

    verify(emailService, never()).send(any())
    verify(notificationRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should throw error if the requested prison is not found during probation booking request`() {
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn null

    val bookingRequest = requestProbationVideoLinkRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val error = assertThrows<EntityNotFoundException> { service.request(bookingRequest, probationUser("probation user")) }
    error.message isEqualTo "Prison with code $WANDSWORTH not found"

    verify(emailService, never()).send(any())
    verify(notificationRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should throw error if the requested probation meeting type is not found`() {
    whenever(referenceCodeRepository.findByGroupCodeAndCode(eq("PROBATION_MEETING_TYPE"), any())) doReturn null

    val bookingRequest = requestProbationVideoLinkRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      prisonCode = WANDSWORTH,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      location = wandsworthLocation,
    )

    val error = assertThrows<EntityNotFoundException> { service.request(bookingRequest, probationUser("probation user")) }
    error.message isEqualTo "PROBATION_MEETING_TYPE with code PSR not found"

    verify(emailService, never()).send(any())
    verify(notificationRepository, never()).saveAndFlush(any())
  }
}
