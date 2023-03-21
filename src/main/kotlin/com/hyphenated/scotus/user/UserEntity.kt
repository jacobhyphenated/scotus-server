package com.hyphenated.scotus.user

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_entity")
data class UserEntity (

  @Id
  val id: String = UUID.randomUUID().toString(),

  @Column(unique = true)
  val username: String,

  @Column
  val password: String,

  @Column(name = "is_admin")
  val isAdmin: Boolean = false
)

class UserDetailsImpl(private val user: UserEntity): UserDetails {

  private val roles = mutableListOf(SimpleGrantedAuthority("ROLE_USER"))

  init {
    if (user.isAdmin) {
      roles.add(SimpleGrantedAuthority("ROLE_ADMIN"))
    }
  }

  override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
    return roles
  }

  override fun isEnabled(): Boolean {
    return true
  }

  override fun getUsername(): String {
    return user.username
  }

  override fun isCredentialsNonExpired(): Boolean {
    return true
  }

  override fun getPassword(): String {
    return user.password
  }

  override fun isAccountNonExpired(): Boolean {
    return true
  }

  override fun isAccountNonLocked(): Boolean {
    return true
  }

}

fun UserEntity.toUserDetails(): UserDetails = UserDetailsImpl(this)