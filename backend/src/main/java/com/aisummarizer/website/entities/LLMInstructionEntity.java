package com.aisummarizer.website.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "llm_instructions")
public class LLMInstructionEntity {

    @Column
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private Long ownerId;
}
