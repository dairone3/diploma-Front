package com.example.dimadyplom2.api

import kotlinx.serialization.Serializable

/** Response body for the auth endpoints: `{ "token": "<jwt>", "id": <user_id> }`. */
@Serializable
data class UserResponse(val token: String, val user: UserDto)

@Serializable
data class UserDto(
    val id: Int,
    val login: String
)

