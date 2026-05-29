package com.Life_ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Life_ledger.entity.FileImport;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FileImportRepository extends JpaRepository<FileImport, Long> {
    Optional<FileImport> findByFileId(String fileId);
    void deleteByUser_Id(Long userId);
    @Modifying
    @Query("delete from FileImport f where f.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

}
