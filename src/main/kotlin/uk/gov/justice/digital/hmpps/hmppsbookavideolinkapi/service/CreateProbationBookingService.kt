package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerValidator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AdditionalBookingDetail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toPrisonerDetails

@Service
class CreateProbationBookingService(
  private val probationTeamRepository: ProbationTeamRepository,
  private val videoBookingRepository: VideoBookingRepository,
  private val appointmentsService: AppointmentsService,
  private val bookingHistoryService: BookingHistoryService,
  private val prisonRepository: PrisonRepository,
  private val prisonerValidator: PrisonerValidator,
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun create(request: CreateVideoBookingRequest, createdBy: User): Pair<VideoBooking, Prisoner> {
    require(request.bookingType == BookingType.PROBATION) {
      "CREATE PROBATION BOOKING: booking type is not probation"
    }

    require((createdBy is ExternalUser && createdBy.isProbationUser) || createdBy is PrisonUser) {
      "Only probation users and prison users can create probation bookings."
    }

    val probationTeam = probationTeamRepository.findByCode(request.probationTeamCode!!)
      ?: throw EntityNotFoundException("Probation team with code ${request.probationTeamCode} not found")

    val prisoner = request.prisoner().validate()

    return VideoBooking.newProbationBooking(
      probationTeam = probationTeam,
      probationMeetingType = request.probationMeetingType!!.name,
      comments = request.comments,
      createdBy = createdBy,
      notesForStaff = request.notesForStaff,
      notesForPrisoners = request.notesForPrisoners,
    )
      .also { thisBooking -> appointmentsService.createAppointmentForProbation(thisBooking, request.prisoner(), createdBy) }
      .also { thisBooking -> videoBookingRepository.saveAndFlush(thisBooking) }
      .also { thisBooking -> request.saveAdditionalDetailsFor(thisBooking) }
      .also { thisBooking -> bookingHistoryService.createBookingHistory(HistoryType.CREATE, thisBooking) }
      .also { thisBooking -> log.info("CREATE PROBATION BOOKING: booking ${thisBooking.videoBookingId} created") } to prisoner
  }

  private fun CreateVideoBookingRequest.saveAdditionalDetailsFor(booking: VideoBooking) {
    additionalBookingDetails?.let { details ->
      additionalBookingDetailRepository.saveAndFlush(
        AdditionalBookingDetail.newDetails(
          videoBooking = booking,
          contactName = details.contactName!!,
          contactEmail = details.contactEmail!!,
          contactPhoneNumber = details.contactNumber,
        ),
      )
    }
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
