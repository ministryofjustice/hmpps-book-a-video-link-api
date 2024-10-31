package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingCancelledEvent
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

class VideoBookingCancelledEventHandlerTest {
  private val booking = courtBooking()
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_PRE",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = birminghamLocation.id,
    )
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(13, 30),
      locationId = birminghamLocation.id,
    )

  private val videoBookingRepository: VideoBookingRepository = mock()
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService = mock()
  private val handler = VideoBookingCancelledEventHandler(videoBookingRepository, manageExternalAppointmentsService)

  @Test
  fun `should cancel external appointment on receipt of video booking`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(booking)

    handler.handle(VideoBookingCancelledEvent(1))

    verify(manageExternalAppointmentsService, times(2)).cancelCurrentAppointment(anyLong())
    verifyNoMoreInteractions(manageExternalAppointmentsService)
  }

  @Test
  fun `should no-op receipt of unknown video booking`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.empty()

    handler.handle(VideoBookingCancelledEvent(1))

    verifyNoInteractions(manageExternalAppointmentsService)
  }
}
