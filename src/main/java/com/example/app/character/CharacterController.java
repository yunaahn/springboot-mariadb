package com.example.app.character;

import com.example.app.common.ApiResponse;
import com.example.app.common.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService service;

    @GetMapping("/active")
    ApiResponse<CharacterDtos.ActiveResponse> active(@CurrentUserId Long userId) {
        return new ApiResponse<>(service.active(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CharacterDtos.CharacterResponse> create(@CurrentUserId Long userId,
                                                        @Valid @RequestBody CharacterDtos.CreateRequest request) {
        return new ApiResponse<>(service.create(userId, request));
    }
}
