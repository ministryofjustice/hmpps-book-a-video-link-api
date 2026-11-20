package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TestEmailConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Event
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PublishEventUtilityModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import java.time.LocalTime
import java.util.UUID

@ContextConfiguration(classes = [TestEmailConfiguration::class])
class UtilityResourceIntegrationTest : SqsIntegrationTestBase() {

  @MockitoBean
  private lateinit var activitiesAppointmentsClient: ActivitiesAppointmentsClient

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Test
  @Sql("classpath:test_data/clean-all-data.sql", "classpath:integration-test-data/seed-cancelled-booking-for-tomorrow.sql")
  fun `should publish and process a VIDEO_BOOKING_CANCELLED domain event`() {
    activitiesAppointmentsClient.stub {
      on { getPrisonersAppointments(BIRMINGHAM, "78910", tomorrow(), UUID.fromString("ba0df03b-7864-47d5-9729-0301b74ecbe2")) } doReturn listOf(
        appointmentSearchResult(
          Prisoner("78910", BIRMINGHAM),
          location = location(
            prisonCode = BIRMINGHAM,
            locationKeySuffix = "PVI-78910",
            id = UUID.fromString("ba0df03b-7864-47d5-9729-0301b74ecbe2"),
          ),
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(10, 0),
          isCancelled = false,
          isDeleted = false,
          appointmentType = "VLB",
        ),
      )
    }

    webTestClient.publishDomainEvent(PublishEventUtilityModel(event = Event.VIDEO_BOOKING_CANCELLED, identifiers = setOf(4000L)))
      .also { it isEqualTo "UTILITY: published domain event VIDEO_BOOKING_CANCELLED" }

    waitUntil {
      verify(activitiesAppointmentsClient).cancelAppointment(99)
    }

    notificationRepository.findAll() hasSize 0
  }

  private fun WebTestClient.publishDomainEvent(request: PublishEventUtilityModel) = this
    .post()
    .uri("/utility/publish")
    .bodyValue(request)
    .accept(MediaType.TEXT_PLAIN)
    .exchange()
    .expectStatus().isOk
    .expectBody(String::class.java)
    .returnResult().responseBody!!
}
