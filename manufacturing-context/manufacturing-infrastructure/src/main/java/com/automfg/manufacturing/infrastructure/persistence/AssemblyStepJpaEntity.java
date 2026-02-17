package com.automfg.manufacturing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assembly_steps")
public class AssemblyStepJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assembly_process_id", nullable = false)
    private AssemblyProcessJpaEntity assemblyProcess;

    @Column(name = "work_station_code", nullable = false, length = 20)
    private String workStationCode;

    @Column(name = "work_station_sequence", nullable = false)
    private int workStationSequence;

    @Column(name = "task_description", nullable = false, length = 500)
    private String taskDescription;

    @Column(name = "standard_time_minutes", nullable = false)
    private int standardTimeMinutes;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "operator_id", length = 50)
    private String operatorId;

    @Column(name = "material_batch_id", length = 100)
    private String materialBatchId;

    @Column(name = "actual_time_minutes")
    private Integer actualTimeMinutes;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "corrects_record_id")
    private UUID correctsRecordId;

    protected AssemblyStepJpaEntity() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AssemblyProcessJpaEntity getAssemblyProcess() {
        return assemblyProcess;
    }

    public void setAssemblyProcess(AssemblyProcessJpaEntity assemblyProcess) {
        this.assemblyProcess = assemblyProcess;
    }

    public String getWorkStationCode() {
        return workStationCode;
    }

    public void setWorkStationCode(String workStationCode) {
        this.workStationCode = workStationCode;
    }

    public int getWorkStationSequence() {
        return workStationSequence;
    }

    public void setWorkStationSequence(int workStationSequence) {
        this.workStationSequence = workStationSequence;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public int getStandardTimeMinutes() {
        return standardTimeMinutes;
    }

    public void setStandardTimeMinutes(int standardTimeMinutes) {
        this.standardTimeMinutes = standardTimeMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getMaterialBatchId() {
        return materialBatchId;
    }

    public void setMaterialBatchId(String materialBatchId) {
        this.materialBatchId = materialBatchId;
    }

    public Integer getActualTimeMinutes() {
        return actualTimeMinutes;
    }

    public void setActualTimeMinutes(Integer actualTimeMinutes) {
        this.actualTimeMinutes = actualTimeMinutes;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public UUID getCorrectsRecordId() {
        return correctsRecordId;
    }

    public void setCorrectsRecordId(UUID correctsRecordId) {
        this.correctsRecordId = correctsRecordId;
    }
}
