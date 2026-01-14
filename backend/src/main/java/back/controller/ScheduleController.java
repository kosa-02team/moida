package back.controller;

import back.common.response.SuccessResponse;
import back.dto.ScheduleCreateRequest;
import back.dto.ScheduleResponse;
import back.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/{clubId}/schedules")
    public SuccessResponse<List<ScheduleResponse>> getSchedules(
            @PathVariable("clubId") Long clubId
            // TODO: 실제 구현 시 @AuthenticationPrincipal 등으로 현재 로그인 유저 ID 주입
    ) {
        // 임시로 하드코딩, 나중에 인증 연동 시 교체
        Long currentUserId = 1L;
        List<ScheduleResponse> schedules = scheduleService.getSchedulesByClubId(clubId, currentUserId);
        return SuccessResponse.success(HttpStatus.OK, schedules);
    }

    @PostMapping("/{clubId}/schedules")
    public SuccessResponse<ScheduleResponse> createSchedule(
            @PathVariable("clubId") Long clubId,
            @RequestBody @jakarta.validation.Valid ScheduleCreateRequest request
            // TODO: 실제 구현 시 @AuthenticationPrincipal 등으로 현재 로그인 유저 ID 주입
    ) {
        // 임시로 하드코딩, 나중에 인증 연동 시 교체
        Long currentUserId = 1L;
        ScheduleResponse response = scheduleService.createSchedule(clubId, currentUserId, request);
        return SuccessResponse.success(HttpStatus.CREATED, response);
    }
}
