package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

class PrisonerAppointmentsCancelledByPrisonTelemetryEvent(
  private val prisonCode: String,
  private val prisonerNumber: String,
  private val reason: String,
) : StandardTelemetryEvent("BVLS-prisoner-appointments-cancelled-by-prison") {
  override fun properties(): Map<String, String> = mapOf("prisoner_code" to prisonCode, "prisoner_number" to prisonerNumber, "reason" to reason)
}
