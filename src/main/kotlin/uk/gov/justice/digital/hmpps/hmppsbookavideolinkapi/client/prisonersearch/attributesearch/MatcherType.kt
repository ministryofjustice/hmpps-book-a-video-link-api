package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.attributesearch

import com.fasterxml.jackson.annotation.JsonValue

enum class MatcherType(@get:JsonValue val value: String) {
  STRING("String"),
}
