package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * This configuration is included mainly for the CSV async downloads to limit the number of threads available for async
 * processing and avoid using too many resources on the server.
 */
@Configuration
@EnableAsync
class AsyncConfiguration {
  @Bean
  fun asyncExecutor(): Executor? = ThreadPoolTaskExecutor().apply {
    corePoolSize = 3
    maxPoolSize = 3
    queueCapacity = 50
    threadNamePrefix = "AsyncThread-"
    initialize()
  }
}
