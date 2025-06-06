package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.AppointmentCreatedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.PrisonerAppointmentsChangedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.PrisonerMergedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.PrisonerReceivedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.PrisonerReleasedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.PrisonerVideoAppointmentCancelledEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingAmendedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingCancelledEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingCreatedEventHandler

class InboundEventsServiceTest {

  private val appointmentCreatedEventHandler: AppointmentCreatedEventHandler = mock()
  private val videoBookingCreatedEventHandler: VideoBookingCreatedEventHandler = mock()
  private val videoBookingCancelledEventHandler: VideoBookingCancelledEventHandler = mock()
  private val videoBookingAmendedEventHandler: VideoBookingAmendedEventHandler = mock()
  private val prisonerReleasedEventHandler: PrisonerReleasedEventHandler = mock()
  private val prisonerMergedEventHandler: PrisonerMergedEventHandler = mock()
  private val prisonerVideoAppointmentCancelledEventHandler: PrisonerVideoAppointmentCancelledEventHandler = mock()
  private val prisonerAppointmentsChangedEventHandler: PrisonerAppointmentsChangedEventHandler = mock()
  private val prisonerReceivedEventHandler: PrisonerReceivedEventHandler = mock()

  private val service = InboundEventsService(
    appointmentCreatedEventHandler,
    videoBookingCreatedEventHandler,
    videoBookingCancelledEventHandler,
    videoBookingAmendedEventHandler,
    prisonerReleasedEventHandler,
    prisonerMergedEventHandler,
    prisonerVideoAppointmentCancelledEventHandler,
    prisonerAppointmentsChangedEventHandler,
    prisonerReceivedEventHandler,
  )

  @Test
  fun `should call video booking created handler when for video booking created event`() {
    val event1 = VideoBookingCreatedEvent(1)
    service.process(event1)
    verify(videoBookingCreatedEventHandler).handle(event1)

    val event2 = VideoBookingCreatedEvent(2)
    service.process(event2)
    verify(videoBookingCreatedEventHandler).handle(event2)
  }

  @Test
  fun `should call appointments handler when for appointment created event`() {
    val event1 = AppointmentCreatedEvent(1)
    service.process(event1)
    verify(appointmentCreatedEventHandler).handle(event1)

    val event2 = AppointmentCreatedEvent(2)
    service.process(event2)
    verify(appointmentCreatedEventHandler).handle(event2)
  }

  @Test
  fun `should call video booking amended handler for a video booking amended event`() {
    val event1 = VideoBookingAmendedEvent(1)
    service.process(event1)
    verify(videoBookingAmendedEventHandler).handle(event1)

    val event2 = VideoBookingAmendedEvent(2)
    service.process(event2)
    verify(videoBookingAmendedEventHandler).handle(event2)
  }

  @Test
  fun `should call video booking cancelled handler when for video booking cancelled event`() {
    val event1 = VideoBookingCancelledEvent(1)
    service.process(event1)
    verify(videoBookingCancelledEventHandler).handle(event1)

    val event2 = VideoBookingCancelledEvent(2)
    service.process(event2)
    verify(videoBookingCancelledEventHandler).handle(event2)
  }

  @Test
  fun `should call prisoner released handler when for prisoner released event`() {
    val event1 = PrisonerReleasedEvent(ReleaseInformation("123456", "RELEASED", BIRMINGHAM))
    service.process(event1)
    verify(prisonerReleasedEventHandler).handle(event1)

    val event2 = PrisonerReleasedEvent(ReleaseInformation("78910", "RELEASED", BIRMINGHAM))
    service.process(event2)
    verify(prisonerReleasedEventHandler).handle(event2)
  }

  @Test
  fun `should call prisoner merged handler when for prisoner merged event`() {
    val event = PrisonerMergedEvent(MergeInformation(nomsNumber = "NEW", removedNomsNumber = "OLD"))
    service.process(event)
    verify(prisonerMergedEventHandler).handle(event)
  }

  @Test
  fun `should call prison video appointment cancelled handler when appointment cancelled event`() {
    val event = mock<PrisonerVideoAppointmentCancelledEvent>()
    service.process(event)
    verify(prisonerVideoAppointmentCancelledEventHandler).handle(event)
  }

  @Test
  fun `should call prisoner appointments changed event handler when prisoner appointment changed event`() {
    val event = mock<PrisonerAppointmentsChangedEvent>()
    service.process(event)
    verify(prisonerAppointmentsChangedEventHandler).handle(event)
  }

  @Test
  fun `should call prisoner received event handler when prisoner received event`() {
    val event = mock<PrisonerReceivedEvent>()
    service.process(event)
    verify(prisonerReceivedEventHandler).handle(event)
  }
}
