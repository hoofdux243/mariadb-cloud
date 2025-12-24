package com.cloud_computing.mariadb.entity.enums;

import lombok.Getter;

@Getter
public enum DbRole {
    OWNER, ADMIN, READWRITE, READONLY;

    public String getGrantStatement(String dbName, String username) {
        switch (this) {
            case OWNER:
                return String.format(
                        "GRANT ALL PRIVILEGES ON `%s`.* TO '%s'@'%%' WITH GRANT OPTION",
                        dbName, username
                );
            case ADMIN:
                return String.format(
                        "GRANT ALL PRIVILEGES ON `%s`.* TO '%s'@'%%'",
                        dbName, username
                );
            case READWRITE:
                return String.format(
                        "GRANT SELECT, INSERT, UPDATE, DELETE ON `%s`.* TO '%s'@'%%'",
                        dbName, username
                );
            case READONLY:
                return String.format(
                        "GRANT SELECT ON `%s`.* TO '%s'@'%%'",
                        dbName, username
                );
            default:
                throw new IllegalArgumentException("Role không xác định: " + this);
        }
    }
}
