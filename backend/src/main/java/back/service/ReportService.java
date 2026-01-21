package back.service;

import back.domain.Reports;
import back.dto.report.ReportCreateRequest;
import back.exception.AdminException;
import back.exception.response.ErrorCode;
import back.repository.ReportsRepository;
import back.repository.club.ClubRepository;
import back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportsRepository reportsRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createReport(Long clubId, Long reporterId, ReportCreateRequest request) {
        // 클럽 존재 확인
        if (!clubRepository.existsById(clubId)) {
            throw new AdminException(ErrorCode.RESOURCE_NOT_FOUND, "모임을 찾을 수 없습니다.");
        }
        
        // 신고 대상 존재 확인 (사용자)
        if (!userRepository.existsById(request.targetId())) {
            throw new AdminException(ErrorCode.USER_NOT_FOUND, "신고 대상 사용자를 찾을 수 없습니다.");
        }
        
        // 자기 자신 신고 방지
        if (reporterId.equals(request.targetId())) {
            throw new AdminException(ErrorCode.INVALID_INPUT, "자기 자신을 신고할 수 없습니다.");
        }
        
        Reports report = new Reports(
                clubId,
                reporterId,
                request.targetId(),
                request.reason(),
                request.photoUrl()
        );
        
        Reports saved = reportsRepository.save(report);
        return saved.getReportId();
    }
}
