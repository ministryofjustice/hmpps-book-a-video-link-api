package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.attributesearch

import com.fasterxml.jackson.annotation.JsonValue

enum class Condition(@get:JsonValue val value: String) {
  CONTAINS("CONTAINS"),
  IN("IN"),
}
