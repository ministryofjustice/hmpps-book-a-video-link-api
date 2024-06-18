package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Component
class OutboundEventsPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val mapper: ObjectMapper,
  features: FeatureSwitches,
) {
  private val isSnsEnabled = features.isEnabled(Feature.SNS_ENABLED)

  companion object {
    const val TOPIC_ID = "domainevents"

    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("PUBLISHER: SNS enabled = $isSnsEnabled")
  }

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId(TOPIC_ID) ?: throw RuntimeException("Topic with name $TOPIC_ID doesn't exist")
  }

  fun send(event: DomainEvent<*>) {
    if (isSnsEnabled) {
      log.info("PUBLISHER: publishing domain event $event")

      runCatching {
        domainEventsTopic.snsClient.publish(
          PublishRequest.builder()
            .topicArn(domainEventsTopic.arn)
            .message(mapper.writeValueAsString(event))
            .messageAttributes(event.attributes())
            .build(),
        ).get()
      }.onFailure { log.error("PUBLISHER: error publishing event $event", it) }
    } else {
      log.info("PUBLISHER: domain event $event not published, publishing is disabled ")
    }
  }

  private fun DomainEvent<*>.attributes() =
    mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build())
}
