package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoBookingSearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.findByCourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.findByProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkCaseLoadAccess
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkVideoBookingAccess
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
@Transactional(readOnly = true)
class VideoLinkBookingsService(
  private val videoBookingRepository: VideoBookingRepository,
  private val referenceCodeRepository: ReferenceCodeRepository,
  private val videoAppointmentRepository: VideoAppointmentRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val prisonApiClient: PrisonApiClient,
  private val nomisMappingClient: NomisMappingClient,
) {
  fun getVideoLinkBookingById(videoBookingId: Long, user: User): VideoLinkBooking {
    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }
      .also { checkVideoBookingAccess(user, it) }
      .also { checkCaseLoadAccess(user, it.prisonCode()) }

    val hearingType = booking
      .takeIf(VideoBooking::isCourtBooking)
      ?.let { referenceCodeRepository.findByCourtHearingType(booking.hearingType!!) }

    val meetingType = booking
      .takeIf(VideoBooking::isProbationBooking)
      ?.let { referenceCodeRepository.findByProbationMeetingType(booking.probationMeetingType!!) }

    val locations = booking.appointments().mapNotNull { locationsInsidePrisonClient.getLocationById(it.prisonLocationId) }.toSet()

    return booking.toModel(
      locations = locations,
      courtHearingTypeDescription = hearingType?.description,
      probationMeetingTypeDescription = meetingType?.description,
    )
  }

  fun findMatchingVideoLinkBooking(searchRequest: VideoBookingSearchRequest, user: User): VideoLinkBooking {
    val locationId = locationsInsidePrisonClient.getLocationByKey(searchRequest.locationKey!!)?.id
      ?: prisonApiClient.getInternalLocationByKey(searchRequest.locationKey)?.let { nomisMappingClient.getNomisLocationMappingBy(it.locationId)?.dpsLocationId }
      ?: throw EntityNotFoundException("Location with key ${searchRequest.locationKey} not found")

    return videoAppointmentRepository.findActiveVideoAppointment(
      prisonerNumber = searchRequest.prisonerNumber!!,
      appointmentDate = searchRequest.date!!,
      prisonLocationId = locationId,
      startTime = searchRequest.startTime!!,
      endTime = searchRequest.endTime!!,
    )
      ?.let { getVideoLinkBookingById(it.videoBookingId, user) }
      ?: throw EntityNotFoundException("Video booking not found matching search criteria $searchRequest")
  }
}
