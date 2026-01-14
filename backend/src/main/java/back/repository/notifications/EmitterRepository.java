package back.repository.notifications;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class EmitterRepository {

    Map<String, SseEmitter> connection = new ConcurrentHashMap<>();

    //SSE 이벤트를 연결할 통로 저장
    public SseEmitter save(String key, SseEmitter emitter) {
        connection.put(key, emitter);
        return emitter;
    }

    public void deleteById(String key) {
        connection.remove(key);
    }

    public Map<String, SseEmitter> findAllEmitterStartWithByUserId(Long userId) {
        Map<String, SseEmitter> result = new ConcurrentHashMap<>();

        for (Map.Entry<String, SseEmitter> entry : connection.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(userId + "_")) {
                result.put(key, entry.getValue());
            }
        }

        return result;
    }
}
