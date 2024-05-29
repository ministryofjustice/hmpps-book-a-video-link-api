package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StringExtensionUtilsTest {
  @Test
  fun `Should not recognise invalid email addresses`() {
    val listOfEmailAddresses = listOf(
      "test.test.com",
      "bob.co.uk",
      "fred12345@",
      "a76673443f",
      "1233@justice",
      "@@321.com",
      "12345",
      "hello",
    )

    listOfEmailAddresses.forEach { address ->
      assertThat(address.isEmail()).isEqualTo(false)
    }
  }

  @Test
  fun `Should recognise various legitimate email addresses`() {
    val listOfEmailAddresses = listOf(
      "test@test.com",
      "bill@bob.co.uk",
      "fred12345@cohort.uk.com",
      "a76673443f@justice.gov.uk",
      "aaabf@digital.justice.gov.uk",
      "first.second.third.namef@digital.justice.gov.uk",
      "first-hyphenated.name@justice.gov.uk",
      "name@hyphenated-domain.com",
    )

    listOfEmailAddresses.forEach { address ->
      assertThat(address.isEmail()).isEqualTo(true)
    }
  }
}
