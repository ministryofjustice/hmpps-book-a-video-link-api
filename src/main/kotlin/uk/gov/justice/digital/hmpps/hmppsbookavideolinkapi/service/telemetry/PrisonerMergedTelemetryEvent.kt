package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

class PrisonerMergedTelemetryEvent(
  private val previousPrisonerNumber: String,
  private val newPrisonerNumber: String,
  private val bookingsUpdated: Long,
) : MetricTelemetryEvent("BVLS-prisoner-merged") {
  override fun properties() = mapOf(
    "previous_prisoner_number" to previousPrisonerNumber,
    "new_prisoner_number" to newPrisonerNumber,
  )

  override fun metrics() = mapOf("bookings_updated" to bookingsUpdated.toDouble())
}
