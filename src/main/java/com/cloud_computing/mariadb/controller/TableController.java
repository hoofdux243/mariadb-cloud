package com.cloud_computing.mariadb.controller;

import com.cloud_computing.mariadb.dto.RowDTO;
import com.cloud_computing.mariadb.dto.TableDataDTO;
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

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> alterColumn(@PathVariable String tableName,
                                        @PathVariable Long dbId,
                                        @Valid @RequestBody TableAlterRequest dto) {
        tableService.alterColumn(dbId, tableName, dto);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_UPDATED.getMessage())
                .build());
    }

    @PatchMapping("/{tableName}/rename")
    public ResponseEntity<?> renameTable(@PathVariable Long dbId, @PathVariable String tableName, @RequestParam String newName) {
        tableService.renameTable(dbId, tableName, newName);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_UPDATED.getMessage())
                .build());
    }

    @DeleteMapping("/{tableName}")
    public ResponseEntity<?> deleteTable(@PathVariable Long dbId, @PathVariable String tableName) {
        tableService.dropTable(dbId, tableName);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(HttpStatus.NO_CONTENT.value())
                .message(APIResponseMessage.SUCCESSFULLY_DELETED.getMessage())
                .build());
    }

    @GetMapping
    public ResponseEntity<?> getTables(@PathVariable Long dbId) {
        return ResponseEntity.ok(APIResponse.<List<String>>builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.getMessage())
                .data(tableService.getTables(dbId))
                .build());
    }

    @GetMapping("/{tableName}/structure")
    public ResponseEntity<?> getTableStructure(
            @PathVariable Long dbId,
            @PathVariable String tableName) {
        Map<String, Object> structure = tableService.getTableStructure(dbId, tableName);
        return ResponseEntity.ok(APIResponse.<Map<String, Object>>builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.getMessage())
                .data(structure)
                .build());
    }

    @GetMapping("/{tableName}/data")
    public ResponseEntity<?> getTableData(@PathVariable Long dbId,
                                      @PathVariable String tableName,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(APIResponse.<TableDataDTO>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy dữ liệu bảng thành công")
                .data(tableService.getTableData(dbId, tableName, page, size))
                .build());
    }

    @PostMapping("/{tableName}/rows")
    public ResponseEntity<?> insertRow(
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @Valid @RequestBody RowDTO request) {
        tableService.insertRow(dbId, tableName, request);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(HttpStatus.CREATED.value())
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.getMessage())
                .build());
    }

    @PatchMapping("/{tableName}/rows")
    public ResponseEntity<?> updateRow(
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @Valid @RequestBody RowDTO request) {
        tableService.updateRow(dbId, tableName, request);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_UPDATED.getMessage())
                .build());
    }
    @DeleteMapping("/{tableName}/rows")
    public ResponseEntity<?> deleteRow(
            @PathVariable Long dbId,
            @PathVariable String tableName,
            @RequestBody RowDTO request) {
        tableService.deleteRow(dbId, tableName, request);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_DELETED.getMessage())
                .build());
    }
}
