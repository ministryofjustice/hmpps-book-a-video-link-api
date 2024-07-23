package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

object CsvMapperConfig {
  val csvMapper = CsvMapper().apply {
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  }
}
