package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.APPOINTMENT_CREATED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.VIDEO_BOOKING_AMENDED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType.VIDEO_BOOKING_CANCELLED
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
      APPOINTMENT_CREATED -> send(AppointmentCreatedEvent(identifier))
      VIDEO_BOOKING_CREATED -> send(VideoBookingCreatedEvent(identifier))
      VIDEO_BOOKING_CANCELLED -> send(VideoBookingCancelledEvent(identifier))
      VIDEO_BOOKING_AMENDED -> send(VideoBookingAmendedEvent(identifier))
      else -> throw IllegalArgumentException("Unsupported domain event $domainEventType")
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

  @Transactional
  override fun send(domainEventType: DomainEventType, identifier: Long) {
    if (isEnabled) {
      when (domainEventType) {
        VIDEO_BOOKING_CREATED -> {
          videoBookingRepository.findById(identifier).ifPresentOrElse(
            { vb ->
              vb.appointments().forEach { appointment ->
                manageExternalAppointmentsService.createAppointment(appointment.prisonAppointmentId)
              }
            },
            {
              log.info("Video booking with ID $identifier not found")
            },
          )
        }
        VIDEO_BOOKING_CANCELLED -> {
          videoBookingRepository.findById(identifier).ifPresentOrElse(
            { vb ->
              vb.appointments().forEach { appointment ->
                manageExternalAppointmentsService.cancelCurrentAppointment(appointment.prisonAppointmentId)
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
