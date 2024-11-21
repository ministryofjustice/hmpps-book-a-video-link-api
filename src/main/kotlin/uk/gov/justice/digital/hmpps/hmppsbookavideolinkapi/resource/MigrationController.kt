package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.MigrateVideoBookingEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.MigrateVideoBookingEventHandler

@Tag(name = "Migrate a video link booking from old BVLS")
@RestController
@RequestMapping(value = ["migrate"])
@PreAuthorize("hasAnyRole('BOOK_A_VIDEO_LINK_ADMIN')")
@Deprecated(message = "Can be removed when migration is completed")
class MigrationController(private val migrateVideoBookingEventHandler: MigrateVideoBookingEventHandler) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Operation(summary = "Endpoint to migrate a single video booking from old BVLS into this service")
  @GetMapping(value = ["/{videoBookingId}"], produces = [MediaType.TEXT_PLAIN_VALUE])
  fun migrateSingle(
    @PathVariable("videoBookingId") videoBookingId: Long,
  ): String {
    migrateVideoBookingEventHandler.handle(MigrateVideoBookingEvent(videoBookingId))

    return "Migrated booking $videoBookingId"
  }
}
