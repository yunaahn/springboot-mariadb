package com.example.app.intake;

import com.example.app.common.ApiResponse;
import com.example.app.common.CurrentUserId;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/intakes")
@RequiredArgsConstructor
public class IntakeController {

    private final IntakeService service;

    @GetMapping
    ApiResponse<IntakeDtos.DayResponse> day(@CurrentUserId Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new ApiResponse<>(service.day(userId, date));
    }

    @PostMapping("/take")
    ApiResponse<IntakeDtos.TakeResponse> take(@CurrentUserId Long userId,
                                              @Valid @RequestBody IntakeDtos.TakeRequest request) {
        return new ApiResponse<>(service.take(userId, request.intakeIds()));
    }

    @PostMapping("/{id}/cancel")
    ApiResponse<IntakeDtos.StatusResponse> cancel(@CurrentUserId Long userId, @PathVariable Long id) {
        return new ApiResponse<>(service.cancel(userId, id));
    }

    @PostMapping("/{id}/skip")
    ApiResponse<IntakeDtos.StatusResponse> skip(@CurrentUserId Long userId, @PathVariable Long id) {
        return new ApiResponse<>(service.skip(userId, id));
    }
}
