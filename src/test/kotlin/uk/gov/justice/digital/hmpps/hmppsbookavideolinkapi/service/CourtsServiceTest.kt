package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court as CourtEntity

class CourtsServiceTest {
  private val courtRepository: CourtRepository = mock()
  private val service = CourtsService(courtRepository)
  private fun generateEntity(id: Long, code: String, desc: String, enabled: Boolean = true, notes: String? = "notes") =
    CourtEntity(id, code, desc, enabled, notes, "name")

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `Should return a list of enabled courts`() {
    val listOfEnabledCourts = listOf(
      generateEntity(1L, "COURT1", "One"),
      generateEntity(2L, "COURT2", "Two"),
      generateEntity(3L, "COURT3", "Three"),
    )

    whenever(courtRepository.findAllByEnabledIsTrue()).thenReturn(listOfEnabledCourts)

    assertThat(service.getEnabledCourts()).isEqualTo(
      listOfEnabledCourts.toModel(),
    )

    Mockito.verify(courtRepository).findAllByEnabledIsTrue()
  }

  @Test
  fun `Should return an empty list when no courts are enabled`() {
    whenever(courtRepository.findAllByEnabledIsTrue()).thenReturn(emptyList())
    assertThat(service.getEnabledCourts()).isEmpty()
    Mockito.verify(courtRepository).findAllByEnabledIsTrue()
  }
}
