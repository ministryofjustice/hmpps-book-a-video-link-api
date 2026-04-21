package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ChangeTrackingService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ChangeType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ServiceUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.VideoBookingServiceDelegate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability.AvailabilityService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryEventFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType as RequestBookingType

/**
 * This facade exists to ensure all booking-related transactions are fully committed before sending any events, emails, and telemetry.
 *
 * Calls to the facade should not be wrapped in transactions, the facade should be the starting point of any transaction.
 */
@Component
class BookingFacade(
  private val videoBookingServiceDelegate: VideoBookingServiceDelegate,
  private val outboundEventsService: OutboundEventsService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val telemetryService: TelemetryService,
  private val availabilityService: AvailabilityService,
  private val changeTrackingService: ChangeTrackingService,
  private val emailFacade: EmailFacade,
  private val rescheduleEmailsFacade: RescheduleEmailsFacade,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun create(bookingRequest: CreateVideoBookingRequest, createdBy: User): Long {
    require(createdBy is PrisonUser || availabilityService.isAvailable(bookingRequest)) {
      if (bookingRequest.bookingType == RequestBookingType.COURT) {
        "Unable to create court booking, booking overlaps with an existing appointment."
      } else {
        "Unable to create probation booking, booking overlaps with an existing appointment."
      }
    }

    val (booking, prisoner) = videoBookingServiceDelegate.create(bookingRequest, createdBy)
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CREATED, booking.videoBookingId)
    emailFacade.sendEmails(BookingAction.CREATE, booking, prisoner, createdBy)
    trackTelemetry(BookingAction.CREATE, booking, createdBy)
    return booking.videoBookingId
  }

  fun amend(videoBookingId: Long, bookingRequest: AmendVideoBookingRequest, amendedBy: User): Long {
    require(amendedBy is PrisonUser || availabilityService.isAvailable(videoBookingId, bookingRequest)) {
      if (bookingRequest.bookingType == RequestBookingType.COURT) {
        "Unable to amend court booking, booking overlaps with an existing appointment."
      } else {
        "Unable to amend probation booking, booking overlaps with an existing appointment."
      }
    }

    val originalBooking = videoBookingServiceDelegate.getVideoBookingById(videoBookingId, amendedBy)

    // Need to check before amendment to see before picture.
    val changeType = changeTrackingService.determineChangeType(videoBookingId, bookingRequest, amendedBy)

    // Amend regardless of changes (this is in-case new fields ever are introduced but not covered by the change check).
    val (booking, prisoner) = videoBookingServiceDelegate.amend(videoBookingId, bookingRequest, amendedBy)
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_AMENDED, videoBookingId)

    // Only send emails on back of change check above.
    if (changeType != ChangeType.NONE) {
      // TODO check if a reschedule is required here and use the appropriate facade.
      emailFacade.sendEmails(BookingAction.AMEND, booking, prisoner, amendedBy, changeType)
    } else {
      log.info("No changes detected for video booking $videoBookingId, not sending email")
    }

    trackTelemetry(BookingAction.AMEND, booking, amendedBy)

    return videoBookingId
  }

  fun cancel(videoBookingId: Long, cancelledBy: PrisonUser) {
    cancelBooking(videoBookingId, cancelledBy)
  }

  fun cancel(videoBookingId: Long, cancelledBy: ExternalUser) {
    cancelBooking(videoBookingId, cancelledBy)
  }

  fun request(booking: RequestVideoBookingRequest, user: ExternalUser) {
    videoBookingServiceDelegate.request(booking, user)
  }

  private fun cancelBooking(videoBookingId: Long, cancelledBy: User) {
    val booking = videoBookingServiceDelegate.cancel(videoBookingId, cancelledBy)
    log.info("Video booking ${booking.videoBookingId} cancelled by user type ${cancelledBy::class.simpleName}")
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CANCELLED, videoBookingId)
    emailFacade.sendEmails(BookingAction.CANCEL, booking, getPrisoner(booking.prisoner()), cancelledBy)
    trackTelemetry(BookingAction.CANCEL, booking, cancelledBy)
  }

  fun cancel(videoBookingId: Long, cancelledBy: ServiceUser) {
    // No events should be raised on the back of calling this function, this is by design.
    val booking = videoBookingServiceDelegate.cancel(videoBookingId, cancelledBy)
    log.info("Video booking ${booking.videoBookingId} cancelled by user type ${cancelledBy::class.simpleName}")
    emailFacade.sendEmails(BookingAction.CANCEL, booking, getPrisoner(booking.prisoner()), cancelledBy)
    trackTelemetry(BookingAction.CANCEL, booking, cancelledBy)
  }

  fun courtHearingLinkReminder(videoBooking: VideoBooking, user: ServiceUser) {
    require(videoBooking.isBookingType(COURT) && videoBooking.isStatus(StatusCode.ACTIVE)) { "Video booking with id ${videoBooking.videoBookingId} must be an active court booking" }
    require(videoBooking.court!!.enabled) { "Video booking with id ${videoBooking.videoBookingId} is not with an enabled court" }
    require(videoBooking.appointments().any { it.appointmentDate > LocalDate.now() }) { "Video booking with id ${videoBooking.videoBookingId} must be after today" }
    require(videoBooking.videoUrl == null) { "Video booking with id ${videoBooking.videoBookingId} already has a court hearing link" }
    emailFacade.sendEmails(BookingAction.COURT_HEARING_LINK_REMINDER, videoBooking, getPrisoner(videoBooking.prisoner()), user)
  }

  fun sendProbationOfficerDetailsReminder(videoBooking: VideoBooking, user: ServiceUser) {
    require(videoBooking.isBookingType(PROBATION) && videoBooking.isStatus(StatusCode.ACTIVE)) { "Video booking with id ${videoBooking.videoBookingId} must be an active probation booking" }
    require(videoBooking.probationTeam!!.enabled) { "Video booking with id ${videoBooking.videoBookingId} is not with an enabled probation team" }
    require(videoBooking.appointments().any { it.appointmentDate > LocalDate.now() }) { "Video booking with id ${videoBooking.videoBookingId} must be after today" }
    emailFacade.sendEmails(BookingAction.PROBATION_OFFICER_DETAILS_REMINDER, videoBooking, getPrisoner(videoBooking.prisoner()), user)
  }

  fun prisonerTransferred(videoBookingId: Long, user: ServiceUser) {
    val booking = videoBookingServiceDelegate.cancel(videoBookingId, user)
    log.info("Video booking ${booking.videoBookingId} cancelled due to transfer")
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CANCELLED, videoBookingId)
    emailFacade.sendEmails(BookingAction.TRANSFERRED, booking, getReleasedOrTransferredPrisoner(booking.prisoner()), user)
    trackTelemetry(BookingAction.TRANSFERRED, booking, user)
  }

  fun prisonerReleased(videoBookingId: Long, user: ServiceUser) {
    val booking = videoBookingServiceDelegate.cancel(videoBookingId, user)
    log.info("Video booking ${booking.videoBookingId} cancelled due to release")
    outboundEventsService.send(DomainEventType.VIDEO_BOOKING_CANCELLED, videoBookingId)
    emailFacade.sendEmails(BookingAction.RELEASED, booking, getReleasedOrTransferredPrisoner(booking.prisoner()), user)
    trackTelemetry(BookingAction.RELEASED, booking, user)
  }

  private fun trackTelemetry(action: BookingAction, booking: VideoBooking, user: User) {
    TelemetryEventFactory.event(action, booking, user)?.let(telemetryService::track)
  }

  private fun getPrisoner(prisonerNumber: String) = prisonerSearchClient.getPrisoner(prisonerNumber)!!.let { Prisoner(it.prisonerNumber, it.prisonId!!, it.firstName, it.lastName, it.dateOfBirth) }

  private fun getReleasedOrTransferredPrisoner(prisonerNumber: String) = prisonerSearchClient.getPrisoner(prisonerNumber)!!.let { Prisoner(it.prisonerNumber, it.lastPrisonId!!, it.firstName, it.lastName, it.dateOfBirth) }
}

enum class BookingAction {
  CREATE,
  AMEND,
  CANCEL,
  COURT_HEARING_LINK_REMINDER,
  PROBATION_OFFICER_DETAILS_REMINDER,
  RELEASED,
  TRANSFERRED,
}
