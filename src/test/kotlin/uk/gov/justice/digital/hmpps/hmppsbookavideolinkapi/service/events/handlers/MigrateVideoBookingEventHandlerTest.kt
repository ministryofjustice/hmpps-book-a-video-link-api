package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.whereaboutsapi.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.whereaboutsapi.WhereaboutsApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration.MigrateVideoBookingService

class MigrateVideoBookingEventHandlerTest {
  private val videoBookingMigrateResponse = VideoBookingMigrateResponse(
    offenderBookingId = 1,
    prisonCode = MOORLAND,
    courtCode = DERBY_JUSTICE_CENTRE,
    createdBy = "TEST",
    madeByTheCourt = true,
    events = emptyList(),
  )
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val whereaboutsApiClient: WhereaboutsApiClient = mock()
  private val migrationService: MigrateVideoBookingService = mock()
  private val telemetryService: TelemetryService = mock()
  private val handler = MigrateVideoBookingEventHandler(videoBookingRepository, whereaboutsApiClient, migrationService, telemetryService)

  @Test
  fun `should capture telemetry information for successful migration`() {
    whenever(whereaboutsApiClient.findBookingDetails(1)) doReturn videoBookingMigrateResponse

    handler.handle(MigrateVideoBookingEvent(1))

    inOrder(videoBookingRepository, whereaboutsApiClient, migrationService, telemetryService) {
      verify(videoBookingRepository).existsByMigratedVideoBookingId(1)
      verify(whereaboutsApiClient).findBookingDetails(1)
      verify(migrationService).migrate(videoBookingMigrateResponse)
      verify(telemetryService).capture("Succeeded migration of booking 1 from whereabouts-api")
    }
  }

  @Test
  fun `should capture telemetry information for failed migration when booking not found`() {
    whenever(whereaboutsApiClient.findBookingDetails(2)) doReturn null

    assertThrows<NullPointerException> { handler.handle(MigrateVideoBookingEvent(2)) }.message isEqualTo "Video booking 2 not found in whereabouts-api"

    inOrder(videoBookingRepository, whereaboutsApiClient, telemetryService) {
      verify(videoBookingRepository).existsByMigratedVideoBookingId(2)
      verify(whereaboutsApiClient).findBookingDetails(2)
      verify(telemetryService).capture("Failed migration of booking 2 from whereabouts-api")
    }

    verify(migrationService, never()).migrate(anyOrNull())
  }

  @Test
  fun `should capture telemetry information for failed migration when migration fails`() {
    whenever(whereaboutsApiClient.findBookingDetails(3)) doReturn videoBookingMigrateResponse
    whenever(migrationService.migrate(anyOrNull())) doThrow RuntimeException("failed to migrate booking")

    assertThrows<RuntimeException> { handler.handle(MigrateVideoBookingEvent(3)) }.message isEqualTo "failed to migrate booking"

    inOrder(videoBookingRepository, whereaboutsApiClient, migrationService, telemetryService) {
      verify(videoBookingRepository).existsByMigratedVideoBookingId(3)
      verify(whereaboutsApiClient).findBookingDetails(3)
      verify(migrationService).migrate(videoBookingMigrateResponse)
      verify(telemetryService).capture("Failed migration of booking 3 from whereabouts-api")
    }
  }

  @Test
  fun `should result in a no-op if booking already migrated`() {
    videoBookingRepository.stub {
      on { existsByMigratedVideoBookingId(1) } doReturn true
      on { existsByMigratedVideoBookingId(2) } doReturn true
      on { existsByMigratedVideoBookingId(3) } doReturn true
    }

    handler.handle(MigrateVideoBookingEvent(1))
    handler.handle(MigrateVideoBookingEvent(2))
    handler.handle(MigrateVideoBookingEvent(3))

    verify(videoBookingRepository).existsByMigratedVideoBookingId(1)
    verify(videoBookingRepository).existsByMigratedVideoBookingId(2)
    verify(videoBookingRepository).existsByMigratedVideoBookingId(3)
    verifyNoInteractions(whereaboutsApiClient)
    verifyNoInteractions(migrationService)
    verifyNoInteractions(telemetryService)
  }
}
