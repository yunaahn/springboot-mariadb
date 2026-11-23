package com.example.app.reward;

import com.example.app.common.ApiException;
import com.example.app.common.CurrentUserId;
import com.example.app.common.Cursor;
import com.example.app.common.PageResponse;
import com.example.app.reward.RewardDtos.RewardResponse;
import com.example.app.user.UserRepository;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardLedgerRepository ledger;
    private final UserRepository users;

    @GetMapping
    PageResponse<RewardResponse> list(@CurrentUserId Long userId, @RequestParam(required = false) String cursor,
                                      @RequestParam(defaultValue = "20") int size) {
        if (size < 1 || size > 100) {
            throw ApiException.badRequest("size는 1~100이어야 해요.");
        }
        ZoneId zone = ZoneId.of(users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(ApiException::unauthorized).getTimezone());
        return Cursor.page(ledger.findByUserIdAndIdLessThanOrderByIdDesc(userId,
                        Cursor.decode(cursor, Long.MAX_VALUE), Limit.of(size + 1)),
                size, RewardLedger::getId, r -> RewardResponse.of(r, zone));
    }
}
