package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.EventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.Message
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.MessageAttributes
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.util.*
import java.util.concurrent.TimeUnit

@ActiveProfiles("test-localstack", inheritProfiles = false)
abstract class SqsIntegrationTestBase : IntegrationTestBase() {

  @Autowired
  protected lateinit var mapper: ObjectMapper

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
    await untilCallTo { countAllMessagesOnQueue().also { println("number messages on queue - $it") } } matches { it == numberOfMessages }
  }

  protected fun countAllMessagesOnQueue(): Int = eventsClient.countAllMessagesOnQueue(eventsQueue.queueUrl).get()

  protected fun receiveEvent(message: DomainEvent<*>) {
    eventsClient.sendMessage(SendMessageRequest.builder().queueUrl(eventsQueue.queueUrl).messageBody(raw(message)).build()).get()
  }

  protected fun raw(event: DomainEvent<*>): String =
    mapper.writeValueAsString(
      Message(
        "Notification",
        mapper.writeValueAsString(event),
        UUID.randomUUID().toString(),
        MessageAttributes(EventType(Type = "String", Value = event.eventType)),
      ),
    )
  companion object {
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}
