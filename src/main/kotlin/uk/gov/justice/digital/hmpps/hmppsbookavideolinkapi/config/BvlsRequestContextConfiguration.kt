package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import java.time.LocalDateTime

@Configuration
class BvlsRequestContextConfiguration(private val bvlsRequestContextInterceptor: BvlsRequestContextInterceptor) : WebMvcConfigurer {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun addInterceptors(registry: InterceptorRegistry) {
    log.info("Adding BVLS user request interceptor")

    registry.addInterceptor(bvlsRequestContextInterceptor)
      .addPathPatterns(
        "/courts/**",
        "/probation-teams/**",
        "/video-link-booking/**",
      )
  }
}

@Configuration
class BvlsRequestContextInterceptor : HandlerInterceptor {

  companion object {
    private val log = LoggerFactory.getLogger(BvlsRequestContextInterceptor::class.java)
  }

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    val username = getUsernameFromClaim()

    if (username != null) {
      request.setAttribute(BvlsRequestContext::class.simpleName, BvlsRequestContext(username = username))
    } else {
      log.info("Unable to determine user from the request.")
    }

    return true
  }

  private fun getUsernameFromClaim(): String? =
    authentication().let {
      it.tokenAttributes["user_name"] as String?
        ?: it.tokenAttributes["username"] as String?
        ?: it.tokenAttributes["client_id"] as String?
    }

  private fun authentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw AccessDeniedException("User is not authenticated")
}

data class BvlsRequestContext(val username: String, val requestAt: LocalDateTime = LocalDateTime.now())

fun HttpServletRequest.getBvlsRequestContext() = getAttribute(BvlsRequestContext::class.simpleName) as BvlsRequestContext
