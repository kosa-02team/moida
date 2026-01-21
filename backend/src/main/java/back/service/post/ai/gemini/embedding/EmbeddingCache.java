package back.service.post.ai.gemini.embedding;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EmbeddingCache {

    private static final int MAX_SIZE = 500;

    private final Map<String, float[]> cache =
            Collections.synchronizedMap(
                    new LinkedHashMap<>(16, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                            return size() > MAX_SIZE;
                        }
                    }
            );

    public float[] get(String query) {
        return cache.get(query);
    }

    public void put(String query, float[] embedding) {
        cache.put(query, embedding);
    }
}
