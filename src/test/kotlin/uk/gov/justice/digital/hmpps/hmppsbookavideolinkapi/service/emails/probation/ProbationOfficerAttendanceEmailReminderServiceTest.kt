package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AdditionalBookingDetail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.additionalDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrisonDisabled
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AdditionalBookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.VideoLinkBookingsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDateTime
import java.util.UUID

class ProbationOfficerAttendanceEmailReminderServiceTest {
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository = mock()
  private val videoLinkBookingsService: VideoLinkBookingsService = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val emailService: EmailService = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val service = ProbationOfficerAttendanceEmailReminderService(
    additionalBookingDetailRepository,
    videoLinkBookingsService,
    prisonerSearchClient,
    emailService,
    notificationRepository,
  )
  private val probationBooking = probationBooking().withProbationPrisonAppointment(prison = wandsworthPrison, prisoner = wandsworthPrisoner, location = wandsworthLocation)
  private val probationBookingModel = probationBooking
    .toModel(
      locations = setOf(wandsworthLocation.toModel()),
      additionalBookingDetails = AdditionalBookingDetails(
        "fred",
        "fred@justice.gov.uk",
        contactNumber = "0114 3456345",
      ),
    )
  private val detailsWithoutJusticeEmail: AdditionalBookingDetail = additionalDetails(probationBooking, contactName = "Bob", contactEmail = "not.justice@email.com")
  private val detailsWithJusticeEmail: AdditionalBookingDetail = additionalDetails(probationBooking, contactName = "Fred", contactEmail = "frED@justiCe.gov.uk")
  private val probationBookingDisabledPrison = probationBooking().withProbationPrisonAppointment(prison = wandsworthPrisonDisabled, prisoner = wandsworthPrisoner, location = wandsworthLocation)
  private val detailsWithJusticeEmailDisabledPrison: AdditionalBookingDetail = additionalDetails(probationBookingDisabledPrison, contactEmail = "frED@justiCe.gov.uk")

  @Nested
  inner class NoEmailSent {
    @Test
    fun `should be no-op when no probation officer contacts`() {
      whenever { additionalBookingDetailRepository.findAdditionalDetailsForAllActiveProbationMeetingsOnDate(today()) } doReturn emptyList()

      service.sendProbationOfficerAttendanceReminders(today())

      verifyNoInteractions(videoLinkBookingsService, prisonerSearchClient, emailService, notificationRepository)
    }

    @Test
    fun `should be no-op when no probation officer contacts without justice emails`() {
      whenever { additionalBookingDetailRepository.findAdditionalDetailsForAllActiveProbationMeetingsOnDate(today()) } doReturn listOf(detailsWithoutJusticeEmail)

      service.sendProbationOfficerAttendanceReminders(today())

      verifyNoInteractions(videoLinkBookingsService, prisonerSearchClient, emailService, notificationRepository)
    }

    @Test
    fun `should be no-op when prison is not a self service prison`() {
      whenever { additionalBookingDetailRepository.findAdditionalDetailsForAllActiveProbationMeetingsOnDate(today()) } doReturn listOf(detailsWithJusticeEmailDisabledPrison)

      service.sendProbationOfficerAttendanceReminders(today())

      verifyNoInteractions(videoLinkBookingsService, prisonerSearchClient, emailService, notificationRepository)
    }
  }

  @Nested
  inner class EmailSent {
    private val emailCaptor = argumentCaptor<Email>()
    private val notificationCaptor = argumentCaptor<Notification>()
    private val govNotifyId = UUID.randomUUID()

    @BeforeEach
    fun before() {
      whenever { emailService.send(any()) } doReturn Result.success(govNotifyId to "template Id")
      whenever { prisonerSearchClient.findByPrisonerNumbers(setOf(wandsworthPrisoner.number), 1000) } doReturn listOf(prisonerSearchPrisoner(prisonCode = wandsworthPrisoner.prison, prisonerNumber = wandsworthPrisoner.number))
    }

    @Test
    fun `should only send emails for probation officer contacts with justice emails`() {
      whenever { additionalBookingDetailRepository.findAdditionalDetailsForAllActiveProbationMeetingsOnDate(today()) } doReturn listOf(detailsWithoutJusticeEmail, detailsWithJusticeEmail)
      whenever { videoLinkBookingsService.getVideoLinkBookingById(probationBooking().videoBookingId, UserService.getServiceAsUser()) } doReturn probationBookingModel

      service.sendProbationOfficerAttendanceReminders(today())

      inOrder(additionalBookingDetailRepository, prisonerSearchClient, videoLinkBookingsService, emailService, notificationRepository) {
        verify(additionalBookingDetailRepository).findAdditionalDetailsForAllActiveProbationMeetingsOnDate(today())
        verify(prisonerSearchClient).findByPrisonerNumbers(setOf(wandsworthPrisoner.number), 1000)
        verify(videoLinkBookingsService).getVideoLinkBookingById(probationBooking().videoBookingId, UserService.getServiceAsUser())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.singleValue isInstanceOf ProbationOfficerAttendanceReminderEmail::class.java

      with(notificationCaptor.singleValue) {
        email isEqualTo "fred@justice.gov.uk"
        govNotifyNotificationId isEqualTo govNotifyId
        createdTime isCloseTo LocalDateTime.now()
        templateName isEqualTo "template Id"
        reason isEqualTo "PROBATION_OFFICER_ATTENDANCE_REMINDER"
      }
    }
  }
}
