package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode as ReferenceCodeEntity

class ReferenceCodeServiceTest {
  private val referenceCodeRepository: ReferenceCodeRepository = mock()
  private val service = ReferenceCodeService(referenceCodeRepository)
  private fun referenceCodeEntity(id: Long, groupCode: String, code: String, description: String) =
    ReferenceCodeEntity(id, groupCode, code, description, "name")

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `Should return a list of references codes for court hearing type`() {
    val groupCode = "COURT_HEARING_TYPE"
    val listOfCodes = listOf(
      referenceCodeEntity(1L, groupCode, "SENTENCE", "Sentence hearing"),
      referenceCodeEntity(2L, groupCode, "APPEAL", "Appeal"),
      referenceCodeEntity(3L, groupCode, "COMMITTAL", "Committal"),
    )

    whenever(referenceCodeRepository.findAllByGroupCodeEquals(groupCode)).thenReturn(listOfCodes)

    assertThat(service.getReferenceDataByGroup(groupCode)).isEqualTo(
      listOfCodes.toModel(),
    )

    verify(referenceCodeRepository).findAllByGroupCodeEquals(groupCode)
  }

  @Test
  fun `Should return an empty list when no reference codes are matched`() {
    val groupCode = "PROBATION_MEETING_TYPE"
    whenever(referenceCodeRepository.findAllByGroupCodeEquals(groupCode)).thenReturn(emptyList())
    assertThat(service.getReferenceDataByGroup(groupCode)).isEmpty()
    verify(referenceCodeRepository).findAllByGroupCodeEquals(groupCode)
  }
}
