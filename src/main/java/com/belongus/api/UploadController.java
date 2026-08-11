package com.belongus.api;

import com.belongus.domain.AppUser;
import com.belongus.domain.Memory;
import com.belongus.repository.MemoryRepository;
import com.belongus.service.AuthService;
import com.belongus.service.SpaceAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private final MemoryRepository memoryRepository;
    private final AuthService authService;
    private final SpaceAccessService spaceAccessService;

    @Value("${app.storage.location:uploads}")
    private String storageLocation;

    public UploadController(
            MemoryRepository memoryRepository,
            AuthService authService,
            SpaceAccessService spaceAccessService
    ) {
        this.memoryRepository = memoryRepository;
        this.authService = authService;
        this.spaceAccessService = spaceAccessService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> upload(
            @RequestParam Long spaceId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) throws IOException {
        AppUser user = authService.requireUser(request);
        spaceAccessService.requireMemberSpace(spaceId, user);
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("请选择一张图片");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("图片不能超过 10MB");
        }

        Path directory = Path.of(storageLocation).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        Files.copy(file.getInputStream(), directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return Map.of("url", "/api/uploads/" + filename);
    }

    @GetMapping("/{filename:.+}")
    @Transactional(readOnly = true)
    public ResponseEntity<FileSystemResource> download(@PathVariable String filename, HttpServletRequest request) {
        AppUser user = authService.requireUser(request);
        String imageUrl = "/api/uploads/" + filename;
        Memory memory = memoryRepository.findFirstByImageUrl(imageUrl)
                .orElseThrow(() -> new IllegalArgumentException("没有找到这张图片"));
        spaceAccessService.requireMemberSpace(memory.getSpace().getId(), user);

        Path file = Path.of(storageLocation).toAbsolutePath().normalize().resolve(filename).normalize();
        if (!file.startsWith(Path.of(storageLocation).toAbsolutePath().normalize()) || !Files.exists(file)) {
            throw new IllegalArgumentException("这张图片已不存在");
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(file.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .contentType(mediaType)
                .body(new FileSystemResource(file));
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        return extension.matches("\\.(jpg|jpeg|png|webp|gif)") ? extension : ".jpg";
    }
}
