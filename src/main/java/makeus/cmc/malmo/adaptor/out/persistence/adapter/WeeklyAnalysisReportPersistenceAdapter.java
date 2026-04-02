package makeus.cmc.malmo.adaptor.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.weekly_analysis_report.WeeklyAnalysisReportEntity;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.WeeklyAnalysisReportMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.weekly_analysis_report.WeeklyAnalysisReportRepository;
import makeus.cmc.malmo.application.port.out.weekly_analysis_report.LoadWeeklyAnalysisReportPort;
import makeus.cmc.malmo.application.port.out.weekly_analysis_report.SaveWeeklyAnalysisReportPort;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class WeeklyAnalysisReportPersistenceAdapter
        implements LoadWeeklyAnalysisReportPort, SaveWeeklyAnalysisReportPort {

    private final WeeklyAnalysisReportRepository weeklyAnalysisReportRepository;
    private final WeeklyAnalysisReportMapper weeklyAnalysisReportMapper;

    @Override
    public Optional<WeeklyAnalysisReport> loadById(Long weeklyAnalysisReportId) {
        return weeklyAnalysisReportRepository.findById(weeklyAnalysisReportId)
                .map(weeklyAnalysisReportMapper::toDomain);
    }

    @Override
    public Optional<WeeklyAnalysisReport> loadByMemberIdAndWeekStartDate(MemberId memberId, LocalDate weekStartDate) {
        return weeklyAnalysisReportRepository.findByMemberId_ValueAndWeekStartDate(memberId.getValue(), weekStartDate)
                .map(weeklyAnalysisReportMapper::toDomain);
    }

    @Override
    public Optional<WeeklyAnalysisReport> loadLatestByMemberId(MemberId memberId) {
        return weeklyAnalysisReportRepository.findTopByMemberId_ValueOrderByWeekStartDateDesc(memberId.getValue())
                .map(weeklyAnalysisReportMapper::toDomain);
    }

    @Override
    public Optional<WeeklyAnalysisReport> loadLatestByMemberIdAndStatuses(
            MemberId memberId,
            Collection<WeeklyAnalysisReportStatus> statuses
    ) {
        return weeklyAnalysisReportRepository.findTopByMemberId_ValueAndStatusInOrderByWeekStartDateDesc(
                        memberId.getValue(),
                        statuses
                )
                .map(weeklyAnalysisReportMapper::toDomain);
    }

    @Override
    public List<WeeklyAnalysisReport> loadPublishedReportsByMemberId(MemberId memberId) {
        return weeklyAnalysisReportRepository.findAllByMemberId_ValueAndStatusOrderByWeekStartDateDesc(
                        memberId.getValue(),
                        WeeklyAnalysisReportStatus.PUBLISHED
                ).stream()
                .map(weeklyAnalysisReportMapper::toDomain)
                .toList();
    }

    @Override
    public WeeklyAnalysisReport saveWeeklyAnalysisReport(WeeklyAnalysisReport weeklyAnalysisReport) {
        WeeklyAnalysisReportEntity entity = getEntityForSave(weeklyAnalysisReport);
        WeeklyAnalysisReportEntity savedEntity = weeklyAnalysisReportRepository.save(entity);
        return weeklyAnalysisReportMapper.toDomain(savedEntity);
    }

    @Override
    public WeeklyAnalysisReport saveAndFlushWeeklyAnalysisReport(WeeklyAnalysisReport weeklyAnalysisReport) {
        WeeklyAnalysisReportEntity entity = getEntityForSave(weeklyAnalysisReport);
        WeeklyAnalysisReportEntity savedEntity = weeklyAnalysisReportRepository.saveAndFlush(entity);
        return weeklyAnalysisReportMapper.toDomain(savedEntity);
    }

    @Override
    public boolean markAsGeneratingIfUpdatable(Long weeklyAnalysisReportId) {
        return weeklyAnalysisReportRepository.updateStatusIfCurrentStatusIn(
                weeklyAnalysisReportId,
                List.of(WeeklyAnalysisReportStatus.PENDING, WeeklyAnalysisReportStatus.FAILED),
                WeeklyAnalysisReportStatus.GENERATING
        ) > 0;
    }

    private WeeklyAnalysisReportEntity getEntityForSave(WeeklyAnalysisReport weeklyAnalysisReport) {
        if (weeklyAnalysisReport.getId() == null) {
            return weeklyAnalysisReportMapper.toEntity(weeklyAnalysisReport);
        }

        return weeklyAnalysisReportRepository.findById(weeklyAnalysisReport.getId())
                .map(existingEntity -> {
                    existingEntity.apply(weeklyAnalysisReport);
                    return existingEntity;
                })
                .orElseGet(() -> weeklyAnalysisReportMapper.toEntity(weeklyAnalysisReport));
    }
}
