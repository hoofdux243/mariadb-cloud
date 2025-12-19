package com.cloud_computing.mariadb.controller;

import com.cloud_computing.mariadb.dto.DbDTO;
import com.cloud_computing.mariadb.dto.response.APIResponse;
import com.cloud_computing.mariadb.dto.response.APIResponseMessage;
import com.cloud_computing.mariadb.entity.Db;
import com.cloud_computing.mariadb.service.DbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dbs")
public class DbController {
    @Autowired
    DbService dbService;

    @PostMapping
    ResponseEntity<?> createDb(@RequestBody DbDTO dbDTO) {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.CREATED.value())
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.getMessage())
                .data(dbService.createDb(dbDTO))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }
}
