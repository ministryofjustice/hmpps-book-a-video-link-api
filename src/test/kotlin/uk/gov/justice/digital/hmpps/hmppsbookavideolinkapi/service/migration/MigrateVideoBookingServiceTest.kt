package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

class MigrateVideoBookingServiceTest {
  private val migrateMappingService: MigrateMappingService = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val bookingHistoryRepository: BookingHistoryRepository = mock()
  private val service = MigrateVideoBookingService(migrateMappingService, videoBookingRepository, bookingHistoryRepository)

  @Test
  fun `should do something`() {
  }
}
