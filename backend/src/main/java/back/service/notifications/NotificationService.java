package back.service.notifications;

import back.repository.notifications.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmitterRepository emitterRepository;

    //구독 (Subscribe)
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(60L * 60 * 1000);
        String key = userId + "_" + System.currentTimeMillis();

        emitter.onCompletion(() -> emitterRepository.deleteById(key));
        emitter.onTimeout(() -> emitterRepository.deleteById(key));

        try {
            emitter.send(SseEmitter.event().name("test").data("success"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return emitterRepository.save(key, emitter);
    }

    //전송 (Send)
    public void send(Long userId, Object data) {
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterStartWithByUserId(userId);

        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification") // 클라이언트에서 수신할 이벤트 이름
                        .data(data));
            } catch (IOException e) {
                emitterRepository.deleteById(key);
            }
        });
    }
}