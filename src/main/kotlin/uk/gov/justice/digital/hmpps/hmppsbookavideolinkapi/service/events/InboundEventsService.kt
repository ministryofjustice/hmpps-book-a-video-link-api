package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.AppointmentCreatedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.PrisonerAppointmentsChangedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.PrisonerMergedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.PrisonerReceivedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.PrisonerReleasedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.PrisonerVideoAppointmentCancelledEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingAmendedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingCancelledEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingCreatedEventHandler

@Service
class InboundEventsService(
  private val appointmentCreatedEventHandler: AppointmentCreatedEventHandler,
  private val videoBookingCreatedEventHandler: VideoBookingCreatedEventHandler,
  private val videoBookingCancelledEventHandler: VideoBookingCancelledEventHandler,
  private val videoBookingAmendedEventHandler: VideoBookingAmendedEventHandler,
  private val prisonerReleasedEventHandler: PrisonerReleasedEventHandler,
  private val prisonerMergedEventHandler: PrisonerMergedEventHandler,
  private val prisonerVideoAppointmentCancelledEventHandler: PrisonerVideoAppointmentCancelledEventHandler,
  private val prisonerAppointmentsChangedEventHandler: PrisonerAppointmentsChangedEventHandler,
  private val prisonerReceivedEventHandler: PrisonerReceivedEventHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun process(event: DomainEvent<*>) {
    when (event) {
      is AppointmentCreatedEvent -> appointmentCreatedEventHandler.handle(event)
      is PrisonerMergedEvent -> prisonerMergedEventHandler.handle(event)
      is PrisonerReleasedEvent -> prisonerReleasedEventHandler.handle(event)
      is VideoBookingAmendedEvent -> videoBookingAmendedEventHandler.handle(event)
      is VideoBookingCancelledEvent -> videoBookingCancelledEventHandler.handle(event)
      is VideoBookingCreatedEvent -> videoBookingCreatedEventHandler.handle(event)
      is PrisonerVideoAppointmentCancelledEvent -> prisonerVideoAppointmentCancelledEventHandler.handle(event)
      is PrisonerAppointmentsChangedEvent -> prisonerAppointmentsChangedEventHandler.handle(event)
      is PrisonerReceivedEvent -> prisonerReceivedEventHandler.handle(event)
      else -> log.warn("Unsupported domain event ${event.javaClass.name}")
    }
  }
}
