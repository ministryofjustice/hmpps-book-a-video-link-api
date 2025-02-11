package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.time.OffsetDateTime
import java.util.Locale

private val ukCountryCode: String = Locale.UK.country
private val phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

fun String.isEmail() = this.matches(
  Regex("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}\\@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"),
)

fun String.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.parse(this)

fun String.isUkPhoneNumber(): Boolean = runCatching {
  phoneNumberUtil.parse(this, ukCountryCode).let {
    // We don't want to allow alpha numbers e.g. 0800 800 ACME
    !phoneNumberUtil.isAlphaNumber(this) &&
      phoneNumberUtil.isValidNumber(it)
  }
}.getOrElse { false }
