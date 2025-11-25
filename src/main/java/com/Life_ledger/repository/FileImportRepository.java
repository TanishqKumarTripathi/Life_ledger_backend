package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.FileImport;

import java.util.Optional;

public interface FileImportRepository extends JpaRepository<FileImport, Long> {
    Optional<FileImport> findByFileId(String fileId);
}
