package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode

@Repository
interface ReferenceCodeRepository : JpaRepository<ReferenceCode, Long> {
  fun findAllByGroupCodeEquals(groupCode: String): List<ReferenceCode>
  fun findByGroupCodeAndCode(groupCode: String, code: String): ReferenceCode?
}

fun ReferenceCodeRepository.findByCourtHearingType(hearingType: String) = findByGroupCodeAndCode("COURT_HEARING_TYPE", hearingType)

fun ReferenceCodeRepository.findByProbationMeetingType(meetingType: String) = findByGroupCodeAndCode("PROBATION_MEETING_TYPE", meetingType)
