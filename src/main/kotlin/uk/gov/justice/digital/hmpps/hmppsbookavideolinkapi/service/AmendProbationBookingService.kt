package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AdditionalBookingDetail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkCaseLoadAccess
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkVideoBookingAccess
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toPrisonerDetails
import java.time.LocalDateTime.now

@Service
class AmendProbationBookingService(
  private val videoBookingRepository: VideoBookingRepository,
  private val prisonRepository: PrisonRepository,
  private val appointmentsService: AppointmentsService,
  private val bookingHistoryService: BookingHistoryService,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun amend(videoBookingId: Long, request: AmendVideoBookingRequest, amendedBy: User): Pair<VideoBooking, Prisoner> {
    require(request.bookingType == BookingType.PROBATION) {
      "AMEND PROBATION BOOKING: booking type is not probation"
    }

    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found.") }
      .also { checkVideoBookingAccess(amendedBy, it) }
      .also { checkCaseLoadAccess(amendedBy, it.prisonCode()) }
      .also { require(BookingType.valueOf(it.bookingType.name) == request.bookingType) { "The booking type ${it.bookingType} does not match the requested type ${request.bookingType}." } }
      .also { require(it.statusCode != StatusCode.CANCELLED) { "Video booking $videoBookingId is already cancelled, and so cannot be amended" } }
      .also { require(it.appointments().all { a -> a.start().isAfter(now()) }) { "Video booking $videoBookingId has already started, and so cannot be amended" } }

    val prisoner = request.prisoner().validate()

    return booking.amendProbationBooking(
      probationMeetingType = request.probationMeetingType!!.name,
      notesForStaff = request.notesForStaff,
      notesForPrisoners = request.notesForPrisoners,
      amendedBy = amendedBy,
    )
      .also { thisBooking -> thisBooking.removeAllAppointments() }
      .also { thisBooking -> appointmentsService.createAppointmentForProbation(thisBooking, request.prisoner(), amendedBy) }
      .also { thisBooking -> videoBookingRepository.saveAndFlush(thisBooking) }
      .also { thisBooking -> request.saveAdditionalDetailsFor(thisBooking) }
      .also { thisBooking -> bookingHistoryService.createBookingHistory(HistoryType.AMEND, thisBooking) }
      .also { thisBooking -> log.info("AMEND PROBATION BOOKING: probation team booking ${thisBooking.videoBookingId} amended") } to prisoner
  }

  // We will only be creating appointments for one single prisoner as part of the initial rollout.
  private fun AmendVideoBookingRequest.prisoner() = prisoners.first()

  private fun PrisonerDetails.validate(): Prisoner {
    // We are not checking if the prison is enabled here as we need to support prison users also.
    // Our UI should not be sending disabled prisons though.
    prisonRepository.findByCode(prisonCode!!) ?: throw EntityNotFoundException("Prison with code $prisonCode not found")
    return prisonerSearchClient.getPrisoner(prisonerNumber!!)?.toPrisonerDetails() ?: throw ValidationException("Prisoner $prisonerNumber not found.")
  }

  private fun AmendVideoBookingRequest.saveAdditionalDetailsFor(booking: VideoBooking) {
    val mayBeExistingDetails = additionalBookingDetailRepository.findByVideoBooking(booking)

    if (additionalBookingDetails == null) {
      if (mayBeExistingDetails != null) {
        additionalBookingDetailRepository.delete(mayBeExistingDetails)
        additionalBookingDetailRepository.flush()
      }
    } else {
      if (mayBeExistingDetails != null) {
        additionalBookingDetailRepository.saveAndFlush(
          mayBeExistingDetails.apply {
            contactName = additionalBookingDetails.contactName!!
            contactEmail = additionalBookingDetails.contactEmail!!
            contactNumber = additionalBookingDetails.contactNumber
          },
        )
      } else {
        additionalBookingDetailRepository.saveAndFlush(
          AdditionalBookingDetail.newDetails(
            videoBooking = booking,
            contactName = additionalBookingDetails.contactName!!,
            contactEmail = additionalBookingDetails.contactEmail!!,
            contactPhoneNumber = additionalBookingDetails.contactNumber,
          ),
        )
      }
    }
  }
}
