package com.shabab.RealEstateManagementSystem.core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Date;

/**
 * Project: ConstructionAndRealEstateManagement-SpringBoot
 * Author: Shabab
 * Created on: 27/09/2024
 */

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "const_stages")
public class ConstructionStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Date startDate;

    private Date endDate;

    @Enumerated(EnumType.STRING)
    private StageStatus status;

    public enum StageStatus {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }

    @ManyToOne
    private Building building;

    @ManyToOne
    private Floor floor;

    @ManyToOne
    private Unit unit;

    @Column(nullable = false)
    private Long companyId; //Loose relation to Company

}
