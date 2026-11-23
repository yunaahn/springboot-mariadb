package com.example.app.character;

import com.example.app.character.CharacterDtos.ActiveResponse;
import com.example.app.character.CharacterDtos.CharacterResponse;
import com.example.app.character.CharacterDtos.CreateRequest;
import com.example.app.character.CharacterDtos.StreakResponse;
import com.example.app.common.ApiException;
import com.example.app.intake.IntakePlanner;
import com.example.app.reward.StreakService;
import com.example.app.user.User;
import com.example.app.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CharacterService {

    private final UserRepository users;
    private final GameCharacterRepository characters;
    private final UserStatsRepository stats;
    private final StreakService streaks;
    private final IntakePlanner planner;

    @Transactional(readOnly = true)
    public ActiveResponse active(Long userId) {
        User user = users.findByIdAndDeletedAtIsNull(userId).orElseThrow(ApiException::unauthorized);
        int current = streaks.run(userId, planner.today(user)).length();
        int best = stats.findById(userId).map(UserStats::getBestStreak).orElse(0);
        return new ActiveResponse(
                characters.findByUserIdAndActiveMarker(userId, 1).map(CharacterResponse::of).orElse(null),
                new StreakResponse(current, Math.max(best, current)));
    }

    public CharacterResponse create(Long userId, CreateRequest request) {
        users.findForUpdate(userId).orElseThrow(ApiException::unauthorized);
        if (characters.findByUserIdAndActiveMarker(userId, 1).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_PROCESSED", "이미 키우고 있는 캐릭터가 있어요.");
        }
        return CharacterResponse.of(characters.save(
                new GameCharacter(userId, request.speciesCode().name(), request.name().strip())));
    }
}
