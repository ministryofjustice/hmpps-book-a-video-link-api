package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.integrations

import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VLB_COURT_MAIN
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VLB_PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PrisonerSchedulesService(private val prisonAppointmentRepository: PrisonAppointmentRepository) {
  fun findCourtBookingIds(
    fromDate: LocalDate,
    toDate: LocalDate,
    prisonCode: String?,
    prisonerNumber: String?,
    locationId: UUID?,
    pageable: Pageable,
  ) = run {
    findBookingIds(
      appointmentType = VLB_COURT_MAIN,
      fromDate = fromDate,
      toDate = toDate,
      prisonCode = prisonCode.sanitised(),
      prisonerNumber = prisonerNumber.sanitised(),
      locationId = locationId,
      pageable = pageable,
    )
  }

  fun findProbationBookingIds(
    fromDate: LocalDate,
    toDate: LocalDate,
    prisonCode: String?,
    prisonerNumber: String?,
    locationId: UUID?,
    pageable: Pageable,
  ) = run {
    findBookingIds(
      appointmentType = VLB_PROBATION,
      fromDate = fromDate,
      toDate = toDate,
      prisonCode = prisonCode.sanitised(),
      prisonerNumber = prisonerNumber.sanitised(),
      locationId = locationId,
      pageable = pageable,
    )
  }

  private fun findBookingIds(
    appointmentType: String,
    fromDate: LocalDate,
    toDate: LocalDate,
    prisonCode: String?,
    prisonerNumber: String?,
    locationId: UUID?,
    pageable: Pageable,
  ) = run {
    require(toDate >= fromDate) { "To date must be on or after the from date" }

    prisonAppointmentRepository.findBookingIdsPaginated(
      appointmentType = appointmentType,
      fromDate = fromDate,
      toDate = toDate,
      prisonCode = prisonCode.sanitised(),
      prisonerNumber = prisonerNumber.sanitised(),
      locationId = locationId,
      pageable = pageable,
    ).let(::PagedModel)
  }

  private fun String?.sanitised() = this?.trim()?.uppercase()?.ifBlank { null }
}
