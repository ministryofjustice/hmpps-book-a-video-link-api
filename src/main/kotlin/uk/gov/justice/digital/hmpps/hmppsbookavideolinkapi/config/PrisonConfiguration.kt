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
  fun startOfDay(prisonCode: String): LocalTime

  fun endOfDay(prisonCode: String): LocalTime
}
