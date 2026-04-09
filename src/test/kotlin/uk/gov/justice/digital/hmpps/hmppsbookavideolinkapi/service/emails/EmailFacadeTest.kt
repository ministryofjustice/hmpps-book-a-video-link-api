package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.additionalDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.locationAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingAction
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ChangeType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ContactsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ServiceUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtHearingLinkReminderEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.TransferredCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationOfficerDetailsReminderEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toPrisonerDetails
import java.time.LocalDate
import java.time.LocalDateTime.now
import java.time.LocalTime
import java.util.UUID

class EmailFacadeTest {
  private val emailCaptor = argumentCaptor<VideoBookingEmail>()
  private val contactsService: ContactsService = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val emailService: EmailService = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val notificationCaptor = argumentCaptor<Notification>()
  private val locationsService: LocationsService = mock()
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository = mock()
  private val facade = EmailFacade(
    prisonRepository,
    contactsService,
    locationsService,
    additionalBookingDetailRepository,
    emailService,
    notificationRepository,
  )
  private val courtBooking = courtBooking(notesForStaff = "court notes for staff")
    .addAppointment(
      prison = prison(prisonCode = WANDSWORTH),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )
  private val courtBookingCreatedByPrison = courtBooking(createdByPrison = true, notesForStaff = "court notes for staff", notesForPrisoners = "court notes for prisoners")
    .addAppointment(
      prison = prison(prisonCode = WANDSWORTH),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )

  private val probationBookingAtBirminghamPrison = probationBooking(notesForStaff = "probation notes for staff")
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "654321",
      appointmentType = AppointmentType.VLB_PROBATION.name,
      locationId = birminghamLocation.id,
      date = tomorrow(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
    )

  private val emailNotificationId = UUID.randomUUID()

