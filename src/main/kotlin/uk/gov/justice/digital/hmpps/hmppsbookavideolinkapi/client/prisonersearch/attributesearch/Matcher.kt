package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.attributesearch

data class Matcher(
  val type: MatcherType,
  val attribute: Attribute,
  val condition: Condition,
  val searchTerm: String,
)
