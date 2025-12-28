package com.cloud_computing.mariadb.controller;

import com.cloud_computing.mariadb.dto.response.APIResponse;
import com.cloud_computing.mariadb.dto.response.APIResponseMessage;
import com.cloud_computing.mariadb.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dbs/{dbId}/backups")
public class BackupController {
    private final BackupService backupService;
    @PostMapping
    public ResponseEntity<?> createBackup(@PathVariable Long dbId, @RequestParam(required = false) String description) {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.CREATED.value())
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.getMessage())
                .data(backupService.createBackup(dbId, description))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> getBackups(@PathVariable Long dbId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.getMessage())
                .data(backupService.getBackups(dbId, page, size))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/{backupId}/download")
    public ResponseEntity<?> downloadBackup(@PathVariable Long backupId) {
        Resource resource = backupService.downloadBackup(backupId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + "backup" + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{backupId}")
    public ResponseEntity<?> deleteBackup(@PathVariable Long backupId) {
        backupService.deleteBackup(backupId);
        return new ResponseEntity<>(APIResponse.builder()
                .code(HttpStatus.NO_CONTENT.value())
                .message(APIResponseMessage.SUCCESSFULLY_DELETED.getMessage())
                .build(),HttpStatus.NO_CONTENT);
    }
}