  @BeforeEach
  fun before() {
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(locationsService.getLocationById(wandsworthLocation.id)) doReturn wandsworthLocation.toModel()
    whenever(locationsService.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation.toModel()
    whenever(locationsService.getLocationById(birminghamLocation.id)) doReturn birminghamLocation.toModel(locationAttributes().copy(prisonVideoUrl = "birmingham-video-url"))
  }

  @Nested
  @DisplayName("Create bookings")
  inner class CreateBooking {
    @Test
    fun `should send events and emails on creation of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)

      whenever(emailService.send(any<NewCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(BookingAction.CREATE, courtBooking, prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH), COURT_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf NewCourtBookingUserEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to COURT_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf NewCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CREATE"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CREATE"
      }
    }

    @Test
    fun `should send events and emails on creation of court booking by a prison user`() {
      setupCourtPrimaryContactsFor(PRISON_USER_WANDSWORTH)

      whenever(emailService.send(any<NewCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.sendEmails(BookingAction.CREATE, courtBookingCreatedByPrison, prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH), PRISON_USER_WANDSWORTH)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf NewCourtBookingUserEmail::class.java
        address isEqualTo PRISON_USER_WANDSWORTH.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_WANDSWORTH.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf NewCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBookingCreatedByPrison
        reason isEqualTo "CREATE"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBookingCreatedByPrison
        reason isEqualTo "CREATE"
      }
    }

    @Test
    fun `should send events and emails on creation of probation booking by probation user`() {
      setupProbationPrimaryContacts(PROBATION_USER)

      val prisoner = prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder")

      whenever(emailService.send(any<NewProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(additionalBookingDetailRepository.findByVideoBooking(probationBookingAtBirminghamPrison)) doReturn additionalDetails(probationBookingAtBirminghamPrison, "probation officer name", "probation.officer@email.com", "0114 2345678")

      facade.sendEmails(BookingAction.CREATE, probationBookingAtBirminghamPrison, prisoner, PROBATION_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2

      with(emailCaptor.firstValue) {
        this isInstanceOf NewProbationBookingUserEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PROBATION_USER.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf NewProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }

      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }
    }

    @Test
    fun `should send events and emails on creation of probation booking by prison user`() {
      setupProbationPrimaryContacts(PRISON_USER_BIRMINGHAM)

      val prisoner = prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder")

      whenever(emailService.send(any<NewProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(additionalBookingDetailRepository.findByVideoBooking(probationBookingAtBirminghamPrison)) doReturn additionalDetails(probationBookingAtBirminghamPrison, "probation officer name", "probation.officer@email.com", "0114 2345678")

      facade.sendEmails(BookingAction.CREATE, probationBookingAtBirminghamPrison, prisoner, PRISON_USER_BIRMINGHAM)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2

      with(emailCaptor.firstValue) {
        this isInstanceOf NewProbationBookingUserEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_BIRMINGHAM.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf NewProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }

      with(notificationCaptor.secondValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }
    }
  }

  @Nested
  @DisplayName("Cancel bookings")
  inner class CancelBooking {
    @Test
    fun `should send events and emails on cancellation of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)

      val prisoner = prisoner(courtBooking.prisoner(), WANDSWORTH, "Bob", "Builder")

      whenever(emailService.send(any<CancelledCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(BookingAction.CANCEL, courtBooking.cancel(COURT_USER), prisoner, COURT_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledCourtBookingUserEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to COURT_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }
    }

    @Test
    fun `should send events and emails on cancellation of court booking by prison user`() {
      setupCourtPrimaryContactsFor(PRISON_USER_WANDSWORTH)

      val prisoner = prisoner(courtBooking.prisoner(), WANDSWORTH, "Bob", "Builder")

      whenever(emailService.send(any<CancelledCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.sendEmails(BookingAction.CANCEL, courtBooking.cancel(PRISON_USER_WANDSWORTH), prisoner, PRISON_USER_WANDSWORTH)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledCourtBookingUserEmail::class.java
        address isEqualTo PRISON_USER_WANDSWORTH.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_WANDSWORTH.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }
    }

    @Test
    fun `should send emails but no events on cancellation of a court booking by service user`() {
      contactsService.stub {
        on { getBookingContacts(any(), eq(SERVICE_USER)) } doReturn listOf(
          bookingContact(contactType = ContactType.COURT, email = COURT_USER.email, name = COURT_USER.name),
        )
      }

      val prisoner = prisoner(courtBooking.prisoner(), WANDSWORTH, "Bob", "Builder")

      whenever(emailService.send(any<CancelledCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.sendEmails(BookingAction.CANCEL, courtBooking.cancel(SERVICE_USER), prisoner, SERVICE_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      with(emailCaptor.allValues.single()) {
        this isInstanceOf CancelledCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
        )
      }

      with(notificationCaptor.allValues.single()) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }
    }

    @Test
    fun `should send events and emails on cancellation of probation booking by probation user`() {
      setupProbationPrimaryContacts(PROBATION_USER)

      val prisoner = prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder")

      whenever(emailService.send(any<CancelledProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(BookingAction.CANCEL, probationBookingAtBirminghamPrison.cancel(PROBATION_USER), prisoner, PROBATION_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledProbationBookingUserEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "userName" to PROBATION_USER.name,
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }
    }

    @Test
    fun `should send events and emails on cancellation of probation booking by prison user`() {
      setupProbationPrimaryContacts(PRISON_USER_BIRMINGHAM)

      val prisoner = prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder")

      whenever(emailService.send(any<CancelledProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.sendEmails(BookingAction.CANCEL, probationBookingAtBirminghamPrison.cancel(PRISON_USER_BIRMINGHAM), prisoner, PRISON_USER_BIRMINGHAM)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledProbationBookingUserEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_BIRMINGHAM.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }
    }

    @Test
    fun `should send emails but no events on cancellation of a probation booking by service user`() {
      contactsService.stub {
        on { getBookingContacts(any(), eq(SERVICE_USER)) } doReturn listOf(
          bookingContact(contactType = ContactType.PROBATION, email = PROBATION_USER.email, name = PROBATION_USER.name),
        )
      }

      val prisoner = prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder")

      whenever(emailService.send(any<CancelledProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.sendEmails(BookingAction.CANCEL, probationBookingAtBirminghamPrison.cancel(SERVICE_USER), prisoner, SERVICE_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      with(emailCaptor.allValues.single()) {
        this isInstanceOf CancelledProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      with(notificationCaptor.allValues.single()) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }
    }
  }

  @Nested
  @DisplayName("Amend bookings")
  inner class AmendBooking {
    @Test
    fun `should send events and emails on amendment of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)

      whenever(emailService.send(any<AmendedCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      courtBooking.apply {
        amendedTime = now()
        amendedBy = COURT_USER.username
      }

      facade.sendEmails(BookingAction.AMEND, courtBooking, prisoner(prisonerNumber = "123456", prisonCode = WANDSWORTH), COURT_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedCourtBookingUserEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to COURT_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "prison" to "Wandsworth",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }
    }

    @Test
    fun `should send events and emails on amendment of court booking by prison user`() {
      setupCourtPrimaryContactsFor(PRISON_USER_WANDSWORTH)

      whenever(emailService.send(any<AmendedCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      courtBooking.apply {
        amendedTime = now()
        amendedBy = COURT_USER.username
      }

      facade.sendEmails(BookingAction.AMEND, courtBooking, prisoner(prisonerNumber = "123456", prisonCode = WANDSWORTH), PRISON_USER_WANDSWORTH)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedCourtBookingUserEmail::class.java
        address isEqualTo PRISON_USER_WANDSWORTH.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_WANDSWORTH.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "prison" to "Wandsworth",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }
    }

    @Test
    fun `should send events and reduced emails on amendment of court booking by prison user`() {
      setupCourtPrimaryContactsFor(PRISON_USER_WANDSWORTH)

      whenever(emailService.send(any<AmendedCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      courtBooking.apply {
        amendedTime = now()
        amendedBy = COURT_USER.username
      }

      facade.sendEmails(BookingAction.AMEND, courtBooking, prisoner(prisonerNumber = "123456", prisonCode = WANDSWORTH), PRISON_USER_WANDSWORTH, ChangeType.PRISON)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 1
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedCourtBookingUserEmail::class.java
        address isEqualTo PRISON_USER_WANDSWORTH.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_WANDSWORTH.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "prison" to "Wandsworth",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }

      notificationCaptor.allValues hasSize 1
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }
    }

    @Test
    fun `should send events and emails on amendment of probation booking by probation user`() {
      setupProbationPrimaryContacts(PROBATION_USER)

      whenever(emailService.send(any<AmendedProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      probationBookingAtBirminghamPrison.apply {
        amendedTime = now()
        amendedBy = PROBATION_USER.username
      }

      facade.sendEmails(BookingAction.AMEND, probationBookingAtBirminghamPrison, prisoner(prisonerNumber = "654321", prisonCode = BIRMINGHAM), PROBATION_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedProbationBookingUserEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "userName" to PROBATION_USER.name,
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }
    }

    @Test
    fun `should send events and emails on amendment of probation booking by prison user`() {
      setupProbationPrimaryContacts(PRISON_USER_BIRMINGHAM)

      whenever(emailService.send(any<AmendedProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      probationBookingAtBirminghamPrison.apply {
        amendedTime = now()
        amendedBy = PRISON_USER_BIRMINGHAM.username
      }

      facade.sendEmails(BookingAction.AMEND, probationBookingAtBirminghamPrison, prisoner(prisonerNumber = "654321", prisonCode = BIRMINGHAM), PRISON_USER_BIRMINGHAM)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedProbationBookingUserEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_BIRMINGHAM.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }
    }

    @Test
    fun `should send events and reduced emails on amendment of probation booking by prison user`() {
      setupProbationPrimaryContacts(PRISON_USER_BIRMINGHAM)

      whenever(emailService.send(any<AmendedProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      probationBookingAtBirminghamPrison.apply {
        amendedTime = now()
        amendedBy = PRISON_USER_BIRMINGHAM.username
      }

      facade.sendEmails(BookingAction.AMEND, probationBookingAtBirminghamPrison, prisoner(prisonerNumber = "654321", prisonCode = BIRMINGHAM), PRISON_USER_BIRMINGHAM, ChangeType.PRISON)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 1
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedProbationBookingUserEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_BIRMINGHAM.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 1
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }
    }
  }

  @Nested
  @DisplayName("Prisoner transfers")
  inner class PrisonerTransfer {
    @Test
    fun `should send events and emails on transfer of prisoner by service user for a court booking`() {
      val prisoner = Prisoner(
        prisonerNumber = courtBooking.prisoner(),
        prisonId = "TRN",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      ).toPrisonerDetails()

      whenever(contactsService.getBookingContacts(any(), anyOrNull())) doReturn listOf(
        bookingContact(contactType = ContactType.PRISON, email = "jon@prison.com", name = "Jon"),
      )

      whenever(emailService.send(any<TransferredCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(BookingAction.TRANSFERRED, courtBooking.cancel(SERVICE_USER), prisoner, SERVICE_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 1
      with(emailCaptor.firstValue) {
        this isInstanceOf TransferredCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo "jon@prison.com"
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
        )
      }

      notificationCaptor.allValues hasSize 1
      with(notificationCaptor.firstValue) {
        email isEqualTo "jon@prison.com"
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "TRANSFERRED"
      }
    }

    @Test
    fun `should send events and emails on transfer of prisoner by service user for a probation booking`() {
      val prisoner = Prisoner(
        prisonerNumber = probationBookingAtBirminghamPrison.prisoner(),
        prisonId = "TRN",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      ).toPrisonerDetails()

      setupProbationPrimaryContacts(SERVICE_USER)

      whenever(emailService.send(any<TransferredProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(emailService.send(any<TransferredProbationBookingPrisonProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(BookingAction.TRANSFERRED, probationBookingAtBirminghamPrison.apply { cancel(SERVICE_USER) }, prisoner, SERVICE_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf TransferredProbationBookingPrisonProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "probationEmailAddress" to "probation.user@probation.com",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf TransferredProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "TRANSFERRED"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "TRANSFERRED"
      }
    }
  }

  @Nested
  @DisplayName("Prisoner releases")
  inner class PrisonerRelease {
    @Test
    fun `should send events and emails on release of prisoner by service user for a court booking`() {
      val prisoner = Prisoner(
        prisonerNumber = courtBooking.prisoner(),
        prisonId = "TRN",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      ).toPrisonerDetails()

      setupCourtPrimaryContactsFor(SERVICE_USER)

      whenever(emailService.send(any<ReleasedCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")
      whenever(emailService.send(any<ReleasedCourtBookingPrisonCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(BookingAction.RELEASED, courtBooking.apply { cancel(SERVICE_USER) }, prisoner, SERVICE_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf ReleasedCourtBookingPrisonCourtEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf ReleasedCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }
    }

    @Test
    fun `should send events and emails on release of prisoner by service user for a probation booking`() {
      val prisoner = Prisoner(
        prisonerNumber = probationBookingAtBirminghamPrison.prisoner(),
        prisonId = "TRN",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      ).toPrisonerDetails()

      setupProbationPrimaryContacts(SERVICE_USER)

      whenever(emailService.send(any<ReleasedProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(emailService.send(any<ReleasedProbationBookingPrisonProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(BookingAction.RELEASED, probationBookingAtBirminghamPrison.apply { cancel(SERVICE_USER) }, prisoner, SERVICE_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf ReleasedProbationBookingPrisonProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "probationEmailAddress" to "probation.user@probation.com",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf ReleasedProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }
    }
  }

  @Nested
  @DisplayName("Court hearing link reminder")
  inner class CourtHearingLinkReminder {
    @Test
    fun `should send an email to the court contact to remind them to add a court hearing link`() {
      val courtBooking = courtBooking(cvpLinkDetails = null, notesForStaff = "court notes for staff")
        .addAppointment(
          prison = prison(prisonCode = WANDSWORTH),
          prisonerNumber = "123456",
          appointmentType = "VLB_COURT_MAIN",
          date = LocalDate.of(2100, 1, 1),
          startTime = LocalTime.of(11, 0),
          endTime = LocalTime.of(11, 30),
          locationId = wandsworthLocation.id,
        )

      val prisoner = Prisoner(
        prisonerNumber = courtBooking.prisoner(),
        prisonId = "WWI",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      ).toPrisonerDetails()

      setupCourtPrimaryContactsFor(SERVICE_USER)

      whenever(emailService.send(any<CourtHearingLinkReminderEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.sendEmails(BookingAction.COURT_HEARING_LINK_REMINDER, courtBooking, prisoner, SERVICE_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 1
      with(emailCaptor.firstValue) {
        this isInstanceOf CourtHearingLinkReminderEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "Not yet known",
          "bookingId" to "0",
        )
      }

      notificationCaptor.allValues hasSize 1
      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "COURT_HEARING_LINK_REMINDER"
      }
    }
  }

  @Nested
  @DisplayName("Probation officer details reminder")
  inner class ProbationOfficerDetailsReminder {
    @Test
    fun `should send an email to the probation contact to remind them to add missing probation officer details`() {
      val prisoner = Prisoner(
        prisonerNumber = probationBookingAtBirminghamPrison.prisoner(),
        prisonId = "WWI",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = BIRMINGHAM,
      ).toPrisonerDetails()

      setupCourtPrimaryContactsFor(SERVICE_USER)

      whenever(emailService.send(any<ProbationOfficerDetailsReminderEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.sendEmails(BookingAction.PROBATION_OFFICER_DETAILS_REMINDER, probationBookingAtBirminghamPrison, prisoner, SERVICE_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 1
      with(emailCaptor.firstValue) {
        this isInstanceOf ProbationOfficerDetailsReminderEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "probationTeam" to "probation team description",
          "meetingType" to "PSR",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "bookingId" to "0",
          "prisonVideoUrl" to "birmingham-video-url",
        )
      }

      notificationCaptor.allValues hasSize 1
      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "PROBATION_OFFICER_DETAILS_REMINDER"
      }
    }
  }

  private fun setupProbationPrimaryContacts(user: User) {
    val mayBeEmail = when (user) {
      is ExternalUser -> user.email
      is PrisonUser -> user.email
      else -> null
    }

    // Not ideal but have logic in test to mimic stubbed service behaviour regarding matching email addresses for contacts
    contactsService.stub {
      on { getBookingContacts(any(), eq(user)) } doReturn listOfNotNull(
        bookingContact(contactType = ContactType.USER, email = mayBeEmail, name = user.name).takeUnless { user is ServiceUser },
        bookingContact(contactType = ContactType.PRISON, email = PRISON_USER_BIRMINGHAM.email, name = PRISON_USER_BIRMINGHAM.name).takeUnless { it.email == mayBeEmail },
        bookingContact(contactType = ContactType.PROBATION, email = PROBATION_USER.email, name = PROBATION_USER.name).takeUnless { it.email == mayBeEmail },
      )
    }
  }

  private fun setupCourtPrimaryContactsFor(user: User) {
    val mayBeEmail = when (user) {
      is ExternalUser -> user.email
      is PrisonUser -> user.email
      else -> null
    }

    // Not ideal but have logic in test to mimic stubbed service behaviour regarding matching email addresses for contacts
    contactsService.stub {
      on { getBookingContacts(any(), eq(user)) } doReturn listOfNotNull(
        bookingContact(contactType = ContactType.USER, email = mayBeEmail, name = user.name).takeUnless { user is ServiceUser },
        bookingContact(contactType = ContactType.PRISON, email = PRISON_USER_BIRMINGHAM.email, name = PRISON_USER_BIRMINGHAM.name).takeUnless { it.email == mayBeEmail },
        bookingContact(contactType = ContactType.COURT, email = COURT_USER.email, name = COURT_USER.name).takeUnless { it.email == mayBeEmail },
        bookingContact(contactType = ContactType.PROBATION, email = PROBATION_USER.email, name = PROBATION_USER.name).takeUnless { it.email == mayBeEmail },
      )
    }
  }
}
