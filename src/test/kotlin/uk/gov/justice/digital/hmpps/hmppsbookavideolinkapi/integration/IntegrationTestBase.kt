package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ActivitiesAppointmentsApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.LocationsInsidePrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ManageUsersApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonerSearchApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.EventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.Message
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.MessageAttributes
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(
  ActivitiesAppointmentsApiExtension::class,
  HmppsAuthApiExtension::class,
  LocationsInsidePrisonApiExtension::class,
  ManageUsersApiExtension::class,
  PrisonApiExtension::class,
  PrisonerSearchApiExtension::class,
)
@Sql(
  "classpath:test_data/clean-all-data.sql",
)
@TestPropertySource(properties = ["feature.events.sns.enabled=true"])
abstract class IntegrationTestBase {

  private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()).configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  protected val eventsQueue by lazy { hmppsQueueService.findByQueueId("bvls") as HmppsQueue }

  protected val eventsClient by lazy { eventsQueue.sqsClient }

  @BeforeEach
  fun `clear queue`() {
    Awaitility.setDefaultPollDelay(1, TimeUnit.MILLISECONDS)
    Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS)
    eventsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(eventsQueue.queueUrl).build()).get()
    await untilCallTo { countAllMessagesOnQueue() } matches { it == 0 }
    Awaitility.setDefaultPollInterval(50, TimeUnit.MILLISECONDS)
  }

  protected fun waitForMessagesOnQueue(numberOfMessages: Int) {
    await untilCallTo { countAllMessagesOnQueue().also { println("number messages a q $it") } } matches { it == numberOfMessages }
  }

  protected fun countAllMessagesOnQueue(): Int = eventsClient.countAllMessagesOnQueue(eventsQueue.queueUrl).get()

  protected fun receiveEvent(message: DomainEvent<*>) {
    eventsClient.sendMessage(SendMessageRequest.builder().queueUrl(eventsQueue.queueUrl).messageBody(raw(message)).build()).get()
  }

  private fun raw(event: DomainEvent<*>) =
    mapper.writeValueAsString(
      Message(
        "Notification",
        mapper.writeValueAsString(event),
        UUID.randomUUID().toString(),
        MessageAttributes(EventType(Type = "String", Value = event.eventType)),
      ),
    )

  protected fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  protected fun stubPingWithResponse(status: Int) {
    ActivitiesAppointmentsApiExtension.server.stubHealthPing(status)
    HmppsAuthApiExtension.server.stubHealthPing(status)
    locationsInsidePrisonApi().stubHealthPing(status)
    manageUsersApi().stubHealthPing(status)
    prisonerApi().stubHealthPing(status)
    prisonSearchApi().stubHealthPing(status)
  }

  protected fun prisonerApi() = PrisonApiExtension.server
  protected fun prisonSearchApi() = PrisonerSearchApiExtension.server

  protected fun locationsInsidePrisonApi() = LocationsInsidePrisonApiExtension.server

  protected fun manageUsersApi() = ManageUsersApiExtension.server

  companion object {
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}
