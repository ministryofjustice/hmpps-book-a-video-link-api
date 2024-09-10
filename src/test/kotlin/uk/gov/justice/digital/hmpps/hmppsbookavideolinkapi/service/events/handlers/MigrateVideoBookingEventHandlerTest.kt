package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.whereaboutsapi.LegacyBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.whereaboutsapi.WhereaboutsApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration.MigrateVideoBookingService

class MigrateVideoBookingEventHandlerTest {
  private val legacyBooking = LegacyBooking("legacy booking")
  private val whereaboutsApiClient: WhereaboutsApiClient = mock()
  private val migrationService: MigrateVideoBookingService = mock()
  private val telemetryService: TelemetryService = mock()
  private val handler = MigrateVideoBookingEventHandler(whereaboutsApiClient, migrationService, telemetryService)

  @Test
  fun `should capture telemetry information for successful migration`() {
    whenever(whereaboutsApiClient.findBookingDetails(1)) doReturn legacyBooking

    handler.handle(MigrateVideoBookingEvent(1))

    verify(migrationService).migrate(legacyBooking)
    verify(telemetryService).capture("Succeeded migration of booking 1 from whereabouts-api")
  }

  @Test
  fun `should capture telemetry information for failed migration when booking not found`() {
    whenever(whereaboutsApiClient.findBookingDetails(1)) doReturn null

    assertThrows<NullPointerException> { handler.handle(MigrateVideoBookingEvent(1)) }.message isEqualTo "Video booking 1 not found in whereabouts-api"

    verify(migrationService, never()).migrate(anyOrNull())
    verify(telemetryService).capture("Failed migration of booking 1 from whereabouts-api")
  }

  @Test
  fun `should capture telemetry information for failed migration when migration fails`() {
    whenever(whereaboutsApiClient.findBookingDetails(1)) doReturn legacyBooking
    whenever(migrationService.migrate(anyOrNull())) doThrow RuntimeException("failed to migrate booking")

    assertThrows<RuntimeException> { handler.handle(MigrateVideoBookingEvent(1)) }.message isEqualTo "failed to migrate booking"

    verify(migrationService).migrate(legacyBooking)
    verify(telemetryService).capture("Failed migration of booking 1 from whereabouts-api")
  }
}
