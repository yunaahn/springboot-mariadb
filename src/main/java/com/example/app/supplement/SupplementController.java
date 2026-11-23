package com.example.app.supplement;

import com.example.app.common.ApiResponse;
import com.example.app.common.CurrentUserId;
import com.example.app.supplement.SupplementDtos.CreateRequest;
import com.example.app.supplement.SupplementDtos.CreateResponse;
import com.example.app.supplement.SupplementDtos.StatusRequest;
import com.example.app.supplement.SupplementDtos.SupplementResponse;
import com.example.app.supplement.SupplementDtos.UpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/supplements")
@RequiredArgsConstructor
public class SupplementController {

    private final SupplementService service;

    @GetMapping
    ApiResponse<List<SupplementResponse>> list(@CurrentUserId Long userId,
                                               @RequestParam(required = false) SupplementStatus status) {
        return new ApiResponse<>(service.list(userId, status));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CreateResponse> create(@CurrentUserId Long userId, @Valid @RequestBody CreateRequest request) {
        return new ApiResponse<>(service.create(userId, request));
    }

    @GetMapping("/{id}")
    ApiResponse<SupplementResponse> get(@CurrentUserId Long userId, @PathVariable Long id) {
        return new ApiResponse<>(service.get(userId, id));
    }

    @PatchMapping("/{id}")
    ApiResponse<SupplementResponse> update(@CurrentUserId Long userId, @PathVariable Long id,
                                           @Valid @RequestBody UpdateRequest request) {
        return new ApiResponse<>(service.update(userId, id, request));
    }

    @PatchMapping("/{id}/status")
    ApiResponse<SupplementResponse> changeStatus(@CurrentUserId Long userId, @PathVariable Long id,
                                                 @Valid @RequestBody StatusRequest request) {
        return new ApiResponse<>(service.changeStatus(userId, id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        service.delete(userId, id);
    }
}
