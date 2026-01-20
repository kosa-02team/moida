package back.controller;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.dto.report.ReportCreateRequest;
import back.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs/{clubId}/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<SuccessResponse<Long>> createReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clubId,
            @Valid @RequestBody ReportCreateRequest request) {
        
        Long reporterId = principal.getUserId();
        Long reportId = reportService.createReport(clubId, reporterId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success(HttpStatus.CREATED, reportId));
    }
}
