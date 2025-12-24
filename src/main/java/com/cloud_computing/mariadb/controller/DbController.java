package com.cloud_computing.mariadb.controller;

import com.cloud_computing.mariadb.dto.DbDTO;
import com.cloud_computing.mariadb.dto.DbMemberDTO;
import com.cloud_computing.mariadb.dto.response.APIResponse;
import com.cloud_computing.mariadb.dto.response.APIResponseMessage;
import com.cloud_computing.mariadb.dto.response.AuthResponse;
import com.cloud_computing.mariadb.entity.Db;
import com.cloud_computing.mariadb.service.DbMemberService;
import com.cloud_computing.mariadb.service.DbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dbs")
public class DbController {
    @Autowired
    DbService dbService;
    @Autowired
    DbMemberService dbMemberService;

    @PostMapping
    ResponseEntity<?> createDb(@RequestBody DbDTO dbDTO) {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.CREATED.value())
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.getMessage())
                .data(dbService.createDb(dbDTO))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping
    ResponseEntity<?> findAllDbs() {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.getMessage())
                .data(dbService.getDbs())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/{dbId}")
    ResponseEntity<?> findDbById(@PathVariable Long dbId) {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.getMessage())
                .data(dbService.getDb(dbId))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @DeleteMapping("/{dbId}")
    ResponseEntity<?> deleteDbById(@PathVariable Long dbId) {
        dbService.deleteDb(dbId);
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.NO_CONTENT.value())
                .message(APIResponseMessage.SUCCESSFULLY_DELETED.getMessage())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.NO_CONTENT);    }

    @GetMapping("/{dbId}/members")
    ResponseEntity<?> findAllMembersByDbId(@PathVariable Long dbId) {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.getMessage())
                .data(dbMemberService.getDbMembersByProjectId(dbId))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/{dbId}/invite")
    public ResponseEntity<?> inviteMember(
            @PathVariable Long dbId,
            @RequestBody DbMemberDTO request) {

        dbService.sendInvitation(dbId, request);
        APIResponse apiResponse = APIResponse.builder().code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_MAIL.getMessage())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<?> acceptInvitation(
            @RequestBody DbMemberDTO dbMemberDTO) {
        dbService.acceptInvitation(dbMemberDTO);
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_JOIN.getMessage())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PatchMapping("/{dbId}/members/{memberId}")
    public ResponseEntity<?> updateMemberRole(@PathVariable Long dbId,
                                              @PathVariable Long memberId,
                                              @RequestBody String role) {
        dbMemberService.updateMemberRole(dbId, memberId, role);
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_UPDATED.getMessage())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @DeleteMapping("/{dbId}/members/{memberId}")
    public ResponseEntity<?> deleteMember(@PathVariable Long dbId, @PathVariable Long memberId) {
        dbMemberService.deleteMember(dbId, memberId);
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.NO_CONTENT.value())
                .message(APIResponseMessage.SUCCESSFULLY_DELETED.getMessage())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.NO_CONTENT);
    }
}
