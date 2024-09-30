package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.TestEmailConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs.JobType
import java.time.LocalTime

@ContextConfiguration(classes = [TestEmailConfiguration::class])
class JobTriggerIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Test
  fun `should send an email to the court to remind them to add the court hearing link to a booking`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
      videoLinkUrl = null,
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    notificationRepository.deleteAll()

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    webTestClient.triggerJob(JobType.COURT_HEARING_LINK_REMINDER)

    // There should be 1 notification - one email to the court to remind them to add the court hearing link
    val notifications = notificationRepository.findAll().also { it hasSize 1 }

    notifications.isPresent("j@j.com", "court hearing link reminder template id", persistedBooking)
  }

  private fun Collection<Notification>.isPresent(email: String, template: String, booking: VideoBooking? = null) {
    with(single { it.email == email }) {
      templateName isEqualTo template
      videoBooking isEqualTo booking
    }
  }

  private fun WebTestClient.triggerJob(jobType: JobType) =
    this
      .post()
      .uri("/job-admin/run/${jobType.name}")
      .bodyValue({})
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(String::class.java)
      .returnResult().responseBody!!
}
