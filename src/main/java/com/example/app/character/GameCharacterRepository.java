package com.example.app.character;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {

    Optional<GameCharacter> findByUserIdAndActiveMarker(Long userId, Integer activeMarker);
}
