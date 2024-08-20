package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.requireNot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkCaseLoadAccess
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkVideoBookingAccess
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toPrisonerDetails
import java.time.LocalDateTime

@Service
class AmendVideoBookingService(
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
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
      .also {
        require(BookingType.valueOf(it.bookingType) == request.bookingType) {
          "The booking type ${it.bookingType} does not match the requested type ${request.bookingType}."
        }
      }

    return when (BookingType.valueOf(booking.bookingType)) {
      BookingType.COURT -> amendCourt(booking, request, amendedBy)
      BookingType.PROBATION -> amendProbation(booking, request, amendedBy)
    }
  }

  private fun amendCourt(existingBooking: VideoBooking, request: AmendVideoBookingRequest, amendedBy: User): Pair<VideoBooking, Prisoner> {
    val requestedCourt = courtRepository.findByCode(request.courtCode!!)
      ?.also { require(it.enabled) { "Court with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Court with code ${request.courtCode} not found")

    requireNot(amendedBy.isUserType(UserType.PRISON) && existingBooking.court != requestedCourt) {
      "Prison users cannot change the court on a booking."
    }

    val prisoner = request.prisoner().validate()

    return existingBooking.apply {
      this.court = requestedCourt
      hearingType = request.courtHearingType!!.name
      comments = request.comments
      videoUrl = request.videoLinkUrl
      this.amendedBy = amendedBy.username
      amendedTime = LocalDateTime.now()
    }
      .also { thisBooking -> thisBooking.removeAllAppointments() }
      .also { thisBooking -> appointmentsService.createAppointmentsForCourt(thisBooking, request.prisoner(), amendedBy) }
      .also { thisBooking -> videoBookingRepository.saveAndFlush(thisBooking) }
      .also { thisBooking -> bookingHistoryService.createBookingHistory(HistoryType.AMEND, thisBooking) }
      .also { thisBooking -> log.info("BOOKINGS: court booking ${thisBooking.videoBookingId} amended") } to prisoner
  }

  private fun amendProbation(booking: VideoBooking, request: AmendVideoBookingRequest, amendedBy: User): Pair<VideoBooking, Prisoner> {
    val probationTeam = probationTeamRepository.findByCode(request.probationTeamCode!!)
      ?.also { require(it.enabled) { "Probation team with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Probation team with code ${request.probationTeamCode} not found")

    val prisoner = request.prisoner().validate()

    return booking.apply {
      this.probationTeam = probationTeam
      probationMeetingType = request.probationMeetingType!!.name
      comments = request.comments
      videoUrl = request.videoLinkUrl
      this.amendedBy = amendedBy.username
      amendedTime = LocalDateTime.now()
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
