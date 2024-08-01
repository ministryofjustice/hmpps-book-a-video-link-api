package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

fun requireNot(value: Boolean, lazyMessage: () -> Any) = require(!value, lazyMessage)
