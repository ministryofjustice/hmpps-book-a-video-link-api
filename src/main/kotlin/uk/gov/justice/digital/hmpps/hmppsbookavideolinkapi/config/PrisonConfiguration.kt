package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalTime

@Configuration
class PrisonRegimeConfiguration {
  @Bean
  fun prisonRegime(): PrisonRegime = object : PrisonRegime {
    override fun startOfDay(prisonCode: String): LocalTime = LocalTime.of(8, 0)

    override fun endOfDay(prisonCode: String): LocalTime = LocalTime.of(18, 0)
  }
}

interface PrisonRegime {

  /**
   * Will be on the hour or at 15 minute intervals within the hour
   */
  fun startOfDay(prisonCode: String): LocalTime

  /**
   * Will be on the hour or at 15 minute intervals within the hour
   */
  fun endOfDay(prisonCode: String): LocalTime
}
