package back.controller.schedule;

import back.common.response.SuccessResponse;
import back.config.security.UserPrincipal;
import back.dto.schedule.*;
import back.exception.ClubException;
import back.service.schedule.ScheduleService;
import jakarta.validation.Valid;
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
            @RequestBody @Valid ScheduleCreateRequest request
    ) {
        Long currentUserId = requireUserId(principal);
        ScheduleResponse response = scheduleService.createSchedule(clubId, currentUserId, request);
        return SuccessResponse.success(HttpStatus.CREATED, response);
    }

    /**
     * 단일 일정 조회
     */
    @GetMapping("/{clubId}/schedules/{scheduleId}")
    public SuccessResponse<ScheduleResponse> getSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId,
            @PathVariable("scheduleId") Long scheduleId
    ) {
        Long currentUserId = requireUserId(principal);
        ScheduleResponse response = scheduleService.getScheduleById(clubId, scheduleId, currentUserId);
        return SuccessResponse.success(HttpStatus.OK, response);
    }

    /**
     * 일정 수정
     */
    @PutMapping("/{clubId}/schedules/{scheduleId}")
    public SuccessResponse<ScheduleResponse> updateSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestBody @Valid ScheduleUpdateRequest request
    ) {
        Long currentUserId = requireUserId(principal);
        ScheduleResponse response = scheduleService.updateSchedule(clubId, scheduleId, currentUserId, request);
        return SuccessResponse.success(HttpStatus.OK, response);
    }

    /**
     * 일정 마감
     */
    @PostMapping("/{clubId}/schedules/{scheduleId}/close")
    public SuccessResponse<Void> closeSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId,
            @PathVariable("scheduleId") Long scheduleId
    ) {
        Long currentUserId = requireUserId(principal);
        scheduleService.closeSchedule(clubId, scheduleId, currentUserId);
        return SuccessResponse.success(HttpStatus.OK);
    }

    /**
     * 일정 취소
     */
    @PostMapping("/{clubId}/schedules/{scheduleId}/cancel")
    public SuccessResponse<Void> cancelSchedule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestBody(required = false) ScheduleCancelRequest request
    ) {
        Long currentUserId = requireUserId(principal);
        scheduleService.cancelSchedule(clubId, scheduleId, currentUserId, request);
        return SuccessResponse.success(HttpStatus.OK);
    }

    /**
     * 정산 정보 수정
     */
    @PutMapping("/{clubId}/schedules/{scheduleId}/settlement")
    public SuccessResponse<ScheduleResponse> updateSettlement(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestBody @Valid ScheduleSettlementRequest request
    ) {
        Long currentUserId = requireUserId(principal);
        ScheduleResponse response = scheduleService.updateSettlement(clubId, scheduleId, currentUserId, request);
        return SuccessResponse.success(HttpStatus.OK, response);
    }

    /**
     * 일정 참여자 목록 조회
     */
    @GetMapping("/{clubId}/schedules/{scheduleId}/participants")
    public SuccessResponse<List<ScheduleParticipantResponse>> getScheduleParticipants(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("clubId") Long clubId,
            @PathVariable("scheduleId") Long scheduleId
    ) {
        Long currentUserId = requireUserId(principal);
        List<ScheduleParticipantResponse> participants = scheduleService.getScheduleParticipants(clubId, scheduleId, currentUserId);
        return SuccessResponse.success(HttpStatus.OK, participants);
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) throw new ClubException.AuthLoginRequired();
        return principal.getUserId();
    }
}
