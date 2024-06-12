package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.Event

/**
 * Event handler implementations should not swallow exceptions. If an exception is thrown it should be propagated.
 *
 * By not catching errors events will (and should) be retried automatically.
 */
fun interface EventHandler<T : Event> {
  fun handle(event: T)
}
