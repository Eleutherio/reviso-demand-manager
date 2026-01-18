package com.guilherme.reviso_demand_manager.application;

import com.guilherme.reviso_demand_manager.domain.Agency;
import com.guilherme.reviso_demand_manager.infra.AgencyRepository;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
public class TenantProvisioningService {

    private final AgencyRepository agencyRepository;

    @Value("${spring.datasource.url}")
    private String baseUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    public TenantProvisioningService(AgencyRepository agencyRepository) {
        this.agencyRepository = agencyRepository;
    }

    @Transactional
    public void provisionTenant(UUID agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Agency not found"));

        if (agency.getDatabaseName() != null) {
            throw new IllegalStateException("Agency already has a database");
        }

        String databaseName = generateDatabaseName(agency.getName(), agencyId);
        
        createDatabase(databaseName);
        runMigrations(databaseName);
        
        agency.setDatabaseName(databaseName);
        agencyRepository.save(agency);
    }

    private void createDatabase(String databaseName) {
        String postgresUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/')) + "/postgres";
        
        try (Connection conn = DriverManager.getConnection(postgresUrl, username, password);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE DATABASE " + databaseName);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create database: " + databaseName, e);
        }
    }

    private void runMigrations(String databaseName) {
        String tenantUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1) + databaseName;
        
        Flyway flyway = Flyway.configure()
                .dataSource(tenantUrl, username, password)
                .locations("classpath:db/migration")
                .load();
        
        flyway.migrate();
    }

    private String generateDatabaseName(String agencyName, UUID agencyId) {
        String normalized = normalize(agencyName);
        String prefix = normalized.length() > 10 ? normalized.substring(0, 10) : normalized;
        String suffix = agencyId.toString().substring(0, 8);
        return "tenant_" + prefix + "_" + suffix;
    }

    private String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String withoutMarks = decomposed.replaceAll("\\p{M}", "");
        return withoutMarks.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
