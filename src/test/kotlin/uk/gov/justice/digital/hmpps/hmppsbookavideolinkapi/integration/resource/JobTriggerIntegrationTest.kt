package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TestEmailConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.daysFromNow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtHearingLinkReminderEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType
import java.nio.charset.StandardCharsets
import java.time.LocalTime
import kotlin.reflect.KClass

@ContextConfiguration(classes = [TestEmailConfiguration::class])
class JobTriggerIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Test
  fun `should send an email to the court to remind them to add the court hearing link to a booking scheduled for tomorrow`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
      videoLinkUrl = null,
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    notificationRepository.deleteAll()

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    webTestClient.triggerJob(JobType.COURT_HEARING_LINK_REMINDER).also { it isEqualTo "Court hearing link reminders job triggered" }

    // There should be 2 notifications - one email to each enabled court contact to remind them to add the court hearing link
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("j@j.com", CourtHearingLinkReminderEmail::class, persistedBooking)
    notifications.isPresent("b@b.com", CourtHearingLinkReminderEmail::class, persistedBooking)
  }

  @Test
  fun `should not send a court hearing link reminder email to the court for bookings after tomorrow`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      date = 2.daysFromNow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
      videoLinkUrl = null,
    )

    webTestClient.createBooking(courtBookingRequest, COURT_USER)

    notificationRepository.deleteAll()

    webTestClient.triggerJob(JobType.COURT_HEARING_LINK_REMINDER).also { it isEqualTo "Court hearing link reminders job triggered" }

    // There should be 0 notifications
    notificationRepository.findAll().also { it hasSize 0 }
  }

  @Test
  @Sql("classpath:integration-test-data/seed-migrated-bookings.sql")
  fun `should not send an email to the court to remind them to add the court hearing link if the booking has been migrated`() {
    notificationRepository.findAll() hasSize 0

    webTestClient.triggerJob(JobType.COURT_HEARING_LINK_REMINDER).also { it isEqualTo "Court hearing link reminders job triggered" }

    // There should be 0 notifications
    notificationRepository.findAll().also { it hasSize 0 }
  }

  @Test
  fun `should not send an email to the court to remind them to add the court hearing link to a booking at a non-enabled prison`() {
    val prisonUser = PRISON_USER_RISLEY.also(::stubUser)

    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", RISLEY)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = RISLEY,
      location = risleyLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
      videoLinkUrl = null,
    )

    webTestClient.createBooking(courtBookingRequest, prisonUser)

    notificationRepository.deleteAll()

    webTestClient.triggerJob(JobType.COURT_HEARING_LINK_REMINDER).also { it isEqualTo "Court hearing link reminders job triggered" }

    // There should be 0 notifications
    notificationRepository.findAll().also { it hasSize 0 }
  }

  @Test
  @Sql("classpath:integration-test-data/seed-bookings-happening-today.sql")
  fun `should not send a court hearing link reminder email to the court for bookings happening today`() {
    notificationRepository.findAll() hasSize 0

    webTestClient.triggerJob(JobType.COURT_HEARING_LINK_REMINDER).also { it isEqualTo "Court hearing link reminders job triggered" }

    // There should be 0 notifications
    notificationRepository.findAll().also { it hasSize 0 }
  }

  private fun <T : Email> Collection<Notification>.isPresent(email: String, template: KClass<T>, booking: VideoBooking? = null) {
    single { it.email == email && it.templateName == template.simpleName && it.videoBooking == booking }
  }

  private fun WebTestClient.triggerJob(jobType: JobType) =
    this
      .post()
      .uri("/job-admin/run/${jobType.name}")
      .bodyValue({})
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
      .expectBody(String::class.java)
      .returnResult().responseBody!!
}
