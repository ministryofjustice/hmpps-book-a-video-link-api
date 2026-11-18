package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.attributesearch

import com.fasterxml.jackson.annotation.JsonValue

enum class Attribute(@get:JsonValue val value: String) {
  FIRST_NAME("firstName"),
  LAST_NAME("lastName"),
  PRISONER_NUMBER("prisonerNumber"),
}
