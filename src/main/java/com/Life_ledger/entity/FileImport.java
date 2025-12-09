package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data

@Entity
@Table(name = "file_imports")
public class FileImport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fileId;
    private String filename;
    private String filetype;
    private Instant uploadAt;

    @ManyToOne
    private User user;
}
