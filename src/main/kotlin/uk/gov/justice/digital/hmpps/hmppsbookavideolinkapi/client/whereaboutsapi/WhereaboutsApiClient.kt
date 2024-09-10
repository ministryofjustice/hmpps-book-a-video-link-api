package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.whereaboutsapi

import org.springframework.stereotype.Component

@Component
class WhereaboutsApiClient {
  fun findBookingDetails(videoBookingId: Long): LegacyBooking? = TODO()
}

// TODO temporary placeholder type until we have something more concrete
@Deprecated(message = "temporary placeholder type until we have something more concrete")
data class LegacyBooking(val x: String)
