package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.sar

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.TestConfiguration
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import javax.sql.DataSource

@Import(TestConfiguration::class, SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestIntegrationTest :
  IntegrationTestBase(),
  SarFlywaySchemaTest,
  SarJpaEntitiesTest,
  SarApiDataTest,
  SarReportTest {

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  override fun getDataSourceInstance(): DataSource = dataSource

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getEntityManagerInstance(): EntityManager = entityManager

  override fun setupTestData() {}

  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  override fun getPrn(): String? = "A4567AZ"

  @Test
  @Sql("classpath:test_data/clean-all-data.sql", "classpath:sar/seed-sar-data.sql")
  override fun `SAR API should return expected data`() {
    super.`SAR API should return expected data`()
  }

  @Test
  @Sql("classpath:test_data/clean-all-data.sql", "classpath:sar/seed-sar-data.sql")
  override fun `SAR report should render as expected`() {
    super.`SAR report should render as expected`()
  }
}
