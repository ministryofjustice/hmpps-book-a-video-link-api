package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class LocationAttributesAvailableService(
  private val locationAttributeRepository: LocationAttributeRepository,
  private val courtRepository: CourtRepository,
  private val probationTeamRepository: ProbationTeamRepository,
) {
  fun isLocationAvailableFor(request: LocationAvailableRequest): AvailabilityStatus {
    val attribute = locationAttributeRepository.findById(request.attributeId)
      .orElseThrow { EntityNotFoundException("Location attribute ${request.attributeId} not found") }

    return when (request.type) {
      LocationAvailableRequest.Type.COURT -> {
        attribute.isAvailableFor(
          courtRepository.findByCode(request.code) ?: throw EntityNotFoundException("Court code ${request.code} not found"),
          request.onDate,
          request.startTime,
          request.endTime,
        )
      }
      LocationAvailableRequest.Type.PROBATION -> {
        attribute.isAvailableFor(
          probationTeamRepository.findByCode(request.code) ?: throw EntityNotFoundException("Probation team ${request.code} not found"),
          request.onDate,
          request.startTime,
          request.endTime,
        )
      }
    }
  }
}

class LocationAvailableRequest private constructor(
  val attributeId: Long,
  val type: Type,
  val code: String,
  val onDate: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
) {
  enum class Type {
    COURT,
    PROBATION,
  }

  companion object {
    fun court(attributeId: Long, courtCode: String, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime) = LocationAvailableRequest(attributeId, Type.COURT, courtCode, onDate, startTime, endTime)

    fun probation(attributeId: Long, probationTeamCode: String, onDate: LocalDate, startTime: LocalTime, endTime: LocalTime) = LocationAvailableRequest(attributeId, Type.PROBATION, probationTeamCode, onDate, startTime, endTime)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LocationAvailableRequest

    if (attributeId != other.attributeId) return false
    if (type != other.type) return false
    if (code != other.code) return false
    if (onDate != other.onDate) return false
    if (startTime != other.startTime) return false
    if (endTime != other.endTime) return false

    return true
  }

  override fun hashCode() = attributeId.hashCode()
}
