package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDateTime

@Service
class AmendVideoBookingService(
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
  private val videoBookingRepository: VideoBookingRepository,
  prisonAppointmentRepository: PrisonAppointmentRepository,
  prisonRepository: PrisonRepository,
  locationValidator: LocationValidator,
  prisonerValidator: PrisonerValidator,
) : CreateVideoAppointmentService(prisonRepository, prisonAppointmentRepository, locationValidator, prisonerValidator) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun amend(videoBookingId: Long, request: AmendVideoBookingRequest, amendedBy: String): Pair<VideoBooking, Prisoner> {
    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }
      .also {
        require(BookingType.valueOf(it.bookingType) == request.bookingType) {
          "The booking type ${it.bookingType} does not match the requested type ${request.bookingType}"
        }
      }

    return when (BookingType.valueOf(booking.bookingType)) {
      BookingType.COURT -> amendCourt(booking, request, amendedBy)
      BookingType.PROBATION -> amendProbation(booking, request, amendedBy)
    }
  }

  private fun amendCourt(booking: VideoBooking, request: AmendVideoBookingRequest, amendedBy: String): Pair<VideoBooking, Prisoner> {
    val court = courtRepository.findByCode(request.courtCode!!)
      ?.also { require(it.enabled) { "Court with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Court with code ${request.courtCode} not found")

    val prisoner = request.prisoner().validate()

    booking.apply {
      this.court = court
      hearingType = request.courtHearingType!!.name
      comments = request.comments
      videoUrl = request.videoLinkUrl
      this.amendedBy = amendedBy
      amendedTime = LocalDateTime.now()
    }

    return booking.let(videoBookingRepository::saveAndFlush)
      .also { deletePrisonAppointments(it) }
      .also { createAppointmentsForCourt(booking, request.prisoner()) }
      .also { log.info("BOOKINGS: court booking ${it.videoBookingId} amended") } to prisoner
  }

  private fun amendProbation(booking: VideoBooking, request: AmendVideoBookingRequest, amendedBy: String): Pair<VideoBooking, Prisoner> {
    val probationTeam = probationTeamRepository.findByCode(request.probationTeamCode!!)
      ?.also { require(it.enabled) { "Probation team with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Probation team with code ${request.probationTeamCode} not found")

    val prisoner = request.prisoner().validate()

    booking.apply {
      this.probationTeam = probationTeam
      probationMeetingType = request.probationMeetingType!!.name
      comments = request.comments
      videoUrl = request.videoLinkUrl
      this.amendedBy = amendedBy
      amendedTime = LocalDateTime.now()
    }

    return booking.let(videoBookingRepository::saveAndFlush)
      .also { deletePrisonAppointments(it) }
      .also { createAppointmentForProbation(booking, request.prisoner()) }
      .also { log.info("BOOKINGS: probation team booking ${it.videoBookingId} amended") } to prisoner
  }

  // We will only be creating appointments for one single prisoner as part of the initial rollout.
  private fun AmendVideoBookingRequest.prisoner() = prisoners.first()
}
