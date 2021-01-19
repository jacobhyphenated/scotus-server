package com.hyphenated.scotus.user

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UserController::class)
@AutoConfigureRestDocs
class UserControllerTests {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var service: UserService

  @Test
  fun testGetUser() {
    val response = UserInfo("testuser", listOf("user", "test"))
    whenever(service.getUserDetails()).thenReturn(response)

    this.mockMvc.perform(get("/user"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("username").value("testuser"))
      .andExpect(jsonPath("roles").value(hasSize<Any>(2)))
      .andExpect(jsonPath("roles[0]").value("user"))
  }

  @Test
  fun testCreateUser() {
    val request = "{\"username\": \"testuser\", \"password\": \"password\"}"
    val response = UserInfo("testuser", listOf("user"))
    whenever(service.createUser(any())).thenReturn(response)

    this.mockMvc.perform(post("/user")
      .contentType(MediaType.APPLICATION_JSON)
      .content(request))
      .andExpect(status().isOk)
      .andExpect(jsonPath("username").value("testuser"))

    val argumentCaptor = argumentCaptor<CreateUserRequest>()
    verify(service).createUser(argumentCaptor.capture())
    assertThat(argumentCaptor.firstValue.password).isEqualTo("password")
    assertThat(argumentCaptor.firstValue.username).isEqualTo("testuser")
  }
}