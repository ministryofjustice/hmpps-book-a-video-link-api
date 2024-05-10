package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

@Service
class CreateVideoBookingService(
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
  private val videoBookingRepository: VideoBookingRepository,
  private val emailService: EmailService,
) {
  fun create(request: CreateVideoBookingRequest): Long {
    TODO("Creation of video link booking to be implemented")
  }
}
