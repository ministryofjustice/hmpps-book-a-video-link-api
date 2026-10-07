package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DELIUS_PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.locationAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AdditionalBookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ContactsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.DeliusUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ServiceUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.BookingDetails
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
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class ReplacementEmailFacadeTest {
  private val emailCaptor = argumentCaptor<VideoBookingEmail>()
  private val contactsService: ContactsService = mock()
  private val emailService: EmailService = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val notificationCaptor = argumentCaptor<Notification>()
  private val locationsService: LocationsService = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val facade = ReplacementEmailFacade(contactsService, locationsService, emailService, notificationRepository, videoBookingRepository)

  private val courtBooking = courtBooking(notesForStaff = "court notes for staff")
    .withMainCourtPrisonAppointment(
      prisonCode = WANDSWORTH,
      prisonerNumber = "123456",
      location = wandsworthLocation,
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
    )
  private val courtBookingCreatedByPrison = courtBooking(
    createdByPrison = true,
    notesForStaff = "court notes for staff",
    notesForPrisoners = "court notes for prisoners",
  )
    .withMainCourtPrisonAppointment(
      prisonCode = WANDSWORTH,
      prisonerNumber = "123456",
      location = wandsworthLocation,
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
    )

  private val probationBookingAtBirminghamPrison = probationBooking(notesForStaff = "probation notes for staff")
    .withProbationPrisonAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "654321",
      location = birminghamLocation,
      date = tomorrow(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
    )

  private val emailNotificationId = UUID.randomUUID()

  @BeforeEach
  fun before() {
    setOf(wandsworthLocation, birminghamLocation).forEach {
      whenever(locationsService.getLocationById(it.id)) doReturn it.toModel()
    }
  }

  @Nested
  @DisplayName("Create bookings")
  inner class CreateBooking {
    private val courtBookingDetails = BookingDetails.create(
      courtBooking.toModel(setOf(wandsworthLocation.toModel()))
        .copy(videoLinkBookingId = 1L),
      prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH),
    )
    private val courtByPrisonBookingDetails = BookingDetails.create(
      courtBookingCreatedByPrison.toModel(setOf(wandsworthLocation.toModel()))
        .copy(videoLinkBookingId = 2L),
      prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH),
    )
    private val probationBookingDetails = BookingDetails.create(
      probationBookingAtBirminghamPrison.toModel(
        setOf(birminghamLocation.toModel(locationAttributes().copy(prisonVideoUrl = "decorated-video-link-url"))),
      ).copy(
        videoLinkBookingId = 3L,
        additionalBookingDetails = AdditionalBookingDetails(
          contactName = "probation officer name",
          contactEmail = "probation.officer@email.com",
          contactNumber = "0114 2345678",
        ),
      ),
      prisoner(prisonerNumber = probationBookingAtBirminghamPrison.prisoner(), prisonCode = BIRMINGHAM),
    )

    @BeforeEach
    fun before() {
      whenever(videoBookingRepository.findById(courtBookingDetails.videoLinkBookingId)) doReturn Optional.of(courtBooking)
      whenever(videoBookingRepository.findById(courtByPrisonBookingDetails.videoLinkBookingId)) doReturn Optional.of(courtBookingCreatedByPrison)
      whenever(videoBookingRepository.findById(probationBookingDetails.videoLinkBookingId)) doReturn Optional.of(probationBookingAtBirminghamPrison)
    }

    @Test
    fun `should send events and emails on creation of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)

      whenever(emailService.send(any<NewCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(courtBookingDetails, COURT_USER)

      verifyServiceAndRepositoryCalls(courtBookingDetails)

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
      with(emailCaptor.lastValue) {
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

      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CREATE"
      }
      with(notificationCaptor.lastValue) {
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

      facade.sendEmails(courtByPrisonBookingDetails, PRISON_USER_WANDSWORTH)

      verifyServiceAndRepositoryCalls(courtByPrisonBookingDetails)

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
      with(emailCaptor.lastValue) {
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

      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBookingCreatedByPrison
        reason isEqualTo "CREATE"
      }
      with(notificationCaptor.lastValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBookingCreatedByPrison
        reason isEqualTo "CREATE"
      }
    }

    @Test
    fun `should send events and emails on creation of probation booking by external probation user`() {
      setupProbationPrimaryContacts(PROBATION_USER)
      whenever(emailService.send(any<NewProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.sendEmails(probationBookingDetails, PROBATION_USER)

      verifyServiceAndRepositoryCalls(probationBookingDetails)

      with(emailCaptor.firstValue) {
        this isInstanceOf NewProbationBookingUserEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PROBATION_USER.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(emailCaptor.lastValue) {
        this isInstanceOf NewProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }

      with(notificationCaptor.lastValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }
    }

    @Test
    fun `should send events and emails on creation of probation booking by Delius probation user`() {
      setupProbationPrimaryContacts(DELIUS_PROBATION_USER)
      whenever(emailService.send(any<NewProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.sendEmails(probationBookingDetails, DELIUS_PROBATION_USER)

      verifyServiceAndRepositoryCalls(probationBookingDetails)

      with(emailCaptor.firstValue) {
        this isInstanceOf NewProbationBookingUserEmail::class.java
        address isEqualTo DELIUS_PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to DELIUS_PROBATION_USER.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(emailCaptor.lastValue) {
        this isInstanceOf NewProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo DELIUS_PROBATION_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }

      with(notificationCaptor.lastValue) {
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
      whenever(emailService.send(any<NewProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.sendEmails(probationBookingDetails, PRISON_USER_BIRMINGHAM)

      verifyServiceAndRepositoryCalls(probationBookingDetails)

      with(emailCaptor.firstValue) {
        this isInstanceOf NewProbationBookingUserEmail::class.java
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
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(emailCaptor.lastValue) {
        this isInstanceOf NewProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }

      with(notificationCaptor.lastValue) {
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
    private val cancelledCourtBookingDetails = BookingDetails.cancelled(
      courtBooking.toModel(setOf(wandsworthLocation.toModel()))
        .copy(videoLinkBookingId = 1L, statusCode = BookingStatus.CANCELLED),
      prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH),
    )
    private val cancelledCourtByPrisonBookingDetails = BookingDetails.cancelled(
      courtBookingCreatedByPrison.toModel(setOf(wandsworthLocation.toModel()))
        .copy(videoLinkBookingId = 2L, statusCode = BookingStatus.CANCELLED),
      prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH),
    )
    private val cancelledProbationBookingDetails = BookingDetails.cancelled(
      probationBookingAtBirminghamPrison.toModel(
        setOf(birminghamLocation.toModel(locationAttributes().copy(prisonVideoUrl = "decorated-video-link-url"))),
      ).copy(
        videoLinkBookingId = 3L,
        additionalBookingDetails = AdditionalBookingDetails(
          contactName = "probation officer name",
          contactEmail = "probation.officer@email.com",
          contactNumber = "0114 2345678",
        ),
        statusCode = BookingStatus.CANCELLED,
      ),
      prisoner(prisonerNumber = probationBookingAtBirminghamPrison.prisoner(), prisonCode = BIRMINGHAM),
    )

    @BeforeEach
    fun before() {
      whenever(videoBookingRepository.findById(cancelledCourtBookingDetails.videoLinkBookingId)) doReturn Optional.of(courtBooking)
      whenever(videoBookingRepository.findById(cancelledCourtByPrisonBookingDetails.videoLinkBookingId)) doReturn Optional.of(courtBookingCreatedByPrison)
      whenever(videoBookingRepository.findById(cancelledProbationBookingDetails.videoLinkBookingId)) doReturn Optional.of(probationBookingAtBirminghamPrison)
    }

    @Test
    fun `should send events and emails on cancellation of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)
      whenever(emailService.send(any<CancelledCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(cancelledCourtBookingDetails, COURT_USER)

      verifyServiceAndRepositoryCalls(cancelledCourtBookingDetails)

      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledCourtBookingUserEmail::class.java
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
        )
      }
      with(emailCaptor.lastValue) {
        this isInstanceOf CancelledCourtBookingPrisonNoCourtEmail::class.java
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
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.lastValue) {
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
      whenever(emailService.send(any<CancelledCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.sendEmails(cancelledCourtByPrisonBookingDetails, PRISON_USER_WANDSWORTH)

      verifyServiceAndRepositoryCalls(cancelledCourtByPrisonBookingDetails)

      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledCourtBookingUserEmail::class.java
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
        )
      }
      with(emailCaptor.lastValue) {
        this isInstanceOf CancelledCourtBookingCourtEmail::class.java
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
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.lastValue) {
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
        on { getCourtBookingContacts(any(), any(), anyList(), eq(SERVICE_USER)) } doReturn listOf(
          bookingContact(contactType = ContactType.COURT, email = COURT_USER.email, name = COURT_USER.name),
        )
      }
      whenever(emailService.send(any<CancelledCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.sendEmails(cancelledCourtBookingDetails, SERVICE_USER)

      inOrder(emailService, videoBookingRepository, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(videoBookingRepository).findById(cancelledCourtBookingDetails.videoLinkBookingId)
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      with(emailCaptor.singleValue) {
        this isInstanceOf CancelledCourtBookingCourtEmail::class.java
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
        )
      }

      with(notificationCaptor.singleValue) {
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
      whenever(emailService.send(any<CancelledProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(cancelledProbationBookingDetails, PROBATION_USER)

      verifyServiceAndRepositoryCalls(cancelledProbationBookingDetails)

      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledProbationBookingUserEmail::class.java
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
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }
      with(emailCaptor.lastValue) {
        this isInstanceOf CancelledProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.lastValue) {
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
      whenever(emailService.send(any<CancelledProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.sendEmails(cancelledProbationBookingDetails, PRISON_USER_BIRMINGHAM)

      verifyServiceAndRepositoryCalls(cancelledProbationBookingDetails)

      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledProbationBookingUserEmail::class.java
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
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(emailCaptor.lastValue) {
        this isInstanceOf CancelledProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.lastValue) {
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
        on { getProbationBookingContacts(any(), any(), any(), eq(SERVICE_USER)) } doReturn listOf(
          bookingContact(contactType = ContactType.PROBATION, email = PROBATION_USER.email, name = PROBATION_USER.name),
        )
      }
      whenever(emailService.send(any<CancelledProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.sendEmails(cancelledProbationBookingDetails, SERVICE_USER)

      inOrder(emailService, videoBookingRepository, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(videoBookingRepository).findById(cancelledProbationBookingDetails.videoLinkBookingId)
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      with(emailCaptor.singleValue) {
        this isInstanceOf CancelledProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.singleValue) {
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
    private val courtBookingDto =
      courtBooking.toModel(setOf(wandsworthLocation.toModel())).copy(videoLinkBookingId = 1L)
    private val amendedCourtBookingDetails = BookingDetails.amended(
      courtBookingDto,
      courtBookingDto,
      prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH),
    )
    private val probationBookingDto = probationBookingAtBirminghamPrison.toModel(
      setOf(birminghamLocation.toModel(locationAttributes().copy(prisonVideoUrl = "decorated-video-link-url"))),
    ).copy(
      videoLinkBookingId = 3L,
      additionalBookingDetails = AdditionalBookingDetails(
        contactName = "probation officer name",
        contactEmail = "probation.officer@email.com",
        contactNumber = "0114 2345678",
      ),
    )
    private val amendedProbationBookingDetails = BookingDetails.amended(
      probationBookingDto,
      probationBookingDto,
      prisoner(prisonerNumber = probationBookingAtBirminghamPrison.prisoner(), prisonCode = BIRMINGHAM),
    )

    @BeforeEach
    fun before() {
      whenever(videoBookingRepository.findById(amendedCourtBookingDetails.videoLinkBookingId)) doReturn Optional.of(courtBooking)
      whenever(videoBookingRepository.findById(amendedProbationBookingDetails.videoLinkBookingId)) doReturn Optional.of(probationBookingAtBirminghamPrison)
    }

    @Test
    fun `should send events and emails on amendment of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)
      whenever(emailService.send(any<AmendedCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(amendedCourtBookingDetails, COURT_USER)

      verifyServiceAndRepositoryCalls(amendedCourtBookingDetails)

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
      with(emailCaptor.lastValue) {
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

      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.lastValue) {
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

      facade.sendEmails(amendedCourtBookingDetails, PRISON_USER_WANDSWORTH)

      verifyServiceAndRepositoryCalls(amendedCourtBookingDetails)

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
      with(emailCaptor.lastValue) {
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

      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.lastValue) {
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

      facade.sendEmails(amendedCourtBookingDetails, PRISON_USER_WANDSWORTH)

      inOrder(emailService, videoBookingRepository, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(videoBookingRepository).findById(amendedCourtBookingDetails.videoLinkBookingId)
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      with(emailCaptor.singleValue) {
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

      with(notificationCaptor.singleValue) {
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

      facade.sendEmails(amendedProbationBookingDetails, PROBATION_USER)

      verifyServiceAndRepositoryCalls(amendedProbationBookingDetails)

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
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }
      with(emailCaptor.lastValue) {
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
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.lastValue) {
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

      facade.sendEmails(amendedProbationBookingDetails, PRISON_USER_BIRMINGHAM)

      verifyServiceAndRepositoryCalls(amendedProbationBookingDetails)

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
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }
      with(emailCaptor.lastValue) {
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
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.lastValue) {
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

      facade.sendEmails(amendedProbationBookingDetails, PRISON_USER_BIRMINGHAM)

      inOrder(emailService, videoBookingRepository, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(videoBookingRepository).findById(amendedProbationBookingDetails.videoLinkBookingId)
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      with(emailCaptor.singleValue) {
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
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.singleValue) {
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
    private val cancelledCourtBookingDetails = BookingDetails.transferred(
      courtBooking.toModel(setOf(wandsworthLocation.toModel()))
        .copy(videoLinkBookingId = 1L, statusCode = BookingStatus.CANCELLED),
      prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH),
    )
    private val cancelledCourtByPrisonBookingDetails = BookingDetails.transferred(
      courtBookingCreatedByPrison.toModel(setOf(wandsworthLocation.toModel()))
        .copy(videoLinkBookingId = 2L, statusCode = BookingStatus.CANCELLED),
      prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH),
    )
    private val cancelledProbationBookingDetails = BookingDetails.transferred(
      probationBookingAtBirminghamPrison.toModel(
        setOf(birminghamLocation.toModel(locationAttributes().copy(prisonVideoUrl = "decorated-video-link-url"))),
      ).copy(
        videoLinkBookingId = 3L,
        additionalBookingDetails = AdditionalBookingDetails(
          contactName = "probation officer name",
          contactEmail = "probation.officer@email.com",
          contactNumber = "0114 2345678",
        ),
        statusCode = BookingStatus.CANCELLED,
      ),
      prisoner(prisonerNumber = probationBookingAtBirminghamPrison.prisoner(), prisonCode = BIRMINGHAM),
    )

    @BeforeEach
    fun before() {
      whenever(videoBookingRepository.findById(cancelledCourtBookingDetails.videoLinkBookingId)) doReturn Optional.of(courtBooking)
      whenever(videoBookingRepository.findById(cancelledCourtByPrisonBookingDetails.videoLinkBookingId)) doReturn Optional.of(courtBookingCreatedByPrison)
      whenever(videoBookingRepository.findById(cancelledProbationBookingDetails.videoLinkBookingId)) doReturn Optional.of(probationBookingAtBirminghamPrison)
    }

    @Test
    fun `should send events and emails on transfer of prisoner by service user for a court booking`() {
      whenever(contactsService.getCourtBookingContacts(any(), any(), anyList(), anyOrNull())) doReturn listOf(
        bookingContact(contactType = ContactType.PRISON, email = "jon@prison.com", name = "Jon"),
      )

      whenever(emailService.send(any<TransferredCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(cancelledCourtBookingDetails, SERVICE_USER)

      inOrder(emailService, videoBookingRepository, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(videoBookingRepository).findById(cancelledCourtBookingDetails.videoLinkBookingId)
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      with(emailCaptor.singleValue) {
        this isInstanceOf TransferredCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo "jon@prison.com"
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "dateOfBirth" to "1 Jan 2000",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
        )
      }

      with(notificationCaptor.singleValue) {
        email isEqualTo "jon@prison.com"
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "TRANSFERRED"
      }
    }

    @Test
    fun `should send events and emails on transfer of prisoner by service user for a probation booking`() {
      setupProbationPrimaryContacts(SERVICE_USER)
      whenever(emailService.send(any<TransferredProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(emailService.send(any<TransferredProbationBookingPrisonProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(cancelledProbationBookingDetails, SERVICE_USER)

      verifyServiceAndRepositoryCalls(cancelledProbationBookingDetails)

      with(emailCaptor.firstValue) {
        this isInstanceOf TransferredProbationBookingPrisonProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "probationEmailAddress" to "probation.user@probation.com",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "dateOfBirth" to "1 Jan 2000",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }
      with(emailCaptor.lastValue) {
        this isInstanceOf TransferredProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "dateOfBirth" to "1 Jan 2000",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "TRANSFERRED"
      }
      with(notificationCaptor.lastValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "TRANSFERRED"
      }
    }
  }

  @Nested
  @DisplayName("Prisoner release")
  inner class PrisonerRelease {
    private val cancelledCourtBookingDetails = BookingDetails.released(
      courtBooking.toModel(setOf(wandsworthLocation.toModel()))
        .copy(videoLinkBookingId = 1L, statusCode = BookingStatus.CANCELLED),
      prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH),
    )
    private val cancelledCourtByPrisonBookingDetails = BookingDetails.released(
      courtBookingCreatedByPrison.toModel(setOf(wandsworthLocation.toModel()))
        .copy(videoLinkBookingId = 2L, statusCode = BookingStatus.CANCELLED),
      prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH),
    )
    private val cancelledProbationBookingDetails = BookingDetails.released(
      probationBookingAtBirminghamPrison.toModel(
        setOf(birminghamLocation.toModel(locationAttributes().copy(prisonVideoUrl = "decorated-video-link-url"))),
      ).copy(
        videoLinkBookingId = 3L,
        additionalBookingDetails = AdditionalBookingDetails(
          contactName = "probation officer name",
          contactEmail = "probation.officer@email.com",
          contactNumber = "0114 2345678",
        ),
        statusCode = BookingStatus.CANCELLED,
      ),
      prisoner(prisonerNumber = probationBookingAtBirminghamPrison.prisoner(), prisonCode = BIRMINGHAM),
    )

    @BeforeEach
    fun before() {
      whenever(videoBookingRepository.findById(cancelledCourtBookingDetails.videoLinkBookingId)) doReturn Optional.of(courtBooking)
      whenever(videoBookingRepository.findById(cancelledCourtByPrisonBookingDetails.videoLinkBookingId)) doReturn Optional.of(courtBookingCreatedByPrison)
      whenever(videoBookingRepository.findById(cancelledProbationBookingDetails.videoLinkBookingId)) doReturn Optional.of(probationBookingAtBirminghamPrison)
    }

    @Test
    fun `should send events and emails on release of prisoner by service user for a court booking`() {
      setupCourtPrimaryContactsFor(SERVICE_USER)
      whenever(emailService.send(any<ReleasedCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")
      whenever(emailService.send(any<ReleasedCourtBookingPrisonCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(cancelledCourtBookingDetails, SERVICE_USER)

      verifyServiceAndRepositoryCalls(cancelledCourtBookingDetails)

      with(emailCaptor.firstValue) {
        this isInstanceOf ReleasedCourtBookingPrisonCourtEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "dateOfBirth" to "1 Jan 2000",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
        )
      }

      with(emailCaptor.lastValue) {
        this isInstanceOf ReleasedCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "dateOfBirth" to "1 Jan 2000",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }
      with(notificationCaptor.lastValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }
    }

    @Test
    fun `should send events and emails on release of prisoner by service user for a probation booking`() {
      setupProbationPrimaryContacts(SERVICE_USER)
      whenever(emailService.send(any<ReleasedProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(emailService.send(any<ReleasedProbationBookingPrisonProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.sendEmails(cancelledProbationBookingDetails, SERVICE_USER)

      verifyServiceAndRepositoryCalls(cancelledProbationBookingDetails)

      with(emailCaptor.firstValue) {
        this isInstanceOf ReleasedProbationBookingPrisonProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "probationEmailAddress" to "probation.user@probation.com",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "dateOfBirth" to "1 Jan 2000",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }
      with(emailCaptor.lastValue) {
        this isInstanceOf ReleasedProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "dateOfBirth" to "1 Jan 2000",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "decorated-video-link-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }
      with(notificationCaptor.lastValue) {
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
    private val courtBookingDetails = BookingDetails.courtHearingLinkReminder(
      courtBooking.toModel(setOf(wandsworthLocation.toModel()))
        .copy(videoLinkBookingId = 1L, videoLinkUrl = null),
      prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH),
    )

    @BeforeEach
    fun before() {
      whenever(videoBookingRepository.findById(courtBookingDetails.videoLinkBookingId)) doReturn Optional.of(courtBooking)
    }

    @Test
    fun `should send an email to the court contact to remind them to add a court hearing link`() {
      setupCourtPrimaryContactsFor(SERVICE_USER)
      whenever(emailService.send(any<CourtHearingLinkReminderEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.sendEmails(courtBookingDetails, SERVICE_USER)

      inOrder(emailService, videoBookingRepository, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(videoBookingRepository).findById(courtBookingDetails.videoLinkBookingId)
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      with(emailCaptor.singleValue) {
        this isInstanceOf CourtHearingLinkReminderEmail::class.java
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
          "courtHearingLink" to "Not yet known",
          "bookingId" to "1",
        )
      }

      with(notificationCaptor.singleValue) {
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
    private val probationBookingDetails = BookingDetails.probationOfficerEmailReminder(
      probationBookingAtBirminghamPrison.toModel(
        setOf(birminghamLocation.toModel(locationAttributes().copy(prisonVideoUrl = "decorated-video-link-url"))),
      ).copy(videoLinkBookingId = 3L, statusCode = BookingStatus.CANCELLED),
      prisoner(prisonerNumber = probationBookingAtBirminghamPrison.prisoner(), prisonCode = BIRMINGHAM),
    )

    @BeforeEach
    fun before() {
      whenever(videoBookingRepository.findById(probationBookingDetails.videoLinkBookingId)) doReturn Optional.of(probationBookingAtBirminghamPrison)
    }

    @Test
    fun `should send an email to the probation contact to remind them to add missing probation officer details`() {
      setupProbationPrimaryContacts(SERVICE_USER)
      whenever(emailService.send(any<ProbationOfficerDetailsReminderEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.sendEmails(probationBookingDetails, SERVICE_USER)

      inOrder(emailService, videoBookingRepository, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(videoBookingRepository).findById(probationBookingDetails.videoLinkBookingId)
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      with(emailCaptor.singleValue) {
        this isInstanceOf ProbationOfficerDetailsReminderEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "probationTeam" to "probation team description",
          "meetingType" to "Pre-sentence report (PSR)",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "bookingId" to "3",
          "prisonVideoUrl" to "decorated-video-link-url",
        )
      }

      with(notificationCaptor.singleValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "PROBATION_OFFICER_DETAILS_REMINDER"
      }
    }
  }

  private fun verifyServiceAndRepositoryCalls(bookingDetails: BookingDetails) {
    inOrder(emailService, videoBookingRepository, notificationRepository) {
      verify(emailService).send(emailCaptor.capture())
      verify(videoBookingRepository).findById(bookingDetails.videoLinkBookingId)
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(videoBookingRepository).findById(bookingDetails.videoLinkBookingId)
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }
  }

  private fun setupCourtPrimaryContactsFor(user: User) {
    val mayBeEmail = when (user) {
      is ExternalUser -> user.email
      is PrisonUser -> user.email
      else -> null
    }

    // Not ideal but have logic in tests to mimic stubbed service behaviour regarding matching email addresses for contacts
    contactsService.stub {
      on { getCourtBookingContacts(any(), any(), anyList(), eq(user)) } doReturn listOfNotNull(
        bookingContact(contactType = ContactType.USER, email = mayBeEmail, name = user.name).takeUnless { user is ServiceUser },
        bookingContact(contactType = ContactType.PRISON, email = PRISON_USER_BIRMINGHAM.email, name = PRISON_USER_BIRMINGHAM.name).takeUnless { it.email == mayBeEmail },
        bookingContact(contactType = ContactType.COURT, email = COURT_USER.email, name = COURT_USER.name).takeUnless { it.email == mayBeEmail },
        bookingContact(contactType = ContactType.PROBATION, email = PROBATION_USER.email, name = PROBATION_USER.name).takeUnless { it.email == mayBeEmail },
      )
    }
  }

  private fun setupProbationPrimaryContacts(user: User) {
    val mayBeEmail = when (user) {
      is ExternalUser -> user.email
      is PrisonUser -> user.email
      is DeliusUser -> user.email
      else -> null
    }

    // Not ideal but have logic in tests to mimic stubbed service behaviour regarding matching email addresses for contacts
    contactsService.stub {
      on { getProbationBookingContacts(any(), any(), any(), eq(user)) } doReturn listOfNotNull(
        bookingContact(contactType = ContactType.USER, email = mayBeEmail, name = user.name).takeUnless { user is ServiceUser },
        bookingContact(contactType = ContactType.PRISON, email = PRISON_USER_BIRMINGHAM.email, name = PRISON_USER_BIRMINGHAM.name).takeUnless { it.email == mayBeEmail },
        bookingContact(contactType = ContactType.PROBATION, email = PROBATION_USER.email, name = PROBATION_USER.name).takeUnless { user is DeliusUser || it.email == mayBeEmail },
      )
    }
  }
}
