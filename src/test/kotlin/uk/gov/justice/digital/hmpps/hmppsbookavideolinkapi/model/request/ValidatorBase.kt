package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat

internal typealias ModelError = Pair<String, String>

abstract class ValidatorBase<MODEL> {

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  internal fun assertNoErrors(model: MODEL) {
    assertThat(validator.validate(model)).isEmpty()
  }

  internal infix fun MODEL.failsWithSingle(value: ModelError) {
    with(validator.validate(this)) {
      assertThat(size).isEqualTo(1)
      assertThat(first().propertyPath.toString()).isEqualTo(value.first)
      assertThat(first().message).isEqualTo(value.second)
    }
  }
}
