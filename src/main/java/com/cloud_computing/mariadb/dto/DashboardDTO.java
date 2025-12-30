package com.cloud_computing.mariadb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private Long totalProjects;
    private Long totalDatabases;
    private Long totalBackups;
}
