package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "file_imports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String fileType; // CSV, PDF, IMAGE

    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "fileImport", cascade = CascadeType.ALL)
    private List<Transaction> transactions;
}
