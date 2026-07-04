package com.filecabinet.document.service;

import com.filecabinet.category.model.Category;
import com.filecabinet.category.repository.CategoryRepository;
import com.filecabinet.shared.exception.ServiceExceptions;
import com.filecabinet.document.model.Document;
import com.filecabinet.document.model.DocumentStatus;
import com.filecabinet.document.model.DocumentType;
import com.filecabinet.document.repository.DocumentRepository;
import com.filecabinet.user.model.User;
import com.filecabinet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public Document create(String title, DocumentType documentType, String filePath, UUID ownerId, UUID categoryId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ServiceExceptions.NotFoundException("User not found: " + ownerId));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ServiceExceptions.NotFoundException("Category not found: " + categoryId));

        Document document = Document.builder()
                .title(title)
                .documentType(documentType)
                .filePath(filePath)
                .status(DocumentStatus.UPLOADED)
                .uploadedOn(LocalDateTime.now())
                .owner(owner)
                .category(category)
                .build();
        return documentRepository.save(document);
    }

    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    public List<Document> findByOwner(UUID ownerId) {
        return documentRepository.findByOwnerId(ownerId);
    }

    public List<Document> findByCategory(UUID categoryId) {
        return documentRepository.findByCategoryId(categoryId);
    }

    public Document findById(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ServiceExceptions.NotFoundException("Document not found: " + id));
    }

    public Document update(UUID id, String title, DocumentType documentType, String filePath, UUID categoryId) {
        Document document = findById(id);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ServiceExceptions.NotFoundException("Category not found: " + categoryId));

        document.setTitle(title);
        document.setDocumentType(documentType);
        document.setFilePath(filePath);
        document.setCategory(category);
        return documentRepository.save(document);
    }

    public Document updateStatus(UUID id, DocumentStatus status) {
        Document document = findById(id);
        document.setStatus(status);
        return documentRepository.save(document);
    }

    public void delete(UUID id) {
        if (!documentRepository.existsById(id)) {
            throw new ServiceExceptions.NotFoundException("Document not found: " + id);
        }
        documentRepository.deleteById(id);
    }
}
