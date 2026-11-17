package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.attributesearch

data class Request(
  val joinType: JoinType,
  val queries: List<Query>,
) {
  companion object {
    fun filterPrisonerNumbersByName(
      prisonerNumbers: List<String>,
      firstName: String?,
      lastName: String?,
    ): Request {
      require(prisonerNumbers.isNotEmpty()) {
        "Either first name or last name must be set"
      }

      require(firstName != null || lastName != null) {
        "Either first name or last name must be set"
      }

      return Request(
        joinType = JoinType.AND,
        queries = listOf(
          Query(
            joinType = JoinType.AND,
            matchers = listOfNotNull(
              Matcher(
                type = MatcherType.STRING,
                attribute = Attribute.PRISONER_NUMBER,
                condition = Condition.IN,
                searchTerm = prisonerNumbers.joinToString(separator = ","),
              ),
              firstName?.let {
                Matcher(
                  type = MatcherType.STRING,
                  attribute = Attribute.FIRST_NAME,
                  condition = Condition.CONTAINS,
                  searchTerm = firstName,
                )
              },
              lastName?.let {
                Matcher(
                  type = MatcherType.STRING,
                  attribute = Attribute.LAST_NAME,
                  condition = Condition.CONTAINS,
                  searchTerm = lastName,
                )
              },
            ),
          ),
        ),
      )
    }
  }
}
