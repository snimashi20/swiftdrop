package com.swiftdrop.courier.service;

import com.swiftdrop.common.exception.BadRequestException;
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

    private final Path rootLocation;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("image/jpeg", "image/png", "image/jpg");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public FileStorageService(@Value("${swiftdrop.upload.dir:uploads/proofs}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload folder", e);
        }
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