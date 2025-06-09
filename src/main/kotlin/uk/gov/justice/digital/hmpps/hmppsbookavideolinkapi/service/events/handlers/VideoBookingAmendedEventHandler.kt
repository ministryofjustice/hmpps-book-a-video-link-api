package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingHistoryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ActivitiesAndAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingAmendedEvent
import kotlin.math.abs

@Component
class VideoBookingAmendedEventHandler(
  private val videoBookingRepository: VideoBookingRepository,
  private val bookingHistoryService: BookingHistoryService,
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService,
  private val activitiesService: ActivitiesAndAppointmentsService,
) : DomainEventHandler<VideoBookingAmendedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  override fun handle(event: VideoBookingAmendedEvent) {
    videoBookingRepository
      .findById(abs(event.additionalInformation.videoBookingId))
      .ifPresentOrElse(
        { amendedBooking ->
          // Get the previous history row - the one before the one just added (which should always exist)
          val oldBooking = bookingHistoryService.getByVideoBookingId(amendedBooking.videoBookingId)
            .sortedByDescending { history -> history.createdTime }
            .let {
              if (event.additionalInformation.videoBookingId < 0) {
                // This feature allows negative video booking ids received in events to trigger a re-sync of appointments
                // without a booking history row first being created (provided something has changed on the appointment)
                // e.g. by an SQL update applied beforehand.
                log.info("Processing negative videoBookingId ${event.additionalInformation.videoBookingId}")

                it[0]
              } else {
                log.info("Processing positive videoBookingId ${event.additionalInformation.videoBookingId}")

                it[1]
              }
            }

          if (activitiesService.isAppointmentsRolledOutAt(amendedBooking.prisonCode())) {
            when (amendedBooking.bookingType) {
              BookingType.COURT -> {
                val (previousPreHearing, amendedPreHearing) = oldBooking.preHearing() to amendedBooking.preHearing()
                val (previousMainHearing, amendedMainHearing) = oldBooking.mainHearing() to amendedBooking.mainHearing()!!
                val (previousPostHearing, amendedPostHearing) = oldBooking.postHearing() to amendedBooking.postHearing()

                // All cancellations must be done first
                cancelIfRemoved(previousPreHearing, amendedPreHearing)
                cancelIfRemoved(previousPostHearing, amendedPostHearing)

                if (amendedMainHearing.isTheSameAs(previousMainHearing)) {
                  amendedPreHearing?.run { createOrAmend(previousPreHearing, amendedPreHearing) }
                  amendedPostHearing?.run { createOrAmend(previousPostHearing, amendedPostHearing) }
                } else {
                  // The order in which appointments are created or amended depends on if the booking is now earlier or
                  // later. If the main hearing is now earlier then we process pre, main and then post.
                  // If the main hearing is now later (or longer) than before then we process post, main and then pre.
                  if (amendedMainHearing.isEarlierThanBefore(previousMainHearing)) {
                    amendedPreHearing?.run { createOrAmend(previousPreHearing, amendedPreHearing) }
                    amendIfNotTheSame(previousMainHearing, amendedMainHearing)
                    amendedPostHearing?.run { createOrAmend(previousPostHearing, amendedPostHearing) }
                  } else {
                    amendedPostHearing?.run { createOrAmend(previousPostHearing, amendedPostHearing) }
                    amendIfNotTheSame(previousMainHearing, amendedMainHearing)
                    amendedPreHearing?.run { createOrAmend(previousPreHearing, amendedPreHearing) }
                  }
                }
              }
              BookingType.PROBATION -> amendIfNotTheSame(oldBooking.probationMeeting(), amendedBooking.probationMeeting()!!)
            }
          } else {
            // All cancellations must be done first so as not to interfere with new appointments
            // There is no ability to amend appointments for non-rolled out prisons so we have to cancel and create
            oldBooking.appointments().forEach(manageExternalAppointmentsService::cancelPreviousAppointment)
            amendedBooking.appointments().forEach(manageExternalAppointmentsService::createAppointment)
          }

          log.info("Processed BOOKING_AMENDED event for videoBookingId ${amendedBooking.videoBookingId}")
        },
        {
          // Ignore, there is nothing we can do if we do not find the booking
          log.warn("Video booking with ID ${event.additionalInformation.videoBookingId} not found")
        },
      )
  }

  private fun PrisonAppointment?.isTheSameAs(historyAppointment: BookingHistoryAppointment?) = run {
    this != null &&
      videoBooking.videoBookingId == historyAppointment?.bookingHistory?.videoBookingId &&
      appointmentDate == historyAppointment.appointmentDate &&
      startTime == historyAppointment.startTime &&
      endTime == historyAppointment.endTime &&
      prisonerNumber == historyAppointment.prisonerNumber &&
      prisonLocationId == historyAppointment.prisonLocationId &&
      comments == historyAppointment.bookingHistory.comments &&
      notesForPrisoners == historyAppointment.bookingHistory.notesForPrisoners &&
      notesForStaff == historyAppointment.bookingHistory.notesForStaff
  }

  private fun PrisonAppointment.isEarlierThanBefore(historyAppointment: BookingHistoryAppointment) = run {
    appointmentDate.atTime(startTime).isBefore(historyAppointment.appointmentDate.atTime(historyAppointment.startTime))
  }

  private fun cancelIfRemoved(old: BookingHistoryAppointment?, new: PrisonAppointment?) {
    old?.takeIf { new == null }?.run(manageExternalAppointmentsService::cancelPreviousAppointment)
  }

  private fun createOrAmend(old: BookingHistoryAppointment?, amended: PrisonAppointment) {
    if (old == null) {
      manageExternalAppointmentsService.createAppointment(amended)
    } else {
      amendIfNotTheSame(old, amended)
    }
  }

  private fun amendIfNotTheSame(old: BookingHistoryAppointment, amended: PrisonAppointment) {
    if (amended.isTheSameAs(old)) {
      log.info("No change to meeting date and times, ignoring amend for appointment $amended")
    } else {
      manageExternalAppointmentsService.amendAppointment(old, amended)
    }
  }
}
