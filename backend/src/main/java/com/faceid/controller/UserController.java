package com.faceid.controller;

import com.faceid.dto.RegisterRequest;
import com.faceid.dto.RegisterResponse;
import com.faceid.dto.UserDto;
import com.faceid.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;


    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/users/register — name={}", request.getName());
        RegisterResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<List<UserDto>> listUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        log.info("DELETE /api/users/{}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
