package com.cloud_computing.mariadb.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "dbs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Db {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "db_name", nullable = false, length = 100)
    private String name;


    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "hostname", nullable = false)
    private String hostname;

    @Column(name = "port", nullable = false)
    private Integer port;

    @PrePersist
    public void prePersist()
    {
        createdAt = Instant.now();
    }
}