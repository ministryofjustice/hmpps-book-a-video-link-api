package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.whereaboutsapi.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

@Service
class MigrateVideoBookingService(
  private val mappingService: MigrateMappingService,
  private val videoBookingRepository: VideoBookingRepository,
  private val bookingHistoryRepository: BookingHistoryRepository,
) {
  @Transactional
  fun migrate(booking: VideoBookingMigrateResponse) {
    // TODO lookup prisoner number from using booking details booking ID
    // TODO lookup location business key(s) using booking details internal location(s)
    // TODO map court/probation codes using booking details
    // TODO need to consider comments length from old to new, new is too short at present
    // TODO persist migrated booking details (with migratedFlag=true)
    // TODO persist migrated booking history
  }
}
