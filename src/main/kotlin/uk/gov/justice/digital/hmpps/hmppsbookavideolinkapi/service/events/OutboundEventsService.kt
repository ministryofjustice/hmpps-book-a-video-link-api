package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.APPOINTMENT_CREATED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.VIDEO_BOOKING_CREATED

fun interface OutboundEventsService {
  fun send(domainEventType: DomainEventType, identifier: Long)
}

@Profile("!local")
@Service
class OutboundEventsServiceImpl(
  private val outboundEventsPublisher: OutboundEventsPublisher,
) : OutboundEventsService {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("Real outbound events service")
  }

  override fun send(domainEventType: DomainEventType, identifier: Long) {
    when (domainEventType) {
      APPOINTMENT_CREATED -> send(AppointmentCreatedEvent(AppointmentInformation(identifier)))
      VIDEO_BOOKING_CREATED -> send(VideoBookingCreatedEvent(VideoBookingInformation(identifier)))
    }
  }

  private fun send(event: DomainEvent<*>) {
    outboundEventsPublisher.send(event)
  }
}

@Profile("local")
@Service
class LocalOutboundEventsService(
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonAppointmentRepository: PrisonAppointmentRepository,
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService,
  featureSwitches: FeatureSwitches,
) : OutboundEventsService {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val isEnabled = featureSwitches.isEnabled(Feature.SNS_ENABLED)

  init {
    log.info("Local outbound events service. ")
  }

  override fun send(domainEventType: DomainEventType, identifier: Long) {
    if (isEnabled) {
      when (domainEventType) {
        VIDEO_BOOKING_CREATED -> {
          videoBookingRepository.findById(identifier).ifPresentOrElse(
            { vb ->
              prisonAppointmentRepository.findByVideoBooking(vb).forEach { appointment ->
                manageExternalAppointmentsService.createAppointment(appointment.prisonAppointmentId)
              }
            },
            {
              log.info("Video booking with ID $identifier not found")
            },
          )
        }
        else -> log.info("Ignoring domain event $domainEventType")
      }
    } else {
      log.info("Ignoring domain events, not enabled")
    }
  }
}
