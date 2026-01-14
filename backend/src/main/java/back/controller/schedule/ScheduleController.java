package back.controller.schedule;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.dto.schedule.ScheduleCreateRequest;
import back.dto.schedule.ScheduleResponse;
import back.exception.ClubAuthException;
import back.service.schedule.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/{clubId}/schedules")
    public SuccessResponse<List<ScheduleResponse>> getSchedules(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId
    ) {
        Long currentUserId = requireUserId(principal);
        List<ScheduleResponse> schedules = scheduleService.getSchedulesByClubId(clubId, currentUserId);
        return SuccessResponse.success(HttpStatus.OK, schedules);
    }

    @PostMapping("/{clubId}/schedules")
    public SuccessResponse<ScheduleResponse> createSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId,
            @RequestBody @jakarta.validation.Valid ScheduleCreateRequest request
    ) {
        Long currentUserId = requireUserId(principal);
        ScheduleResponse response = scheduleService.createSchedule(clubId, currentUserId, request);
        return SuccessResponse.success(HttpStatus.CREATED, response);
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) throw new ClubAuthException.LoginRequired();
        return principal.getUserId();
    }
}
