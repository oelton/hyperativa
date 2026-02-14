package com.hyperativa.crud.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
public class Card extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String cardNumberHash;

    @Column(nullable = false)
    private String encryptedCardNumber;

    @Builder
    public Card(Long id, String cardNumberHash, String encryptedCardNumber) {
        this.id = id;
        this.cardNumberHash = cardNumberHash;
        this.encryptedCardNumber = encryptedCardNumber;
    }
}
