package com.cloud_computing.mariadb.controller;

import com.cloud_computing.mariadb.dto.ProjectDTO;
import com.cloud_computing.mariadb.dto.response.APIResponse;
import com.cloud_computing.mariadb.dto.response.APIResponseMessage;
import com.cloud_computing.mariadb.service.DbService;
import com.cloud_computing.mariadb.service.ProjectService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectController {
    @Autowired
    ProjectService projectService;
    @Autowired
    DbService dbService;

    @GetMapping
    public ResponseEntity<?> getProjects() {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.getMessage())
                .data(projectService.getProjects())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProjectById(@PathVariable Long projectId) {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.getMessage())
                .data(projectService.getProject(projectId))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody ProjectDTO projectDTO) {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.CREATED.value())
                .message(APIResponseMessage.SUCCESSFULLY_CREATED.getMessage())
                .data(projectService.createProject(projectDTO))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.NO_CONTENT.value())
                .message(APIResponseMessage.SUCCESSFULLY_DELETED.getMessage())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{projectId}/dbs")
    public ResponseEntity<?> getDBs(@PathVariable Long projectId) {
        APIResponse apiResponse = APIResponse.builder()
                .code(HttpStatus.OK.value())
                .message(APIResponseMessage.SUCCESSFULLY_RETRIEVED.getMessage())
                .data(dbService.getDbsByProjectId(projectId))
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
