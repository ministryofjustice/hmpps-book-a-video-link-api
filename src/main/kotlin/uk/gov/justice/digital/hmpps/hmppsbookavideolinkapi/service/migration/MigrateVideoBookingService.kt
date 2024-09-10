package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.whereaboutsapi.LegacyBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

@Service
class MigrateVideoBookingService(
  private val migrateMappingService: MigrateMappingService,
  private val videoBookingRepository: VideoBookingRepository,
  private val bookingHistoryRepository: BookingHistoryRepository,
) {
  @Transactional
  fun migrate(booking: LegacyBooking) {
    // TODO lookup prisoner number from using booking details booking ID
    // TODO lookup location business key(s) using booking details internal location(s)
    // TODO map court/probation codes using booking details
    // TODO persist migrated booking details (with migratedFlag=true)
    // TODO persist migrated booking history
  }
}
