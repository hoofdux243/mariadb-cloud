package com.cloud_computing.mariadb.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Slf4j
@Component
public class DataSourceHealthCheck implements CommandLineRunner {

    private final DataSource primaryDataSource;
    private final DataSource secondaryDataSource;

    public DataSourceHealthCheck(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("secondaryDataSource") DataSource secondaryDataSource) {
        this.primaryDataSource = primaryDataSource;
        this.secondaryDataSource = secondaryDataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("Checking DataSource Connections...");
        log.info("========================================");

        // Check Primary DataSource
        try (Connection conn = primaryDataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            log.info("✅ PRIMARY DataSource Connected:");
            log.info("   URL: {}", metaData.getURL());
            log.info("   Database: {}", metaData.getDatabaseProductName());
            log.info("   Version: {}", metaData.getDatabaseProductVersion());
            log.info("   Driver: {}", metaData.getDriverName());
        } catch (Exception e) {
            log.error("❌ PRIMARY DataSource Connection Failed: {}", e.getMessage());
        }

        // Check Secondary DataSource
        try (Connection conn = secondaryDataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            log.info("✅ SECONDARY DataSource Connected:");
            log.info("   URL: {}", metaData.getURL());
            log.info("   Database: {}", metaData.getDatabaseProductName());
            log.info("   Version: {}", metaData.getDatabaseProductVersion());
            log.info("   Driver: {}", metaData.getDriverName());
        } catch (Exception e) {
            log.error("❌ SECONDARY DataSource Connection Failed: {}", e.getMessage());
        }

        log.info("========================================");
    }
}