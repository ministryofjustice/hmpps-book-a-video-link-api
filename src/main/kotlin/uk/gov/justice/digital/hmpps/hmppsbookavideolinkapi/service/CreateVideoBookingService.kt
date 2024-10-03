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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkCaseLoadAccess
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toPrisonerDetails

@Service
class CreateVideoBookingService(
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
  private val videoBookingRepository: VideoBookingRepository,
  private val appointmentsService: AppointmentsService,
  private val bookingHistoryService: BookingHistoryService,
  private val prisonRepository: PrisonRepository,
  private val prisonerValidator: PrisonerValidator,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun create(booking: CreateVideoBookingRequest, createdBy: User): Pair<VideoBooking, Prisoner> =
    when (booking.bookingType!!) {
      BookingType.COURT -> createCourt(booking, createdBy)
      BookingType.PROBATION -> createProbation(booking, createdBy)
    }

  private fun createCourt(request: CreateVideoBookingRequest, createdBy: User): Pair<VideoBooking, Prisoner> {
    checkCaseLoadAccess(createdBy, request.prisoner().prisonCode!!)

    require(createdBy.isCourtUser || createdBy.isUserType(UserType.PRISON)) {
      "Only court and prison users can create court bookings."
    }

    val court = courtRepository.findByCode(request.courtCode!!)
      ?.also { if (!createdBy.isUserType(UserType.PRISON)) require(it.enabled) { "Court with code ${it.code} is not enabled" } }
      ?.also { requireNot(it.readOnly) { "Court with code ${it.code} is read-only" } }
      ?: throw EntityNotFoundException("Court with code ${request.courtCode} not found")

    val prisoner = request.prisoner().validate()

    return VideoBooking.newCourtBooking(
      court = court,
      hearingType = request.courtHearingType!!.name,
      comments = request.comments,
      videoUrl = request.videoLinkUrl,
      createdBy = createdBy.username,
      createdByPrison = createdBy.isUserType(UserType.PRISON),
    )
      .also { booking -> appointmentsService.createAppointmentsForCourt(booking, request.prisoner(), createdBy) }
      .also { booking -> videoBookingRepository.saveAndFlush(booking) }
      .also { booking -> bookingHistoryService.createBookingHistory(HistoryType.CREATE, booking) }
      .also { log.info("BOOKINGS: court booking with id ${it.videoBookingId} created") } to prisoner
  }

  private fun createProbation(request: CreateVideoBookingRequest, createdBy: User): Pair<VideoBooking, Prisoner> {
    require(createdBy.isProbationUser) {
      "Only probation users can create probation bookings."
    }

    val probationTeam = probationTeamRepository.findByCode(request.probationTeamCode!!)
      ?.also { require(it.enabled) { "Probation team with code ${it.code} is not enabled" } }
      ?: throw EntityNotFoundException("Probation team with code ${request.probationTeamCode} not found")

    val prisoner = request.prisoner().validate()

    return VideoBooking.newProbationBooking(
      probationTeam = probationTeam,
      probationMeetingType = request.probationMeetingType!!.name,
      comments = request.comments,
      videoUrl = request.videoLinkUrl,
      createdBy = createdBy.username,
      createdByPrison = createdBy.isUserType(UserType.PRISON),
    )
      .also { thisBooking -> appointmentsService.createAppointmentForProbation(thisBooking, request.prisoner(), createdBy) }
      .also { thisBooking -> videoBookingRepository.saveAndFlush(thisBooking) }
      .also { thisBooking -> bookingHistoryService.createBookingHistory(HistoryType.CREATE, thisBooking) }
      .also { thisBooking -> log.info("BOOKINGS: probation team booking ${thisBooking.videoBookingId} created") } to prisoner
  }

  // We will only be creating appointments for one single prisoner as part of the initial rollout.
  private fun CreateVideoBookingRequest.prisoner() = prisoners.first()

  private fun PrisonerDetails.validate(): Prisoner {
    // We are not checking if the prison is enabled here as we need to support prison users also.
    // Our UI should not be sending disabled prisons though.
    prisonRepository.findByCode(prisonCode!!) ?: throw EntityNotFoundException("Prison with code $prisonCode not found")
    return prisonerValidator.validatePrisonerAtPrison(prisonerNumber!!, prisonCode).toPrisonerDetails()
  }
}
