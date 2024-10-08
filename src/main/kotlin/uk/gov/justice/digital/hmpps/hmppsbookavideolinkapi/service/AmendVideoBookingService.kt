package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkCaseLoadAccess
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkVideoBookingAccess
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toPrisonerDetails
import java.time.LocalDateTime.now

@Service
class AmendVideoBookingService(
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonRepository: PrisonRepository,
  private val appointmentsService: AppointmentsService,
  private val bookingHistoryService: BookingHistoryService,
  private val prisonerValidator: PrisonerValidator,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun amend(videoBookingId: Long, request: AmendVideoBookingRequest, amendedBy: User): Pair<VideoBooking, Prisoner> {
    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found.") }
      .also { checkVideoBookingAccess(amendedBy, it) }
      .also { checkCaseLoadAccess(amendedBy, it.prisonCode()) }
      .also { require(BookingType.valueOf(it.bookingType) == request.bookingType) { "The booking type ${it.bookingType} does not match the requested type ${request.bookingType}." } }
      .also { require(it.statusCode != StatusCode.CANCELLED) { "Video booking $videoBookingId is already cancelled, and so cannot be amended" } }
      .also { require(it.appointments().all { a -> a.isStartsAfter(now()) }) { "Video booking $videoBookingId has already started, and so cannot be amended" } }

    return when (BookingType.valueOf(booking.bookingType)) {
      BookingType.COURT -> amendCourt(booking, request, amendedBy)
      BookingType.PROBATION -> amendProbation(booking, request, amendedBy)
    }
  }

  private fun amendCourt(existingBooking: VideoBooking, request: AmendVideoBookingRequest, amendedBy: User): Pair<VideoBooking, Prisoner> {
    val prisoner = request.prisoner().validate()

    return existingBooking.apply {
      hearingType = request.courtHearingType!!.name
      comments = request.comments
      videoUrl = request.videoLinkUrl
      this.amendedBy = amendedBy.username
      amendedTime = now()
    }
      .also { thisBooking -> thisBooking.removeAllAppointments() }
      .also { thisBooking -> appointmentsService.createAppointmentsForCourt(thisBooking, request.prisoner(), amendedBy) }
      .also { thisBooking -> videoBookingRepository.saveAndFlush(thisBooking) }
      .also { thisBooking -> bookingHistoryService.createBookingHistory(HistoryType.AMEND, thisBooking) }
      .also { thisBooking -> log.info("BOOKINGS: court booking ${thisBooking.videoBookingId} amended") } to prisoner
  }

  private fun amendProbation(booking: VideoBooking, request: AmendVideoBookingRequest, amendedBy: User): Pair<VideoBooking, Prisoner> {
    val prisoner = request.prisoner().validate()

    return booking.apply {
      probationMeetingType = request.probationMeetingType!!.name
      comments = request.comments
      videoUrl = request.videoLinkUrl
      this.amendedBy = amendedBy.username
      amendedTime = now()
    }
      .also { thisBooking -> thisBooking.removeAllAppointments() }
      .also { thisBooking -> appointmentsService.createAppointmentForProbation(thisBooking, request.prisoner(), amendedBy) }
      .also { thisBooking -> videoBookingRepository.saveAndFlush(thisBooking) }
      .also { thisBooking -> bookingHistoryService.createBookingHistory(HistoryType.AMEND, thisBooking) }
      .also { thisBooking -> log.info("BOOKINGS: probation team booking ${thisBooking.videoBookingId} amended") } to prisoner
  }

  // We will only be creating appointments for one single prisoner as part of the initial rollout.
  private fun AmendVideoBookingRequest.prisoner() = prisoners.first()

  private fun PrisonerDetails.validate(): Prisoner {
    // We are not checking if the prison is enabled here as we need to support prison users also.
    // Our UI should not be sending disabled prisons though.
    prisonRepository.findByCode(prisonCode!!) ?: throw EntityNotFoundException("Prison with code $prisonCode not found")
    return prisonerValidator.validatePrisonerAtPrison(prisonerNumber!!, prisonCode).toPrisonerDetails()
  }
}
