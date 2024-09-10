package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TelemetryService {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  // TODO temporary placeholder function until we have something more concrete
  @Deprecated(message = "temporary placeholder function until we have something more concrete")
  fun capture(text: String) {
    log.info(text)
  }
}
