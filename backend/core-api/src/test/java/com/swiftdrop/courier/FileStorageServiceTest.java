package com.swiftdrop.courier;

import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.courier.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("Null or empty optional photo returns null")
    void store_nullOrEmptyFile_returnsNull() {
        assertNull(fileStorageService.store(null));

        MockMultipartFile emptyFile = new MockMultipartFile("photo", "empty.jpg", "image/jpeg", new byte[0]);
        assertNull(fileStorageService.store(emptyFile));
    }

    @Test
    @DisplayName("Valid JPEG and PNG files are stored and return proof path")
    void store_validImages_savesFileAndReturnsPath() throws Exception {
        MockMultipartFile jpeg = new MockMultipartFile("photo", "proof.jpg", "image/jpeg", "fake-jpeg".getBytes());
        String jpegPath = fileStorageService.store(jpeg);
        assertNotNull(jpegPath);
        assertTrue(jpegPath.startsWith("/uploads/proofs/"));
        assertTrue(jpegPath.endsWith(".jpg"));
        assertEquals(1L, Files.list(tempDir).count());

        MockMultipartFile png = new MockMultipartFile("photo", "proof.png", "image/png", "fake-png".getBytes());
        String pngPath = fileStorageService.store(png);
        assertNotNull(pngPath);
        assertTrue(pngPath.endsWith(".png"));
        assertEquals(2L, Files.list(tempDir).count());
    }

    @Test
    @DisplayName("Files larger than 5MB throw BadRequestException")
    void store_fileExceeding5Mb_throwsBadRequest() {
        byte[] oversized = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("photo", "big.jpg", "image/jpeg", oversized);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> fileStorageService.store(file));
        assertEquals("File exceeds maximum allowed size of 5MB", ex.getMessage());
    }

    @Test
    @DisplayName("Unsupported MIME types (e.g. PDF, text) throw BadRequestException")
    void store_disallowedMimeType_throwsBadRequest() {
        MockMultipartFile pdf = new MockMultipartFile("photo", "doc.pdf", "application/pdf", "%PDF-1.4".getBytes());
        BadRequestException pdfEx = assertThrows(BadRequestException.class, () -> fileStorageService.store(pdf));
        assertEquals("Only JPEG and PNG images are allowed", pdfEx.getMessage());

        MockMultipartFile text = new MockMultipartFile("photo", "script.txt", "text/plain", "hello".getBytes());
        assertThrows(BadRequestException.class, () -> fileStorageService.store(text));
    }
}