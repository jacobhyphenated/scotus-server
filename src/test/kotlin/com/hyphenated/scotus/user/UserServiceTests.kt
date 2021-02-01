package com.hyphenated.scotus.user

import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import org.assertj.core.api.Assertions.*

@ExtendWith(MockitoExtension::class)
class UserServiceTests {

  @Mock
  private lateinit var userRepo: UserRepo

  @Mock
  private lateinit var passwordEncoder: PasswordEncoder

  @InjectMocks
  private lateinit var userService: UserService

  @Test
  fun testCreateUser_alreadyExists() {
    val mockExistingUser = UserEntity("100", "testuser", "", false)
    whenever(userRepo.findOneByUsername("testuser")).thenReturn(mockExistingUser)

    val request = CreateUserRequest("testuser", "testpassword")
    assertThrows<UsernameNotAvailable> { userService.createUser(request) }
  }

  @Test
  fun testCreateUser() {
    val mockUser = UserEntity("100", "testuser", "encodedPass", false)
    whenever(userRepo.findOneByUsername("testuser")).thenReturn(null)

    // save() signature of S extends T requires an explicit type for mocks
    whenever(userRepo.save<UserEntity>(any())).thenReturn(mockUser)

    whenever(passwordEncoder.encode("testpassword")).thenReturn("encodedPass")
    val request = CreateUserRequest("testuser", "testpassword")

    val response = userService.createUser(request)
    assertThat(response.username).isEqualTo("testuser")
    assertThat(response.roles)
      .contains("ROLE_USER")
      .doesNotContain("ROLE_ADMIN")
      .hasSize(1)

    val argCaptor = argumentCaptor<UserEntity>()
    verify(userRepo).save(argCaptor.capture())
    assertThat(argCaptor.firstValue.username).isEqualTo("testuser")
    assertThat(argCaptor.firstValue.password).isEqualTo("encodedPass")
  }
}
