package com.hyperativa.crud.domain.repository;

import com.hyperativa.crud.domain.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByCardNumberHash(String cardNumberHash);
}
