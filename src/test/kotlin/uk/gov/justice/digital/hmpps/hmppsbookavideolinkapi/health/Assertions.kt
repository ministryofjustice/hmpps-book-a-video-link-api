package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.health

import org.assertj.core.api.Assertions.assertThat

internal infix fun <T> T.isEqualTo(value: T) {
  assertThat(this).isEqualTo(value)
}
