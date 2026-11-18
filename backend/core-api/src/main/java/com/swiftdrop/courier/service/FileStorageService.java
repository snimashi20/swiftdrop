package com.swiftdrop.courier.service;

import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
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
        String filename = validateAndSave(file, this.rootLocation);
        return "/uploads/proofs/" + filename;
    }

    /**
     * Reuses the same 5MB / JPEG-PNG / UUID rules to store KYC verification documents (e.g., NIC front/back).
     */
    public String storeDocument(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Document image is required");
        }
        if (category == null || !category.matches("^[a-z0-9_-]+$")) {
            throw new BadRequestException("Invalid document category");
        }
        try {
            Path categoryDir = this.rootLocation.resolve(category).normalize().toAbsolutePath();
            if (!categoryDir.startsWith(this.rootLocation.toAbsolutePath())) {
                throw new BadRequestException("Invalid storage path");
            }
            Files.createDirectories(categoryDir);
            String filename = validateAndSave(file, categoryDir);
            return "/api/v1/courier/documents/" + category + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store document: " + e.getMessage(), e);
        }
    }

    /**
     * Loads a stored KYC document safely with path-traversal protection.
     */
    public Resource loadDocument(String category, String filename) {
        if (category == null || !category.matches("^[a-z0-9_-]+$")
                || filename == null || !filename.matches("^[a-zA-Z0-9._-]+$")) {
            throw new BadRequestException("Invalid document path");
        }
        Path categoryDir = this.rootLocation.resolve(category).normalize().toAbsolutePath();
        Path filePath = categoryDir.resolve(filename).normalize().toAbsolutePath();
        if (!filePath.startsWith(categoryDir) || !Files.exists(filePath)) {
            throw new NotFoundException("Document not found");
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("Document not readable");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("Document not found");
        }
    }

    private String validateAndSave(MultipartFile file, Path targetDirectory) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File exceeds maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_EXTENSIONS.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Only JPEG and PNG images are allowed");
        }

        try {
            String extension = contentType.toLowerCase().contains("png") ? ".png" : ".jpg";
            String filename = UUID.randomUUID() + extension;
            Path destinationFile = targetDirectory.resolve(filename).normalize().toAbsolutePath();
            if (!destinationFile.startsWith(targetDirectory.toAbsolutePath())) {
                throw new BadRequestException("Invalid file destination");
            }
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }
}