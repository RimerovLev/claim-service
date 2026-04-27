package com.claims.mvp.claim.service.storage;

import com.claims.mvp.claim.dao.BoardingDocumentsRepository;
import com.claims.mvp.claim.dao.ClaimRepository;
import com.claims.mvp.claim.dto.request.DocumentUploadRequest;
import com.claims.mvp.claim.dto.response.DocumentResponse;
import com.claims.mvp.claim.mapper.DocumentMapper;
import com.claims.mvp.claim.model.BoardingDocuments;
import com.claims.mvp.claim.model.Claim;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentStorageServiceImpl implements DocumentStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final ClaimRepository claimRepository;
    private final BoardingDocumentsRepository boardingDocumentsRepository;
    private final DocumentMapper documentMapper;

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
    );

    private static final long MAX_FILE_SIZE = 1024 * 1024 * 5;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/jpg"
    );

    @Override
    @Transactional
    public DocumentResponse uploadDocument(DocumentUploadRequest request) throws IOException {
        validateFile(request.getFile());
        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new IllegalArgumentException("Claim not found with id: " + request.getClaimId()));

        Path uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String fileName = generateUniqueFileName(request.getFile().getContentType());
        Path filePath = uploadPath.resolve(fileName);

        BoardingDocuments document = new BoardingDocuments();
        document.setId(UUID.randomUUID().toString());
        document.setClaim(claim);
        document.setType(request.getType());
        document.setUrl("/api/documents/" + document.getId());
        document.setFileName(request.getFile().getOriginalFilename());
        document.setFileSize(request.getFile().getSize());
        document.setMimeType(request.getFile().getContentType());
        document.setDescription(request.getDescription());
        document.setStorageKey(fileName);
        document.setUploadedAt(OffsetDateTime.now());

        claim.getDocuments().add(document);

        try {
            request.getFile().transferTo(filePath.toFile());
            claimRepository.save(claim);
        } catch (IOException e) {
            Files.deleteIfExists(filePath);
            log.error("Failed to save file: {}", fileName, e);
            throw new IOException("Failed to save file: " + fileName, e);
        }

        return documentMapper.toResponse(document);
    }

    @Override
    public DocumentResponse getDocument(String id) throws IOException {
        BoardingDocuments document = boardingDocumentsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + id));
        Path resolvedPath = getSafePath(document.getStorageKey());
        if (!Files.exists(resolvedPath)) {
            log.warn("File not found: " + resolvedPath);
            throw new RuntimeException("File not found: " + resolvedPath);
        }
        return documentMapper.toResponse(document);
    }

    @Override
    public List<DocumentResponse> getDocumentsByClaimId(Long claimId) throws IOException {
        if (!claimRepository.existsById(claimId)) {
            throw new IllegalArgumentException("Claim not found with id: " + claimId);
        }
        return boardingDocumentsRepository.findAllByClaimId(claimId).stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentResponse downloadDocument(String id) throws IOException {
        BoardingDocuments document = boardingDocumentsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + id));
        Path resolvedPath = getSafePath(document.getStorageKey());
        if (!Files.exists(resolvedPath)) {
            log.warn("File not found for download: {}", id);
            throw new RuntimeException("File not found for download: " + id);
        }
        Resource resource = new FileSystemResource(resolvedPath);
        DocumentResponse response = documentMapper.toResponse(document);
        response.setResource(resource);
        return response;
    }

    @Override
    @Transactional
    public void deleteDocument(String id) throws IOException {
        BoardingDocuments document = boardingDocumentsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with id: " + id));
        Path resolvedPath = getSafePath(document.getStorageKey());
        if (Files.exists(resolvedPath)) {
            Files.delete(resolvedPath);
        }
        Claim claim = document.getClaim();
        claim.getDocuments().remove(document);
        claimRepository.save(claim);
    }

    @Override
    public void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File must not be larger than 5MB");
        }
        String contentType = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("File must be a PDF, JPG or PNG");
        }
    }

    private String detectMimeType(MultipartFile file) throws IOException {
        byte[] header;
        try (InputStream inputStream = file.getInputStream()) {
            header = inputStream.readNBytes(8);
        }
        for (Map.Entry<String, byte[]> entry : MAGIC_BYTES.entrySet()) {
            if (startsWith(header, entry.getValue())) return entry.getKey();
        }
        return "application/octet-stream";
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        return Arrays.mismatch(data, 0, prefix.length, prefix, 0, prefix.length) == -1;
    }

    private Path getSafePath(String storageKey) {
        Path baseUploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
        Path resolvedPath = baseUploadPath.resolve(storageKey).normalize();
        if (!resolvedPath.startsWith(baseUploadPath)) {
            throw new SecurityException("Invalid file path");
        }
        return resolvedPath;
    }

    private String generateUniqueFileName(String contentType) {
        String extension = switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new IllegalArgumentException("Unsupported MIME type: " + contentType);
        };
        return UUID.randomUUID() + extension;
    }
}
