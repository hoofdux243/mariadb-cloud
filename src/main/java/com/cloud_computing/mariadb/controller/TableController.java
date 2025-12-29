package com.cloud_computing.mariadb.controller;

import com.cloud_computing.mariadb.dto.request.TableAlterRequest;
import com.cloud_computing.mariadb.dto.request.TableCreateRequest;
import com.cloud_computing.mariadb.dto.response.APIResponse;
import com.cloud_computing.mariadb.dto.response.APIResponseMessage;
import com.cloud_computing.mariadb.service.TableService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dbs/{dbId}/tables")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TableController {
    final TableService tableService;

    @PostMapping
    public ResponseEntity<?> createTable(@PathVariable Long dbId, @Valid @RequestBody TableCreateRequest dto) {
        tableService.createTable(dbId, dto);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(HttpStatus.CREATED.value())
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.getMessage())
                .build());
    }

    @PatchMapping("/{tableName}")
    public ResponseEntity<?> alterTable(@PathVariable String tableName,
                                        @PathVariable Long dbId,
                                        @Valid @RequestBody TableAlterRequest dto) {
        tableService.alterTable(dbId, tableName, dto);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_UPDATED.getMessage())
                .build());
    }
}
