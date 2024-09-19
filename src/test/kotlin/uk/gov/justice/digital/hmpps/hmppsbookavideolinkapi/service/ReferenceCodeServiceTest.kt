package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode as ReferenceCodeEntity

class ReferenceCodeServiceTest {
  private val ref1 = referenceCodeEntity(1L, "GROUP_CODE", "SENTENCE", "Sentence hearing")
  private val ref2 = referenceCodeEntity(2L, "GROUP_CODE", "APPEAL", "Appeal")
  private val ref3 = referenceCodeEntity(3L, "GROUP_CODE", "COMMITTAL", "Committal")
  private val ref4 = referenceCodeEntity(4L, "GROUP_CODE", "DISABLED", "Disabled", false)
  private val referenceCodeRepository: ReferenceCodeRepository = mock()
  private val service = ReferenceCodeService(referenceCodeRepository)

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `Should return a list of enabled only references codes for a given group code`() {
    whenever(referenceCodeRepository.findAllByGroupCodeEquals("GROUP_CODE")).thenReturn(listOf(ref1, ref2, ref3, ref4))

    service.getReferenceDataByGroup("GROUP_CODE", true) containsExactlyInAnyOrder listOf(ref1, ref2, ref3).toModel()

    verify(referenceCodeRepository).findAllByGroupCodeEquals("GROUP_CODE")
  }

  @Test
  fun `Should return a list of enabled and disabled references codes for a given group code`() {
    whenever(referenceCodeRepository.findAllByGroupCodeEquals("GROUP_CODE")).thenReturn(listOf(ref1, ref2, ref3, ref4))

    service.getReferenceDataByGroup("GROUP_CODE", false) containsExactlyInAnyOrder listOf(ref1, ref2, ref3, ref4).toModel()

    verify(referenceCodeRepository).findAllByGroupCodeEquals("GROUP_CODE")
  }

  private fun referenceCodeEntity(id: Long, groupCode: String, code: String, description: String, enabled: Boolean = true) =
    ReferenceCodeEntity(id, groupCode, code, description, "name", enabled = enabled)

  @Test
  fun `Should return an empty list when no reference codes are matched`() {
    val groupCode = "PROBATION_MEETING_TYPE"
    whenever(referenceCodeRepository.findAllByGroupCodeEquals(groupCode)).thenReturn(emptyList())
    assertThat(service.getReferenceDataByGroup(groupCode, true)).isEmpty()
    verify(referenceCodeRepository).findAllByGroupCodeEquals(groupCode)
  }
}
