package com.example.app.character;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CharacterDtos {

    private CharacterDtos() {
    }

    public record CreateRequest(@NotNull Species speciesCode, @NotBlank @Size(max = 30) String name) {
    }

    public record CharacterResponse(Long id, String speciesCode, String name, int level, int exp,
                                    int nextLevelExp, int stage) {
        static CharacterResponse of(GameCharacter c) {
            return new CharacterResponse(c.getId(), c.getSpeciesCode(), c.getName(), c.getLevel(), c.getExp(),
                    c.nextLevelExp(), c.getStage());
        }
    }

    public record StreakResponse(int current, int best) {
    }

    /** 진행 중인 캐릭터가 없으면 character는 null. */
    public record ActiveResponse(CharacterResponse character, StreakResponse streak) {
    }
}
