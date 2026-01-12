package back.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ScheduleRegisteredEvent {
    private final Long clubId;
    private final Long scheduleId;
    private final String scheduleName;
}