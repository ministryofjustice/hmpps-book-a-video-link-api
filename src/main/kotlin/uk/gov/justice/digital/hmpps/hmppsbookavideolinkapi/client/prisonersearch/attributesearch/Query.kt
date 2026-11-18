package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.attributesearch

data class Query(
  val joinType: JoinType,
  val matchers: List<Matcher>,
)
