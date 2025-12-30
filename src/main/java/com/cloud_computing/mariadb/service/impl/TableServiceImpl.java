package com.cloud_computing.mariadb.service.impl;

import com.cloud_computing.mariadb.annotation.AuditLog;
import com.cloud_computing.mariadb.dto.ColumnCreateDTO;
import com.cloud_computing.mariadb.dto.ColumnModifyDTO;
import com.cloud_computing.mariadb.dto.RowDTO;
import com.cloud_computing.mariadb.dto.TableDataDTO;
import com.cloud_computing.mariadb.dto.request.TableAlterRequest;
import com.cloud_computing.mariadb.dto.request.TableCreateRequest;
import com.cloud_computing.mariadb.entity.Db;
import com.cloud_computing.mariadb.entity.DbMember;
import com.cloud_computing.mariadb.entity.DbUser;
import com.cloud_computing.mariadb.entity.User;
import com.cloud_computing.mariadb.entity.enums.DbRole;
import com.cloud_computing.mariadb.exception.BadRequestException;
import com.cloud_computing.mariadb.exception.ResourceNotFoundException;
import com.cloud_computing.mariadb.exception.UnauthorizedException;
import com.cloud_computing.mariadb.repository.DbMemberRepository;
import com.cloud_computing.mariadb.repository.DbRepository;
import com.cloud_computing.mariadb.repository.DbUserRepository;
import com.cloud_computing.mariadb.repository.UserRepository;
import com.cloud_computing.mariadb.service.TableService;
import com.cloud_computing.mariadb.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TableServiceImpl implements TableService {
    final DbRepository dbRepository;
    final DbMemberRepository dbMemberRepository;
    final DbUserRepository dbUserRepository;
    final UserRepository userRepository;

    @Autowired
    @Qualifier("secondaryJdbcTemplate")
    JdbcTemplate mariadbJdbcTemplate;

    @Override
    @Transactional
    @AuditLog(action = "CREATE_TABLE", description = "tạo bảng")
    public void createTable(Long dbId, TableCreateRequest dto) {
        User currentUser = userRepository.findByUsername(SecurityUtils.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập"));
        checkPermission(dbId, currentUser, DbRole.READWRITE);
        Db db =getDb(dbId);
        DbUser dbUser = getDbUser(currentUser.getId(), dbId);
        JdbcTemplate jdbcTemplate = createJdbcTemplate(db, dbUser);
        String sql = buildCreateTableSql(dto);

        jdbcTemplate.execute(sql);
    }

    @Override
    @Transactional
    @AuditLog(action = "ALTER_COLUMN", description = "chỉnh sửa cột")
    public void alterColumn(Long dbId, String tableName, TableAlterRequest request) {
        User currentUser = getCurrentUser();
        checkPermission(dbId, currentUser, DbRole.READWRITE);

        Db db = getDb(dbId);
        DbUser dbUser = getDbUser(currentUser.getId(), dbId);

        JdbcTemplate template = createJdbcTemplate(db, dbUser);
        List<String> statements = buildAlterTableSql(tableName, request);

        for (String sql : statements) {
            template.execute(sql);
        }
    }

    @Override
    @Transactional
    @AuditLog(action = "RENAME_TABLE", description = "đổi tên bảng")
    public void renameTable(Long dbId, String oldName, String newName) {
        User currentUser = getCurrentUser();
        checkPermission(dbId, currentUser, DbRole.READWRITE);

        Db db = getDb(dbId);
        DbUser dbUser = getDbUser(currentUser.getId(), dbId);
        JdbcTemplate template = createJdbcTemplate(db, dbUser);
        template.execute(String.format("RENAME TABLE `%s` TO `%s`", oldName, newName));
    }

    @Override
    @Transactional
    @AuditLog(action = "DELETE_TABLE", description = "xóa bảng")
    public void dropTable(Long dbId, String tableName) {
        User currentUser = getCurrentUser();
        checkPermission(dbId, currentUser, DbRole.READWRITE);

        Db db = getDb(dbId);
        DbUser dbUser = getDbUser(currentUser.getId(), dbId);
        JdbcTemplate template = createJdbcTemplate(db, dbUser);
        template.execute(String.format("DROP TABLE IF EXISTS `%s`", tableName));
    }

    @Override
    public List<TableDataDTO> getTables(Long dbId) {
        User currentUser = getCurrentUser();
        checkPermission(dbId, currentUser, DbRole.READONLY);

        Db db = getDb(dbId);
        DbUser dbUser = getDbUser(currentUser.getId(), dbId);

        JdbcTemplate template = createJdbcTemplate(db, dbUser);
        List<String> tableNames = template.queryForList("SHOW TABLES", String.class);

        List<TableDataDTO> tableData = new ArrayList<>();

        for (String tableName : tableNames) {
            try {
                // Lấy thông tin chi tiết từ information_schema
                String sql = """
                    SELECT 
                        TABLE_NAME as name,
                        TABLE_ROWS as totalRows,
                        ENGINE as engine,
                        TABLE_COLLATION as collation
                    FROM information_schema.TABLES
                    WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                    """;

                Map<String, Object> tableInfo = template.queryForMap(sql, db.getName(), tableName);

                // Đếm số columns
                String countColumnsSql = String.format("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '%s' AND TABLE_NAME = '%s'",
                        db.getName(), tableName);
                Long totalColumns = template.queryForObject(countColumnsSql, Long.class);

                // Build DTO
                TableDataDTO dto = TableDataDTO.builder()
                        .name((String) tableInfo.get("name"))
                        .totalColumns(totalColumns)
                        .build();

                tableData.add(dto);

            } catch (Exception e) {

                // Fallback: chỉ trả về tên bảng
                tableData.add(TableDataDTO.builder()
                        .name(tableName)
                        .totalColumns(0L)
                        .totalRows(0L)
                        .build());
            }
        }

        return tableData;
    }

    @Override
    public Map<String, Object> getTableStructure(Long dbId, String tableName) {
        User currentUser = getCurrentUser();
        checkPermission(dbId, currentUser, DbRole.READONLY);

        Db db = getDb(dbId);
        DbUser dbUser = getDbUser(currentUser.getId(), dbId);

        JdbcTemplate template = createJdbcTemplate(db, dbUser);

        Map<String, Object> result = new HashMap<>();

        String descSql = String.format("DESCRIBE `%s`", tableName);
        result.put("columns", template.queryForList(descSql));

        String indexSql = String.format("SHOW INDEX FROM `%s`", tableName);
        result.put("indexes", template.queryForList(indexSql));

        String createSql = String.format("SHOW CREATE TABLE `%s`", tableName);
        result.put("createStatement", template.queryForMap(createSql).get("Create Table"));

        return result;
    }

    @Override
    public TableDataDTO getTableData(Long dbId, String tableName, int page, int pageSize) {
        User currentUser = getCurrentUser();
        checkPermission(dbId, currentUser, DbRole.READONLY);

        Db db = getDb(dbId);
        DbUser dbUser = getDbUser(currentUser.getId(), dbId);

        JdbcTemplate template = createJdbcTemplate(db, dbUser);

        // Lấy tổng số rows
        String countSql = String.format("SELECT COUNT(*) FROM `%s`", tableName);
        Long totalRows = template.queryForObject(countSql, Long.class);

        // Lấy tên columns
        String descSql = String.format("DESCRIBE `%s`", tableName);
        List<Map<String, Object>> columnInfo = template.queryForList(descSql);
        List<String> columns = new ArrayList<>();
        for (Map<String, Object> col : columnInfo) {
            columns.add((String) col.get("Field"));
        }

        // Lấy data với phân trang
        int offset = page * pageSize;
        String dataSql = String.format("SELECT * FROM `%s` LIMIT %d OFFSET %d",
                tableName, pageSize, offset);
        List<Map<String, Object>> rows = template.queryForList(dataSql);

        return TableDataDTO.builder()
                .name(tableName)
                .columns(columns)
                .rows(rows)
                .totalRows(totalRows != null ? totalRows : 0)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional
    @AuditLog(action = "INSERT_ROW", description = "thêm dòng")
    public void insertRow(Long dbId, String tableName, RowDTO request) {
        User currentUser = getCurrentUser();
        checkPermission(dbId, currentUser, DbRole.READWRITE);

        Db db = getDb(dbId);
        DbUser dbUser = getDbUser(currentUser.getId(), dbId);
        JdbcTemplate template = createJdbcTemplate(db, dbUser);

        List<Map<String, Object>> data = request.getData();

        if (data == null || data.isEmpty()) {
            throw new BadRequestException("Danh sách data không được rỗng");
        }

        Map<String, Object> firstRow = data.get(0);
        String columns = firstRow.keySet().stream()
                .map(col -> "`" + col + "`")
                .collect(Collectors.joining(", "));

        String placeholders = firstRow.keySet().stream()
                .map(col -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("INSERT INTO `%s` (%s) VALUES (%s)",
                tableName, columns, placeholders);

        List<Object[]> batchArgs = data.stream()
                .map(row -> row.values().toArray())
                .collect(Collectors.toList());

        template.batchUpdate(sql, batchArgs);
    }

    @Override
    @Transactional
    @AuditLog(action = "UPDATE_ROW", description = "chỉnh sửa dòng")
    public void updateRow(Long dbId, String tableName, RowDTO request) {
        User currentUser = getCurrentUser();
        checkPermission(dbId, currentUser, DbRole.READWRITE);

        Db db = getDb(dbId);
        DbUser dbUser = getDbUser(currentUser.getId(), dbId);
        JdbcTemplate template = createJdbcTemplate(db, dbUser);

        List<Long> ids = request.getIds();
        List<Map<String, Object>> data = request.getData();

        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Danh sách IDs không được rỗng");
        }
        if (data == null || data.isEmpty()) {
            throw new BadRequestException("Danh sách data không được rỗng");
        }
        if (ids.size() != data.size()) {
            throw new BadRequestException("Số lượng IDs và data phải bằng nhau");
        }

        int updatedCount = 0;

        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            Map<String, Object> rowData = data.get(i);

            if (rowData.isEmpty()) {
                continue;
            }

            String setClause = rowData.keySet().stream()
                    .map(col -> "`" + col + "` = ?")
                    .collect(Collectors.joining(", "));

            String sql = String.format("UPDATE `%s` SET %s WHERE `id` = ?",
                    tableName, setClause);

            List<Object> values = new ArrayList<>(rowData.values());
            values.add(id);

            int affected = template.update(sql, values.toArray());

            if (affected == 0) {
            } else {
                updatedCount++;
            }
        }
    }

    @Override
    @Transactional
    @AuditLog(action = "DELETE_ROW", description = "xóa dòng")
    public void deleteRow(Long dbId, String tableName, RowDTO request) {
        User currentUser = getCurrentUser();
        checkPermission(dbId, currentUser, DbRole.READWRITE);

        Db db = getDb(dbId);
        DbUser dbUser = getDbUser(currentUser.getId(), dbId);
        JdbcTemplate template = createJdbcTemplate(db, dbUser);

        List<Long> ids = request.getIds();

        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Danh sách IDs không được rỗng");
        }

        // DELETE FROM table WHERE id IN (?, ?, ?)
        String placeholders = ids.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("DELETE FROM `%s` WHERE `id` IN (%s)",
                tableName, placeholders);

        Object[] values = ids.toArray();

        int rowsAffected = template.update(sql, values);

        if (rowsAffected == 0) {
            throw new ResourceNotFoundException("Không tìm thấy row nào với IDs đã cho");
        }
    }

    private void checkPermission(Long dbId, User user, DbRole minRole) {
        DbMember member = dbMemberRepository.findByDb_IdAndUser_Id(dbId, user.getId())
                .orElseThrow(() -> new UnauthorizedException("Bạn không có quyền truy cập database này"));

        DbRole userRole = DbRole.valueOf(member.getRole());

        if (minRole == DbRole.READWRITE) {
            if (userRole != DbRole.OWNER && userRole != DbRole.ADMIN && userRole != DbRole.READWRITE) {
                throw new UnauthorizedException("Bạn không có quyền thực hiện thao tác này");
            }
        } else if (minRole == DbRole.ADMIN) {
            if (userRole != DbRole.OWNER && userRole != DbRole.ADMIN) {
                throw new UnauthorizedException("Chỉ OWNER/ADMIN mới có quyền");
            }
        }
    }


    private User getCurrentUser() {
        return userRepository.findByUsername(SecurityUtils.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập"));
    }


    private Db getDb(Long dbId) {
        return dbRepository.findById(dbId)
                .orElseThrow(() -> new ResourceNotFoundException("Database không tồn tại"));
    }

    private DbUser getDbUser(Long userId, Long dbId) {
        return dbUserRepository.findByUser_IdAndDb_Id(userId, dbId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy credentials"));
    }

    private JdbcTemplate createJdbcTemplate(Db db, DbUser dbUser) {
        String url = String.format("jdbc:mariadb://%s:%d/%s",
                db.getHostname(), db.getPort(), db.getName());

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(dbUser.getUsername());
        dataSource.setPassword(dbUser.getPassword());

        return new JdbcTemplate(dataSource);
    }

    private String buildCreateTableSql(TableCreateRequest request) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `").append(request.getTableName()).append("` (\n");

        List<String> columnDefs = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        List<String> foreignKeys = new ArrayList<>();

        for (ColumnCreateDTO col : request.getColumns()) {
            StringBuilder colDef = new StringBuilder("  `").append(col.getName()).append("` ");
            colDef.append(parseColumnType(col));
            columnDefs.add(colDef.toString());

            // Collect Primary Keys
            if (col.getConstraints() != null &&
                    col.getConstraints().toLowerCase().contains("primary key")) {
                primaryKeys.add(col.getName());
            }

            // ✅ Collect Foreign Keys
            if (col.getForeignKeyTable() != null && col.getForeignKeyColumn() != null) {
                StringBuilder fkDef = new StringBuilder();
                fkDef.append("  CONSTRAINT `fk_").append(request.getTableName())
                        .append("_").append(col.getName()).append("` ");
                fkDef.append("FOREIGN KEY (`").append(col.getName()).append("`) ");
                fkDef.append("REFERENCES `").append(col.getForeignKeyTable()).append("` ");
                fkDef.append("(`").append(col.getForeignKeyColumn()).append("`)");

                if (col.getOnDelete() != null && !col.getOnDelete().isEmpty()) {
                    fkDef.append(" ON DELETE ").append(col.getOnDelete().toUpperCase());
                }
                if (col.getOnUpdate() != null && !col.getOnUpdate().isEmpty()) {
                    fkDef.append(" ON UPDATE ").append(col.getOnUpdate().toUpperCase());
                }

                foreignKeys.add(fkDef.toString());
            }
        }

        sql.append(String.join(",\n", columnDefs));

        // Add PRIMARY KEY
        if (!primaryKeys.isEmpty()) {
            sql.append(",\n  PRIMARY KEY (");
            sql.append(String.join(", ", primaryKeys.stream()
                    .map(k -> "`" + k + "`")
                    .toArray(String[]::new)));
            sql.append(")");
        }

        // ✅ Add FOREIGN KEYs
        if (!foreignKeys.isEmpty()) {
            sql.append(",\n");
            sql.append(String.join(",\n", foreignKeys));
        }

        sql.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        return sql.toString();
    }

    private List<String> buildAlterTableSql(String tableName, TableAlterRequest request) {
        List<String> statements = new ArrayList<>();

        if (request.getAddColumns() != null) {
            for (ColumnCreateDTO col : request.getAddColumns()) {
                String sql = String.format("ALTER TABLE `%s` ADD COLUMN `%s` %s",
                        tableName, col.getName(), parseColumnType(col));
                statements.add(sql);

                // ✅ Add Foreign Key nếu có
                if (col.getForeignKeyTable() != null && col.getForeignKeyColumn() != null) {
                    StringBuilder fkSql = new StringBuilder();
                    fkSql.append("ALTER TABLE `").append(tableName).append("` ");
                    fkSql.append("ADD CONSTRAINT `fk_").append(tableName)
                            .append("_").append(col.getName()).append("` ");
                    fkSql.append("FOREIGN KEY (`").append(col.getName()).append("`) ");
                    fkSql.append("REFERENCES `").append(col.getForeignKeyTable()).append("` ");
                    fkSql.append("(`").append(col.getForeignKeyColumn()).append("`)");

                    if (col.getOnDelete() != null && !col.getOnDelete().isEmpty()) {
                        fkSql.append(" ON DELETE ").append(col.getOnDelete().toUpperCase());
                    }
                    if (col.getOnUpdate() != null && !col.getOnUpdate().isEmpty()) {
                        fkSql.append(" ON UPDATE ").append(col.getOnUpdate().toUpperCase());
                    }

                    statements.add(fkSql.toString());
                }
            }
        }

        if (request.getDropColumns() != null) {
            for (String colName : request.getDropColumns()) {
                statements.add(String.format("ALTER TABLE `%s` DROP COLUMN `%s`",
                        tableName, colName));
            }
        }

        if (request.getModifyColumns() != null) {
            for (ColumnModifyDTO col : request.getModifyColumns()) {
                ColumnCreateDTO colDef = ColumnCreateDTO.builder()
                        .type(col.getType())
                        .length(col.getLength())
                        .defaultValue(col.getDefaultValue())
                        .constraints(col.getConstraints())
                        .build();

                String sql = String.format("ALTER TABLE `%s` CHANGE COLUMN `%s` `%s` %s",
                        tableName, col.getOldName(), col.getNewName(), parseColumnType(colDef));
                statements.add(sql);
            }
        }

        return statements;
    }

    private String parseColumnType(ColumnCreateDTO col) {
        StringBuilder sql = new StringBuilder();

        String type = col.getType().toLowerCase();

        switch (type) {
            case "serial":
                sql.append("INT AUTO_INCREMENT");
                break;
            case "integer":
            case "int":
                sql.append("INT");
                break;
            case "bigint":
                sql.append("BIGINT");
                break;
            case "varchar":
            case "string":
                int length = col.getLength() != null ? col.getLength() : 255;
                sql.append("VARCHAR(").append(length).append(")");
                break;
            case "text":
                sql.append("TEXT");
                break;
            case "boolean":
            case "bool":
                sql.append("TINYINT(1)");
                break;
            case "timestamp":
            case "datetime":
                sql.append("DATETIME");
                break;
            case "date":
                sql.append("DATE");
                break;
            case "time":
                sql.append("TIME");
                break;
            case "float":
                sql.append("FLOAT");
                break;
            case "double":
                sql.append("DOUBLE");
                break;
            case "decimal":
                sql.append("DECIMAL(10,2)");
                break;
            case "json":
                sql.append("JSON");
                break;
            default:
                sql.append(type.toUpperCase());
        }

        String constraints = col.getConstraints();
        if (constraints != null) {
            String lower = constraints.toLowerCase();
            if (lower.contains("not null")) {
                sql.append(" NOT NULL");
            }
            if (lower.contains("unique")) {
                sql.append(" UNIQUE");
            }
        }

        String defaultVal = col.getDefaultValue();
        if (defaultVal != null && !defaultVal.isEmpty() &&
                !defaultVal.equalsIgnoreCase("no default")) {

            if (defaultVal.equalsIgnoreCase("null")) {
                sql.append(" DEFAULT NULL");
            } else if (defaultVal.equalsIgnoreCase("current_timestamp")) {
                sql.append(" DEFAULT CURRENT_TIMESTAMP");
            } else if (defaultVal.matches("^\\d+$")) {
                sql.append(" DEFAULT ").append(defaultVal);
            } else {
                sql.append(" DEFAULT '").append(defaultVal.replace("'", "''")).append("'");
            }
        }

        return sql.toString();
    }
//
//    private List<String> splitSqlStatements(String sqlContent) {
//        List<String> statements = new ArrayList<>();
//        StringBuilder current = new StringBuilder();
//        boolean inString = false;
//        char stringChar = 0;
//
//        for (int i = 0; i < sqlContent.length(); i++) {
//            char c = sqlContent.charAt(i);
//
//            if (inString) {
//                current.append(c);
//                if (c == stringChar && (i == 0 || sqlContent.charAt(i - 1) != '\\')) {
//                    inString = false;
//                }
//            } else {
//                if (c == '\'' || c == '"') {
//                    inString = true;
//                    stringChar = c;
//                    current.append(c);
//                } else if (c == ';') {
//                    String stmt = current.toString().trim();
//                    if (!stmt.isEmpty()) {
//                        statements.add(stmt);
//                    }
//                    current = new StringBuilder();
//                } else {
//                    current.append(c);
//                }
//            }
//        }
//
//        String last = current.toString().trim();
//        if (!last.isEmpty()) {
//            statements.add(last);
//        }
//
//        return statements;
//    }
//}
}
