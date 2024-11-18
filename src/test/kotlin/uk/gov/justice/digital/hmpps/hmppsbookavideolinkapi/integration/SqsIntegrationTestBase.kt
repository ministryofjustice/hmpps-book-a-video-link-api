package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.EventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.Message
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.MessageAttributes
import java.util.UUID
import java.util.concurrent.TimeUnit

@ActiveProfiles("test-localstack", inheritProfiles = false)
abstract class SqsIntegrationTestBase : IntegrationTestBase() {

  @Autowired
  protected lateinit var mapper: ObjectMapper

  protected fun raw(event: DomainEvent<*>): String =
    mapper.writeValueAsString(
      Message(
        "Notification",
        mapper.writeValueAsString(event),
        UUID.randomUUID().toString(),
        MessageAttributes(EventType(Type = "String", Value = event.eventType)),
      ),
    )

  protected fun waitUntil(fn: () -> Unit) {
    // Default wait is 10 seconds, so dropping down to 5.
    await.atMost(5, TimeUnit.SECONDS) untilAsserted {
      fn()
    }
  }

  companion object {
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}
