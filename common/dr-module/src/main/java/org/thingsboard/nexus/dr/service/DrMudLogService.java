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
package org.thingsboard.nexus.dr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.nexus.dr.dto.DrMudLogDto;
import org.thingsboard.nexus.dr.model.DrMudLog;
import org.thingsboard.nexus.dr.model.enums.LithologyType;
import org.thingsboard.nexus.dr.repository.DrMudLogRepository;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Mud Log operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DrMudLogService {

    private final DrMudLogRepository mudLogRepository;

    // --- CRUD Operations ---

    @Transactional(readOnly = true)
    public DrMudLogDto getById(UUID id) {
        DrMudLog mudLog = mudLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mud log not found: " + id));
        return DrMudLogDto.fromEntity(mudLog);
    }

    @Transactional(readOnly = true)
    public Page<DrMudLogDto> getByTenant(UUID tenantId, Pageable pageable) {
        return mudLogRepository.findByTenantId(tenantId, pageable)
                .map(DrMudLogDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<DrMudLogDto> getByRun(UUID runId, Pageable pageable) {
        return mudLogRepository.findByRunId(runId, pageable)
                .map(DrMudLogDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getMudLogsByRunOrdered(UUID runId) {
        return mudLogRepository.findByRunIdOrderByMdFtAsc(runId).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<DrMudLogDto> getByFilters(UUID tenantId, UUID runId, UUID wellId,
                                           LithologyType lithology, String formationName, Pageable pageable) {
        return mudLogRepository.findByFilters(tenantId, runId, wellId, lithology, formationName, pageable)
                .map(DrMudLogDto::fromEntity);
    }

    @Transactional
    public DrMudLogDto create(DrMudLog mudLog) {
        log.info("Creating new mud log for run {} at MD {} ft", mudLog.getRunId(), mudLog.getMdFt());

        DrMudLog saved = mudLogRepository.save(mudLog);
        return DrMudLogDto.fromEntity(saved);
    }

    @Transactional
    public DrMudLogDto update(UUID id, DrMudLog mudLogUpdate) {
        log.info("Updating mud log: {}", id);

        DrMudLog existing = mudLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mud log not found: " + id));

        // Update depth info
        existing.setMdFt(mudLogUpdate.getMdFt());
        existing.setMdTopFt(mudLogUpdate.getMdTopFt());
        existing.setMdBottomFt(mudLogUpdate.getMdBottomFt());
        existing.setTvdFt(mudLogUpdate.getTvdFt());

        // Update lithology
        existing.setPrimaryLithology(mudLogUpdate.getPrimaryLithology());
        existing.setSecondaryLithology(mudLogUpdate.getSecondaryLithology());
        existing.setPrimaryLithologyPercent(mudLogUpdate.getPrimaryLithologyPercent());
        existing.setLithologyDescription(mudLogUpdate.getLithologyDescription());
        existing.setColor(mudLogUpdate.getColor());
        existing.setGrainSize(mudLogUpdate.getGrainSize());
        existing.setHardness(mudLogUpdate.getHardness());
        existing.setPorosityType(mudLogUpdate.getPorosityType());
        existing.setPorosityPercent(mudLogUpdate.getPorosityPercent());
        existing.setCite(mudLogUpdate.getCite());
        existing.setSorting(mudLogUpdate.getSorting());
        existing.setRoundness(mudLogUpdate.getRoundness());

        // Update gas data
        existing.setTotalGasUnits(mudLogUpdate.getTotalGasUnits());
        existing.setBackgroundGasUnits(mudLogUpdate.getBackgroundGasUnits());
        existing.setConnectionGasUnits(mudLogUpdate.getConnectionGasUnits());
        existing.setTripGasUnits(mudLogUpdate.getTripGasUnits());
        existing.setC1Percent(mudLogUpdate.getC1Percent());
        existing.setC2Percent(mudLogUpdate.getC2Percent());
        existing.setC3Percent(mudLogUpdate.getC3Percent());
        existing.setIc4Percent(mudLogUpdate.getIc4Percent());
        existing.setNc4Percent(mudLogUpdate.getNc4Percent());
        existing.setIc5Percent(mudLogUpdate.getIc5Percent());
        existing.setNc5Percent(mudLogUpdate.getNc5Percent());

        // Update hydrocarbon shows
        existing.setOilShowType(mudLogUpdate.getOilShowType());
        existing.setOilShowIntensity(mudLogUpdate.getOilShowIntensity());
        existing.setFluorescenceColor(mudLogUpdate.getFluorescenceColor());
        existing.setFluorescencePercent(mudLogUpdate.getFluorescencePercent());
        existing.setCutDescription(mudLogUpdate.getCutDescription());
        existing.setStainDescription(mudLogUpdate.getStainDescription());

        // Update drilling parameters
        existing.setRopFtHr(mudLogUpdate.getRopFtHr());
        existing.setWobKlbs(mudLogUpdate.getWobKlbs());
        existing.setRpm(mudLogUpdate.getRpm());
        existing.setTorqueFtLbs(mudLogUpdate.getTorqueFtLbs());
        existing.setPumpPressurePsi(mudLogUpdate.getPumpPressurePsi());
        existing.setFlowRateGpm(mudLogUpdate.getFlowRateGpm());

        // Update mud properties
        existing.setMudWeightPpg(mudLogUpdate.getMudWeightPpg());
        existing.setMudWeightOutPpg(mudLogUpdate.getMudWeightOutPpg());
        existing.setMudViscosity(mudLogUpdate.getMudViscosity());
        existing.setMudTempInF(mudLogUpdate.getMudTempInF());
        existing.setMudTempOutF(mudLogUpdate.getMudTempOutF());
        existing.setChloridesPpm(mudLogUpdate.getChloridesPpm());

        // Update sample info
        existing.setSampleType(mudLogUpdate.getSampleType());
        existing.setSampleNumber(mudLogUpdate.getSampleNumber());
        existing.setLagTimeMinutes(mudLogUpdate.getLagTimeMinutes());
        existing.setSampleTime(mudLogUpdate.getSampleTime());

        // Update formation info
        existing.setFormationName(mudLogUpdate.getFormationName());
        existing.setFormationTopFt(mudLogUpdate.getFormationTopFt());
        existing.setGeologicalAge(mudLogUpdate.getGeologicalAge());

        // Update metadata
        existing.setLoggedBy(mudLogUpdate.getLoggedBy());
        existing.setNotes(mudLogUpdate.getNotes());
        existing.setRawData(mudLogUpdate.getRawData());

        DrMudLog saved = mudLogRepository.save(existing);
        return DrMudLogDto.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting mud log: {}", id);
        if (!mudLogRepository.existsById(id)) {
            throw new EntityNotFoundException("Mud log not found: " + id);
        }
        mudLogRepository.deleteById(id);
    }

    // --- Lithology Methods ---

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getByLithology(UUID runId, LithologyType lithology) {
        return mudLogRepository.findByRunIdAndLithology(runId, lithology).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LithologyType> getDistinctLithologies(UUID runId) {
        return mudLogRepository.findDistinctLithologiesByRunId(runId);
    }

    @Transactional(readOnly = true)
    public Map<LithologyType, Long> getLithologyDistribution(UUID runId) {
        List<Object[]> results = mudLogRepository.countByLithologyForRunId(runId);
        return results.stream()
                .collect(Collectors.toMap(
                        r -> (LithologyType) r[0],
                        r -> (Long) r[1]
                ));
    }

    // --- Formation Methods ---

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getByFormation(UUID runId, String formationName) {
        return mudLogRepository.findByRunIdAndFormationName(runId, formationName).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctFormations(UUID runId) {
        return mudLogRepository.findDistinctFormationsByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getFormationTops(UUID runId) {
        return mudLogRepository.findFormationTopsByRunId(runId).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Record a formation top.
     */
    @Transactional
    public DrMudLogDto recordFormationTop(UUID id, String formationName, BigDecimal topDepth) {
        log.info("Recording formation top {} at {} ft for mud log {}", formationName, topDepth, id);

        DrMudLog mudLog = mudLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mud log not found: " + id));

        mudLog.setFormationName(formationName);
        mudLog.setFormationTopFt(topDepth);

        DrMudLog saved = mudLogRepository.save(mudLog);
        return DrMudLogDto.fromEntity(saved);
    }

    // --- Gas Analysis Methods ---

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getGasShows(UUID runId, BigDecimal threshold) {
        return mudLogRepository.findGasShowsByRunId(runId, threshold).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getMaxTotalGas(UUID runId) {
        return mudLogRepository.getMaxTotalGasByRunId(runId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageBackgroundGas(UUID runId) {
        return mudLogRepository.getAverageBackgroundGasByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getConnectionGasEvents(UUID runId, BigDecimal threshold) {
        return mudLogRepository.findConnectionGasEventsByRunId(runId, threshold).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update gas readings for a mud log.
     */
    @Transactional
    public DrMudLogDto updateGasReadings(UUID id, BigDecimal totalGas, BigDecimal backgroundGas,
                                          BigDecimal c1, BigDecimal c2, BigDecimal c3,
                                          BigDecimal ic4, BigDecimal nc4, BigDecimal ic5, BigDecimal nc5) {
        log.info("Updating gas readings for mud log {}", id);

        DrMudLog mudLog = mudLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mud log not found: " + id));

        mudLog.setTotalGasUnits(totalGas);
        mudLog.setBackgroundGasUnits(backgroundGas);
        mudLog.setC1Percent(c1);
        mudLog.setC2Percent(c2);
        mudLog.setC3Percent(c3);
        mudLog.setIc4Percent(ic4);
        mudLog.setNc4Percent(nc4);
        mudLog.setIc5Percent(ic5);
        mudLog.setNc5Percent(nc5);

        DrMudLog saved = mudLogRepository.save(mudLog);
        return DrMudLogDto.fromEntity(saved);
    }

    // --- Hydrocarbon Show Methods ---

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getOilShows(UUID runId) {
        return mudLogRepository.findOilShowsByRunId(runId).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getFluorescenceShows(UUID runId) {
        return mudLogRepository.findFluorescenceShowsByRunId(runId).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Record hydrocarbon show.
     */
    @Transactional
    public DrMudLogDto recordOilShow(UUID id, String showType, String intensity,
                                      String fluorescenceColor, BigDecimal fluorescencePercent,
                                      String cutDescription, String stainDescription) {
        log.info("Recording oil show for mud log {}", id);

        DrMudLog mudLog = mudLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mud log not found: " + id));

        mudLog.setOilShowType(showType);
        mudLog.setOilShowIntensity(intensity);
        mudLog.setFluorescenceColor(fluorescenceColor);
        mudLog.setFluorescencePercent(fluorescencePercent);
        mudLog.setCutDescription(cutDescription);
        mudLog.setStainDescription(stainDescription);

        DrMudLog saved = mudLogRepository.save(mudLog);
        return DrMudLogDto.fromEntity(saved);
    }

    // --- Depth Range Methods ---

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getByDepthRange(UUID runId, BigDecimal minDepth, BigDecimal maxDepth) {
        return mudLogRepository.findByRunIdAndDepthRange(runId, minDepth, maxDepth).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DrMudLogDto getDeepestMudLog(UUID runId) {
        List<DrMudLog> mudLogs = mudLogRepository.findDeepestMudLogByRunId(runId, PageRequest.of(0, 1));
        if (mudLogs.isEmpty()) {
            return null;
        }
        return DrMudLogDto.fromEntity(mudLogs.get(0));
    }

    @Transactional(readOnly = true)
    public DrMudLogDto getLatestMudLog(UUID runId) {
        List<DrMudLog> mudLogs = mudLogRepository.findLatestMudLogByRunId(runId, PageRequest.of(0, 1));
        if (mudLogs.isEmpty()) {
            return null;
        }
        return DrMudLogDto.fromEntity(mudLogs.get(0));
    }

    // --- Drilling Parameters Methods ---

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getHighRopZones(UUID runId, BigDecimal threshold) {
        return mudLogRepository.findHighRopZones(runId, threshold).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageRopForDepthRange(UUID runId, BigDecimal minDepth, BigDecimal maxDepth) {
        return mudLogRepository.getAverageRopForDepthRange(runId, minDepth, maxDepth);
    }

    // --- Porosity Methods ---

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getPorousZones(UUID runId, BigDecimal threshold) {
        return mudLogRepository.findPorousZones(runId, threshold).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }

    // --- Statistics ---

    @Transactional(readOnly = true)
    public long countByRun(UUID runId) {
        return mudLogRepository.countByRunId(runId);
    }

    @Transactional(readOnly = true)
    public long countByWell(UUID wellId) {
        return mudLogRepository.countByWellId(wellId);
    }

    // --- Geologist Methods ---

    @Transactional(readOnly = true)
    public List<String> getDistinctGeologists(UUID runId) {
        return mudLogRepository.findDistinctGeologistsByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<DrMudLogDto> getByGeologist(UUID runId, String loggedBy) {
        return mudLogRepository.findByRunIdAndLoggedBy(runId, loggedBy).stream()
                .map(DrMudLogDto::fromEntity)
                .collect(Collectors.toList());
    }
}
