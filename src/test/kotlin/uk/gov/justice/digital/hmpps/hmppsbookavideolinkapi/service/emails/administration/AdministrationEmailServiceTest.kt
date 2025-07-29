package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.administration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.util.UUID

class AdministrationEmailServiceTest {
  private val prisonsService: PrisonsService = mock()
  private val locationsService: LocationsService = mock()
  private val emailService: EmailService = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val emailCaptor = argumentCaptor<Email>()
  private val govNotifyNotificationId = UUID.randomUUID()
  private val notificationCaptor = argumentCaptor<Notification>()

  @BeforeEach
  fun beforeEach() {
    whenever(prisonsService.getListOfPrisons(true)) doReturn listOf(pentonvillePrison.toModel())
    whenever(
      locationsService.getVideoLinkLocationsAtPrison(
        PENTONVILLE,
        true,
      ),
    ) doReturn listOf(pentonvilleLocation.toModel())
    whenever(emailService.send(any())) doReturn Result.success(govNotifyNotificationId to "template_id")
  }

  @Test
  fun `should send one email for Pentonville prison location`() {
    service(" EMAIL@domain.com").sendEmailsForNewPrisonVideoRoom()

    verify(prisonsService).getListOfPrisons(true)
    verify(locationsService).getVideoLinkLocationsAtPrison(PENTONVILLE, true)
    verify(emailService).send(emailCaptor.capture())
    verify(notificationRepository).saveAndFlush(notificationCaptor.capture())

    with(emailCaptor.allValues.single()) {
      this isInstanceOf AdministrationNewVideoRoomEmail::class.java
      address isEqualTo "email@domain.com"
      personalisation() isEqualTo mapOf("prisonsAndRooms" to "Pentonville (HMP & YOI)\nPentonville room 3\n\n")
    }

    with(notificationCaptor.allValues.single()) {
      email isEqualTo "email@domain.com"
      templateName isEqualTo "template_id"
      this.govNotifyNotificationId isEqualTo govNotifyNotificationId
      reason isEqualTo "NEW_PRISON_VIDEO_ROOM"
    }
  }

  @Test
  fun `should send one email for multiple prisons`() {
    whenever(prisonsService.getListOfPrisons(true)) doReturn listOf(pentonvillePrison.toModel(), wandsworthPrison.toModel())
    whenever(
      locationsService.getVideoLinkLocationsAtPrison(
        WANDSWORTH,
        true,
      ),
    ) doReturn listOf(wandsworthLocation.toModel(), wandsworthLocation2.toModel())

    service(" EMAIL@domain.com").sendEmailsForNewPrisonVideoRoom()

    verify(prisonsService).getListOfPrisons(true)
    verify(locationsService).getVideoLinkLocationsAtPrison(PENTONVILLE, true)
    verify(emailService).send(emailCaptor.capture())
    verify(notificationRepository).saveAndFlush(notificationCaptor.capture())

    with(emailCaptor.allValues.single()) {
      this isInstanceOf AdministrationNewVideoRoomEmail::class.java
      address isEqualTo "email@domain.com"
      personalisation() isEqualTo mapOf("prisonsAndRooms" to "Pentonville (HMP & YOI)\nPentonville room 3\n\nWandsworth\nWandsworth room\nWandsworth room 2\n\n")
    }

    with(notificationCaptor.allValues.single()) {
      email isEqualTo "email@domain.com"
      templateName isEqualTo "template_id"
      this.govNotifyNotificationId isEqualTo govNotifyNotificationId
      reason isEqualTo "NEW_PRISON_VIDEO_ROOM"
    }
  }

  @Test
  fun `should only send one email for duplicate emails for Pentonville prison location`() {
    service("email@domain.com", " emaiL@Domain.com ").sendEmailsForNewPrisonVideoRoom()

    verify(prisonsService).getListOfPrisons(true)
    verify(locationsService).getVideoLinkLocationsAtPrison(PENTONVILLE, true)
    verify(emailService).send(emailCaptor.capture())

    with(emailCaptor.allValues.single()) {
      this isInstanceOf AdministrationNewVideoRoomEmail::class.java
      address isEqualTo "email@domain.com"
      personalisation() isEqualTo mapOf("prisonsAndRooms" to "Pentonville (HMP & YOI)\nPentonville room 3\n\n")
    }
  }

  @Test
  fun `should send two emails for Pentonville prison location`() {
    service("email1@domain.com", "email2@domain.com").sendEmailsForNewPrisonVideoRoom()

    verify(prisonsService).getListOfPrisons(true)
    verify(locationsService).getVideoLinkLocationsAtPrison(PENTONVILLE, true)
    verify(emailService, times(2)).send(emailCaptor.capture())
    verify(notificationRepository, times(2)).saveAndFlush(notificationCaptor.capture())

    with(emailCaptor.allValues.first()) {
      this isInstanceOf AdministrationNewVideoRoomEmail::class.java
      address isEqualTo "email1@domain.com"
      personalisation() isEqualTo mapOf("prisonsAndRooms" to "Pentonville (HMP & YOI)\nPentonville room 3\n\n")
    }

    with(emailCaptor.allValues.last()) {
      this isInstanceOf AdministrationNewVideoRoomEmail::class.java
      address isEqualTo "email2@domain.com"
      personalisation() isEqualTo mapOf("prisonsAndRooms" to "Pentonville (HMP & YOI)\nPentonville room 3\n\n")
    }

    with(notificationCaptor.firstValue) {
      email isEqualTo "email1@domain.com"
    }

    with(notificationCaptor.lastValue) {
      email isEqualTo "email2@domain.com"
    }
  }

  @Test
  fun `should send no emails when no admin email address supplied`() {
    service().sendEmailsForNewPrisonVideoRoom()
    service("").sendEmailsForNewPrisonVideoRoom()
    service(" ").sendEmailsForNewPrisonVideoRoom()

    verifyNoInteractions(prisonsService)
    verifyNoInteractions(locationsService)
    verifyNoInteractions(emailService)
    verifyNoInteractions(notificationRepository)
  }

  @Test
  fun `should send no emails when no new rooms`() {
    whenever(locationsService.getVideoLinkLocationsAtPrison(PENTONVILLE, true)) doReturn emptyList()

    service("email@domain.com").sendEmailsForNewPrisonVideoRoom()

    verify(prisonsService).getListOfPrisons(true)
    verify(locationsService).getVideoLinkLocationsAtPrison(PENTONVILLE, true)
    verifyNoInteractions(emailService)
    verifyNoInteractions(notificationRepository)
  }

  private fun service(vararg emails: String) = AdministrationEmailService(
    prisonsService,
    locationsService,
    emailService,
    notificationRepository,
    emails.joinToString(","),
  )
}
