package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import java.time.LocalDateTime

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
          request.onDateTime,
        )
      }
      LocationAvailableRequest.Type.PROBATION -> {
        attribute.isAvailableFor(
          probationTeamRepository.findByCode(request.code) ?: throw EntityNotFoundException("Probation team ${request.code} not found"),
          request.onDateTime,
        )
      }
    }
  }
}

class LocationAvailableRequest private constructor(
  val attributeId: Long,
  val type: Type,
  val code: String,
  val onDateTime: LocalDateTime,
) {
  enum class Type {
    COURT,
    PROBATION,
  }

  companion object {
    fun court(attributeId: Long, courtCode: String, onDateTime: LocalDateTime) = LocationAvailableRequest(attributeId, Type.COURT, courtCode, onDateTime)

    fun probation(attributeId: Long, probationTeamCode: String, onDateTime: LocalDateTime) = LocationAvailableRequest(attributeId, Type.PROBATION, probationTeamCode, onDateTime)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LocationAvailableRequest

    if (attributeId != other.attributeId) return false
    if (type != other.type) return false
    if (code != other.code) return false
    if (onDateTime != other.onDateTime) return false

    return true
  }

  override fun hashCode() = attributeId.hashCode()
}
