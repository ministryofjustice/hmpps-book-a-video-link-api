package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

/**
 * A migration exception cannot be recovered from i.e. there is an unrecoverable reason as to why this has occurred.
 *
 * In the event of a migration exception it will require further independent investigation e.g. looking at logs etc.
 */
class MigrationException(message: String) : RuntimeException(message)
