package com.interviewrecord.tracking.application;

import com.interviewrecord.common.error.ConflictException;
import com.interviewrecord.common.error.InvalidInputException;
import com.interviewrecord.common.error.NotFoundException;
import com.interviewrecord.common.util.ResourceIds;
import com.interviewrecord.tracking.api.TrackingDtos.StatusRequest;
import com.interviewrecord.tracking.api.TrackingDtos.StatusResponse;
import com.interviewrecord.tracking.domain.PositionStatus;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class PositionStatusService {
    private final JpaManagedPositionStatusRepository statuses;
    private final JpaPositionRepository positions;
    private final Clock clock;

    public PositionStatusService(JpaManagedPositionStatusRepository statuses, JpaPositionRepository positions,
            Clock clock) {
        this.statuses = statuses; this.positions = positions; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StatusResponse> list(Long userId) {
        return statuses.findAllByUserIdOrderBySortOrderAsc(userId).stream()
                .map(status -> toResponse(status, positions.countByUserIdAndStatusId(userId, status.id())))
                .toList();
    }

    @Transactional
    public StatusResponse create(Long userId, StatusRequest request) {
        String name = request.name().trim();
        requireValidCategory(request.statisticsCategory());
        if (statuses.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("STATUS_NAME_TAKEN", "已存在同名状态");
        }
        int nextOrder = statuses.maxSortOrder(userId) + 1;
        PositionStatus saved = statuses.save(new PositionStatus(userId, name, nextOrder,
                request.color(), request.statisticsCategory(), clock.instant()));
        return toResponse(saved, 0);
    }

    @Transactional
    public StatusResponse update(Long userId, Long statusId, StatusRequest request) {
        PositionStatus status = requireOwned(userId, statusId);
        String name = request.name().trim();
        requireValidCategory(request.statisticsCategory());
        if (!name.equalsIgnoreCase(status.name()) && statuses.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("STATUS_NAME_TAKEN", "已存在同名状态");
        }
        boolean active = request.active() == null ? status.active() : request.active();
        status.update(name, request.color(), request.statisticsCategory(), active, clock.instant());
        return toResponse(status, positions.countByUserIdAndStatusId(userId, statusId));
    }

    /** Accepts the full ordered id list of the user and reassigns 1..n positions. */
    @Transactional
    public List<StatusResponse> reorder(Long userId, List<String> orderedIds) {
        Set<Long> requested = new LinkedHashSet<>();
        for (String id : orderedIds) {
            requested.add(ResourceIds.parse(id));
        }
        List<PositionStatus> owned = statuses.findAllByUserIdOrderBySortOrderAsc(userId);
        if (owned.size() != requested.size() || !owned.stream().map(PositionStatus::id).allMatch(requested::contains)) {
            throw new InvalidInputException("INVALID_STATUS_ORDER", "排序列表必须包含当前用户的全部状态");
        }
        Map<Long, PositionStatus> byId = new HashMap<>();
        owned.forEach(status -> byId.put(status.id(), status));
        Instant now = clock.instant();
        int order = 1;
        for (Long id : requested) {
            byId.get(id).assignOrder(order++, now);
        }
        return list(userId);
    }

    @Transactional
    public void delete(Long userId, Long statusId, String migrateToId) {
        PositionStatus status = requireOwned(userId, statusId);
        long usage = positions.countByUserIdAndStatusId(userId, statusId);
        if (usage > 0) {
            if (migrateToId == null || migrateToId.isBlank()) {
                throw new ConflictException("STATUS_IN_USE",
                        "该状态正在被 " + usage + " 个岗位使用，请先指定迁移目标状态");
            }
            Long targetId = ResourceIds.parse(migrateToId);
            if (targetId.equals(statusId)) {
                throw new InvalidInputException("INVALID_MIGRATION_TARGET", "迁移目标不能是被删除的状态");
            }
            PositionStatus target = requireOwned(userId, targetId);
            positions.findAllByUserIdAndArchivedOrderByUpdatedAtDesc(userId, false).stream()
                    .filter(position -> statusId.equals(position.statusId()))
                    .forEach(position -> position.changeStatus(target.id(), clock.instant()));
            positions.findAllByUserIdAndArchivedOrderByUpdatedAtDesc(userId, true).stream()
                    .filter(position -> statusId.equals(position.statusId()))
                    .forEach(position -> position.changeStatus(target.id(), clock.instant()));
        }
        statuses.delete(status);
    }

    private PositionStatus requireOwned(Long userId, Long statusId) {
        return statuses.findByIdAndUserId(statusId, userId).orElseThrow(NotFoundException::new);
    }

    private void requireValidCategory(String category) {
        if (!PositionStatus.STATISTICS_CATEGORIES.contains(category)) {
            throw new InvalidInputException("INVALID_STATISTICS_CATEGORY", "统计分类必须是 ACTIVE、SUCCESS、REJECTED 或 WITHDRAWN");
        }
    }

    private StatusResponse toResponse(PositionStatus status, long positionCount) {
        return new StatusResponse(Long.toString(status.id()), status.name(), status.sortOrder(),
                status.color(), status.statisticsCategory(), status.active(), positionCount);
    }
}
