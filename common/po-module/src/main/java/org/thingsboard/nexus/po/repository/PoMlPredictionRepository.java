/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.nexus.po.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.nexus.po.dto.ml.PredictionType;
import org.thingsboard.nexus.po.model.PoMlPrediction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ML prediction results.
 */
@Repository
public interface PoMlPredictionRepository extends JpaRepository<PoMlPrediction, UUID> {

    /**
     * Find predictions by tenant with pagination.
     */
    Page<PoMlPrediction> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find predictions by well asset.
     */
    List<PoMlPrediction> findByWellAssetIdOrderByCreatedTimeDesc(UUID wellAssetId);

    /**
     * Find predictions by type.
     */
    Page<PoMlPrediction> findByTenantIdAndPredictionType(UUID tenantId, PredictionType predictionType, Pageable pageable);

    /**
     * Find latest prediction for a well by type.
     */
    @Query("SELECT p FROM PoMlPrediction p WHERE p.wellAssetId = :wellAssetId AND p.predictionType = :predictionType ORDER BY p.createdTime DESC LIMIT 1")
    Optional<PoMlPrediction> findLatestByWellAndType(@Param("wellAssetId") UUID wellAssetId, @Param("predictionType") PredictionType predictionType);

    /**
     * Find latest predictions for all wells (one per well).
     */
    @Query(value = "SELECT DISTINCT ON (well_asset_id) * FROM po_ml_prediction " +
            "WHERE tenant_id = :tenantId AND prediction_type = :predictionType " +
            "ORDER BY well_asset_id, created_time DESC",
            nativeQuery = true)
    List<PoMlPrediction> findLatestPredictions(@Param("tenantId") UUID tenantId, @Param("predictionType") String predictionType);

    /**
     * Find high-risk wells (failure probability above threshold).
     */
    @Query("SELECT p FROM PoMlPrediction p WHERE p.tenantId = :tenantId " +
            "AND p.predictionType = 'FAILURE' AND p.probability >= :threshold " +
            "ORDER BY p.probability DESC")
    List<PoMlPrediction> findHighRiskWells(@Param("tenantId") UUID tenantId, @Param("threshold") BigDecimal threshold);

    /**
     * Find wells by health level.
     */
    @Query(value = "SELECT DISTINCT ON (well_asset_id) * FROM po_ml_prediction " +
            "WHERE tenant_id = :tenantId AND prediction_type = 'HEALTH_SCORE' " +
            "AND health_level = :healthLevel " +
            "ORDER BY well_asset_id, created_time DESC",
            nativeQuery = true)
    List<PoMlPrediction> findByHealthLevel(@Param("tenantId") UUID tenantId, @Param("healthLevel") String healthLevel);

    /**
     * Find anomalous wells.
     */
    @Query(value = "SELECT DISTINCT ON (well_asset_id) * FROM po_ml_prediction " +
            "WHERE tenant_id = :tenantId AND prediction_type = 'ANOMALY' " +
            "AND is_anomaly = true " +
            "ORDER BY well_asset_id, created_time DESC",
            nativeQuery = true)
    List<PoMlPrediction> findAnomalousWells(@Param("tenantId") UUID tenantId);

    /**
     * Find prediction history for a well.
     */
    @Query("SELECT p FROM PoMlPrediction p WHERE p.wellAssetId = :wellAssetId " +
            "AND p.predictionType = :predictionType " +
            "AND p.createdTime BETWEEN :startTime AND :endTime " +
            "ORDER BY p.createdTime ASC")
    List<PoMlPrediction> findHistory(@Param("wellAssetId") UUID wellAssetId,
                                      @Param("predictionType") PredictionType predictionType,
                                      @Param("startTime") Long startTime,
                                      @Param("endTime") Long endTime);

    /**
     * Count predictions requiring action (not acknowledged/dismissed).
     */
    @Query("SELECT COUNT(p) FROM PoMlPrediction p WHERE p.tenantId = :tenantId " +
            "AND p.probability >= :threshold " +
            "AND p.acknowledged = false AND p.dismissed = false")
    long countPendingActions(@Param("tenantId") UUID tenantId, @Param("threshold") BigDecimal threshold);

    /**
     * Delete old predictions.
     */
    @Modifying
    @Query("DELETE FROM PoMlPrediction p WHERE p.tenantId = :tenantId AND p.createdTime < :beforeTime")
    void deleteOldPredictions(@Param("tenantId") UUID tenantId, @Param("beforeTime") Long beforeTime);
}
