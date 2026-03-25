package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration

import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsPublisher

@TestConfiguration
class TestConfiguration {

  @Primary
  @Bean
  fun stubOutboundEventsPublisher(): OutboundEventsPublisher = mock()
}
