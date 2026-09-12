package com.lunchorder.web

import com.lunchorder.service.AuthService
import com.lunchorder.web.dto.LoginRequest
import com.lunchorder.web.dto.LoginResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@RequestBody req: LoginRequest): LoginResponse = authService.login(req.loginName, req.password)
}
