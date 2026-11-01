package com.swiftdrop.courier.service;

import com.swiftdrop.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path rootLocation;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("image/jpeg", "image/png", "image/jpg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public FileStorageService(@Value("${swiftdrop.upload.dir:${SWIFTDROP_UPLOAD_DIR:uploads/proofs}}") String uploadDir) {
        Path targetPath = Paths.get(uploadDir);
        Path resolvedPath;
        try {
            Files.createDirectories(targetPath);
            resolvedPath = targetPath;
        } catch (Exception e) {
            log.warn("Could not initialize upload folder at {}. Falling back to system temp folder: {}", targetPath, e.getMessage());
            Path fallbackPath = Paths.get(System.getProperty("java.io.tmpdir"), "swiftdrop", "proofs");
            try {
                Files.createDirectories(fallbackPath);
                resolvedPath = fallbackPath;
            } catch (IOException ex) {
                throw new RuntimeException("Could not initialize upload folder at " + targetPath + " or fallback " + fallbackPath, ex);
            }
        }
        this.rootLocation = resolvedPath;
        log.info("FileStorageService initialized upload directory at: {}", this.rootLocation.toAbsolutePath());
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File exceeds maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_EXTENSIONS.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Only JPEG and PNG images are allowed");
        }

        try {
            String extension = contentType.contains("png") ? ".png" : ".jpg";
            String filename = UUID.randomUUID() + extension;
            Path destinationFile = this.rootLocation.resolve(filename).normalize().toAbsolutePath();

            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/proofs/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }
}