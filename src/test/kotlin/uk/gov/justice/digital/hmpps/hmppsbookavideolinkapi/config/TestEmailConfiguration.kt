package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.util.UUID

@TestConfiguration
class TestEmailConfiguration {
  @Bean
  fun emailService() = EmailService { email -> Result.success(UUID.randomUUID() to email.javaClass.simpleName) }
}
