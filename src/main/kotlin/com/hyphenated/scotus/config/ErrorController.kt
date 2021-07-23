package com.hyphenated.scotus.config

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.ServletWebRequest
import java.util.*
import javax.servlet.http.HttpServletRequest


@RestController
@RequestMapping("/error")
class ErrorController(private val errorAttributes: ErrorAttributes,
                      private val env: Environment): ErrorController {

  @RequestMapping
  fun error(request: HttpServletRequest): ErrorResponse{
    val err = getErrorAttributes(request, isLocal())
    return ErrorResponse(
        err["error"]?.toString()?.uppercase(Locale.getDefault()) ?: "UNKNOWN_ERROR",
        err["message"]?.toString(),
        err["trace"]?.toString()
    )
  }

  private fun isLocal(): Boolean {
    return env.activeProfiles.contains("local") || env.activeProfiles.contains(("dev"))
  }

  private fun getErrorAttributes(request: HttpServletRequest, includeStackTrace: Boolean): Map<String, Any?> {
    val webRequest = ServletWebRequest(request)
    return errorAttributes.getErrorAttributes(webRequest,
        if (includeStackTrace) ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE)
        else ErrorAttributeOptions.defaults())
  }

}