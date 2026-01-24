package back.service.post;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class ImageService {

    @Value("${custom.upload.path:uploads/images/}")
    private String uploadPath;

    /**
     * Base64 데이터를 파일로 저장하고 접근 가능한 URL 경로를 반환합니다.
     * 
     * @param base64Data "data:image/jpeg;base64,..." 형식의 데이터
     * @return "/uploads/images/uuid.jpg" 형식의 경로. 이미 URL 형식이면 그대로 반환.
     */
    public String saveBase64Image(String base64Data) {
        if (base64Data == null || !base64Data.startsWith("data:image")) {
            return base64Data; // 이미 URL이거나 잘못된 형식이면 그대로 반환
        }

        try {
            String[] parts = base64Data.split(",");
            if (parts.length < 2)
                return base64Data;

            String header = parts[0];
            String base64Content = parts[1];

            // 확장자 추출
            String extension = "jpg";
            if (header.contains("image/png"))
                extension = "png";
            else if (header.contains("image/gif"))
                extension = "gif";
            else if (header.contains("image/webp"))
                extension = "webp";

            byte[] imageBytes = Base64.getDecoder().decode(base64Content);

            // 저장 디렉토리 생성
            Path directory = Paths.get(uploadPath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // 파일명 생성
            String fileName = UUID.randomUUID() + "." + extension;
            Path filePath = directory.resolve(fileName);

            // 파일 쓰기
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(imageBytes);
            }

            log.info("Image saved to: {}", filePath.toAbsolutePath());

            // 웹에서 접근 가능한 경로 반환
            return "/uploads/images/" + fileName;

        } catch (Exception e) {
            log.error("Failed to save base64 image", e);
            return base64Data; // 실패 시 원본 반환 (DB에서 터질 수 있음)
        }
    }
}
