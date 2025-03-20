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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TestEmailConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
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

  @MockitoBean
  private lateinit var prisonApiClient: PrisonApiClient

  @MockitoBean
  private lateinit var nomisMappingClient: NomisMappingClient

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Test
  @Sql("classpath:test_data/clean-all-data.sql", "classpath:integration-test-data/seed-cancelled-booking-for-tomorrow.sql")
  fun `should publish and process a VIDEO_BOOKING_CANCELLED domain event`() {
    activitiesAppointmentsClient.stub { on { isAppointmentsRolledOutAt(BIRMINGHAM) } doReturn false }

    nomisMappingClient.stub {
      on { getNomisLocationMappingBy(UUID.fromString("ba0df03b-7864-47d5-9729-0301b74ecbe2")) } doReturn NomisDpsLocationMapping(
        dpsLocationId = UUID.fromString("ba0df03b-7864-47d5-9729-0301b74ecbe2"),
        nomisLocationId = 99,
      )
    }

    prisonApiClient.stub {
      on { getPrisonersAppointmentsAtLocations(BIRMINGHAM, "78910", tomorrow(), 99) } doReturn listOf(
        PrisonerSchedule(
          offenderNo = "78910",
          locationId = 99,
          firstName = "Bob",
          lastName = "Builder",
          eventId = 99,
          event = "VLB",
          startTime = tomorrow().atTime(LocalTime.of(9, 0)),
          endTime = tomorrow().atTime(LocalTime.of(10, 0)),
        ),
      )
    }

    webTestClient.publishDomainEvent(PublishEventUtilityModel(event = Event.VIDEO_BOOKING_CANCELLED, identifiers = setOf(4000L)))
      .also { it isEqualTo "UTILITY: published domain event VIDEO_BOOKING_CANCELLED" }

    waitUntil {
      verify(prisonApiClient).cancelAppointment(99)
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
