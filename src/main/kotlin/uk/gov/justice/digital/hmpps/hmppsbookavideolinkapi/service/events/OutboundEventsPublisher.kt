package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.publish

@Component
class OutboundEventsPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val mapper: ObjectMapper,
) {
  companion object {
    const val TOPIC_ID = "domainevents"

    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId(TOPIC_ID) ?: throw RuntimeException("Topic with name $TOPIC_ID doesn't exist")
  }

  fun send(event: DomainEvent<*>) {
    log.info("PUBLISHER: publishing domain event $event")

    runCatching {
      domainEventsTopic.publish(
        eventType = event.eventType,
        event = mapper.writeValueAsString(event),
        attributes = event.attributes(),
      )
    }.onFailure { log.error("PUBLISHER: error publishing event $event", it) }
  }

  private fun DomainEvent<*>.attributes() =
    mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build())
}
