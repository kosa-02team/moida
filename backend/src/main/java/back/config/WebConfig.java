package back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${custom.upload.path:uploads/images/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // uploads 디렉토리 경로 absolute로 변환
        Path path = Paths.get(uploadPath).toAbsolutePath().normalize();
        // Windows 경로의 백슬래시를 슬래시로 변환
        String absolutePath = "file:///" + path.toString().replace("\\", "/") + "/";

        System.out.println("📁 Static resource mapping: /uploads/images/** -> " + absolutePath);
        
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(absolutePath);
    }
}
