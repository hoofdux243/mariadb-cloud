package com.cloud_computing.mariadb.controller;

import com.cloud_computing.mariadb.dto.DashboardDTO;
import com.cloud_computing.mariadb.dto.response.APIResponse;
import com.cloud_computing.mariadb.repository.ProjectRepository;
import com.cloud_computing.mariadb.service.DashboardService;
import com.cloud_computing.mariadb.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {
    final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        DashboardDTO stats = dashboardService.getDashboard();
        return ResponseEntity.ok(APIResponse.<DashboardDTO>builder()
                .code(200)
                .message("Lấy thống kê thành công")
                .data(stats)
                .build());
    }
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }


}
