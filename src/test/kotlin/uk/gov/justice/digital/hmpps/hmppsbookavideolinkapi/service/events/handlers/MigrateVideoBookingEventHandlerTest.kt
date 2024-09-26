package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.AppointmentLocationTimeSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.MigrationClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration.MigrateVideoBookingService
import java.time.LocalTime

class MigrateVideoBookingEventHandlerTest {
  private val videoBookingMigrateResponse = VideoBookingMigrateResponse(
    videoBookingId = 1,
    offenderBookingId = 1,
    prisonCode = MOORLAND,
    courtCode = DERBY_JUSTICE_CENTRE,
    courtName = null,
    probation = false,
    createdByUsername = "TEST",
    madeByTheCourt = true,
    pre = null,
    main = AppointmentLocationTimeSlot(1, today(), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT.plusHours(1)),
    post = null,
    cancelled = false,
    comment = "some comments",
    events = emptyList(),
  )
  private val migratedBooking: VideoBooking = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val whereaboutsApiClient: MigrationClient = mock()
  private val migrationService: MigrateVideoBookingService = mock()
  private val telemetryService: TelemetryService = mock()
  private val telemetryCaptor = argumentCaptor<TelemetryEvent>()
  private val handler = MigrateVideoBookingEventHandler(videoBookingRepository, whereaboutsApiClient, migrationService, telemetryService)

  @Test
  fun `should capture telemetry information for successful migration`() {
    whenever(whereaboutsApiClient.findBookingToMigrate(1)) doReturn videoBookingMigrateResponse
    whenever(migrationService.migrate(videoBookingMigrateResponse)) doReturn migratedBooking

    migratedBooking.stub {
      on { videoBookingId } doReturn 99
      on { court } doReturn court(DERBY_JUSTICE_CENTRE)
      on { migratedVideoBookingId } doReturn 1
      on { prisonCode() } doReturn MOORLAND
    }

    handler.handle(MigrateVideoBookingEvent(1))

    inOrder(videoBookingRepository, whereaboutsApiClient, migrationService, telemetryService) {
      verify(videoBookingRepository).existsByMigratedVideoBookingId(1)
      verify(whereaboutsApiClient).findBookingToMigrate(1)
      verify(migrationService).migrate(videoBookingMigrateResponse)
      verify(telemetryService).track(telemetryCaptor.capture())
    }

    val successfulTelemetryEvent = telemetryCaptor.firstValue as MigratedBookingSuccessTelemetryEvent

    successfulTelemetryEvent.eventType isEqualTo TelemetryEventType.MIGRATED_BOOKING_SUCCESS
    successfulTelemetryEvent.properties() containsEntriesExactlyInAnyOrder mapOf(
      "video_booking_id" to "99",
      "migrated_video_booking_id" to "1",
      "court" to DERBY_JUSTICE_CENTRE,
      "prison" to MOORLAND,
    )
  }

  @Test
  fun `should capture telemetry information for failed migration when booking not found`() {
    whenever(whereaboutsApiClient.findBookingToMigrate(2)) doReturn null

    handler.handle(MigrateVideoBookingEvent(2))

    inOrder(videoBookingRepository, whereaboutsApiClient, telemetryService) {
      verify(videoBookingRepository).existsByMigratedVideoBookingId(2)
      verify(whereaboutsApiClient).findBookingToMigrate(2)
      verify(telemetryService).track(telemetryCaptor.capture())
    }

    with(telemetryCaptor.firstValue as MigratedBookingFailureTelemetryEvent) {
      eventType isEqualTo TelemetryEventType.MIGRATED_BOOKING_FAILURE
      properties() containsEntriesExactlyInAnyOrder mapOf("video_booking_id" to "2", "message" to "booking not found in whereabouts-api")
    }

    verify(migrationService, never()).migrate(anyOrNull())
  }

  @Test
  fun `should capture telemetry information for failed migration when migration fails`() {
    whenever(whereaboutsApiClient.findBookingToMigrate(3)) doReturn videoBookingMigrateResponse
    whenever(migrationService.migrate(anyOrNull())) doThrow RuntimeException("failed to migrate booking")

    assertThrows<RuntimeException> { handler.handle(MigrateVideoBookingEvent(3)) }.message isEqualTo "failed to migrate booking"

    inOrder(videoBookingRepository, whereaboutsApiClient, migrationService, telemetryService) {
      verify(videoBookingRepository).existsByMigratedVideoBookingId(3)
      verify(whereaboutsApiClient).findBookingToMigrate(3)
      verify(migrationService).migrate(videoBookingMigrateResponse)
      verify(telemetryService).track(telemetryCaptor.capture())
    }

    with(telemetryCaptor.firstValue as MigratedBookingFailureTelemetryEvent) {
      eventType isEqualTo TelemetryEventType.MIGRATED_BOOKING_FAILURE
      properties() containsEntriesExactlyInAnyOrder mapOf("video_booking_id" to "3", "message" to "failed to migrate booking")
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
    verify(telemetryService, times(3)).track(telemetryCaptor.capture())

    with(telemetryCaptor.firstValue as MigratedBookingFailureTelemetryEvent) {
      eventType isEqualTo TelemetryEventType.MIGRATED_BOOKING_FAILURE
      properties() containsEntriesExactlyInAnyOrder mapOf("video_booking_id" to "1", "message" to "booking already migrated")
    }
    with(telemetryCaptor.secondValue as MigratedBookingFailureTelemetryEvent) {
      eventType isEqualTo TelemetryEventType.MIGRATED_BOOKING_FAILURE
      properties() containsEntriesExactlyInAnyOrder mapOf("video_booking_id" to "2", "message" to "booking already migrated")
    }
    with(telemetryCaptor.thirdValue as MigratedBookingFailureTelemetryEvent) {
      eventType isEqualTo TelemetryEventType.MIGRATED_BOOKING_FAILURE
      properties() containsEntriesExactlyInAnyOrder mapOf("video_booking_id" to "3", "message" to "booking already migrated")
    }

    verifyNoInteractions(whereaboutsApiClient)
    verifyNoInteractions(migrationService)
  }
}
