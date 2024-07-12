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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getClientAsUser
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
class BvlsRequestContextInterceptor(private val userService: UserService) : HandlerInterceptor {

  companion object {
    private val log = LoggerFactory.getLogger(BvlsRequestContextInterceptor::class.java)
  }

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    val username = authentication().userName
    val clientId = authentication().clientId

    if (username != null) {
      request.setAttribute(BvlsRequestContext::class.simpleName, BvlsRequestContext(user = userService.getUser(username) ?: throw IllegalArgumentException("User with username $username not found")))
    } else {
      // The clientId is non-nullable, otherwise the request would not be authenticated!
      request.setAttribute(BvlsRequestContext::class.simpleName, BvlsRequestContext(user = getClientAsUser(clientId)))
    }

    return true
  }

  private fun authentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw AccessDeniedException("User is not authenticated")
}

data class BvlsRequestContext(val user: User, val requestAt: LocalDateTime = LocalDateTime.now())

fun HttpServletRequest.getBvlsRequestContext() = getAttribute(BvlsRequestContext::class.simpleName) as BvlsRequestContext
