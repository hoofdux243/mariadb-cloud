package com.cloud_computing.mariadb.controller;

import com.cloud_computing.mariadb.dto.response.APIResponse;
import com.cloud_computing.mariadb.dto.response.APIResponseMessage;
import com.cloud_computing.mariadb.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/logs")
//@RequiredArgsConstructor
public class AuditLogController {
//    private final AuditLogService auditLogService;

//    @GetMapping("/{dbId}")
//    public ResponseEntity<?> getDbLogs(
//            @PathVariable Long dbId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size){
//        APIResponse apiResponse = APIResponse.builder()
//                .code(HttpStatus.OK.value())
//                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.getMessage())
//                .data(auditLogService.getDbLogs(dbId, page, size))
//                .build();
//        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
//    }
}
