package com.filecabinet.shared.config;

import com.filecabinet.category.model.Category;
import com.filecabinet.category.service.CategoryService;
import com.filecabinet.document.model.Document;
import com.filecabinet.document.model.DocumentStatus;
import com.filecabinet.document.model.DocumentType;
import com.filecabinet.document.service.DocumentService;
import com.filecabinet.user.model.Role;
import com.filecabinet.user.model.User;
import com.filecabinet.user.repository.UserRepository;
import com.filecabinet.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SampleDataGenerator implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final DocumentService documentService;

    @Override
    public void run(String... args) {
        User admin = userRepository.findByUsername("jane.doe").orElseGet(() -> {
            User created = userService.register("jane.doe", "jane.doe@company.com", "password123");
            created.setRole(Role.ADMIN);
            return userRepository.save(created);
        });

        User clerk = userRepository.findByUsername("mike.chen")
                .orElseGet(() -> userService.register("mike.chen", "mike.chen@company.com", "password123"));

        userRepository.findByUsername("dimi").orElseGet(() -> {
            User created = userService.register("dimi", "dimi@company.com", "pass123");
            created.setRole(Role.ADMIN);
            return userRepository.save(created);
        });

        if (categoryService.findAll().isEmpty()) {
            List<Category> categories = List.of(
                    categoryService.create("Vendor Invoices", "Invoices received from suppliers and vendors"),
                    categoryService.create("Lease Agreements", "Office and equipment lease contracts"),
                    categoryService.create("Vendor Contracts", "Signed agreements with vendors and contractors"),
                    categoryService.create("Office Costs", "Receipts for general office expenses")
            );

            if (documentService.findAll().isEmpty()) {
                Document invoice = seedDocument("Q3 Vendor Invoice — Acme Corp", DocumentType.INVOICE, categories.get(0), admin);
                documentService.addField(invoice.getId(), "Vendor", "Acme Corp");
                documentService.addField(invoice.getId(), "Amount Due", "4,250.00 USD");
                documentService.addField(invoice.getId(), "Due Date", "2026-08-15");
                documentService.updateStatus(invoice.getId(), DocumentStatus.APPROVED);

                seedDocument("Lease Agreement — Downtown Office", DocumentType.CONTRACT, categories.get(1), clerk);
                seedDocument("Consulting Agreement — Nova LLC", DocumentType.CONTRACT, categories.get(2), admin);
                seedDocument("Receipt — Office Supplies", DocumentType.RECEIPT, categories.get(3), clerk);
                seedDocument("Employment Contract — J. Smith", DocumentType.LEGAL_FILING, categories.get(1), admin);
            }
        }
    }

    private Document seedDocument(String title, DocumentType type, Category category, User owner) {
        return documentService.create(title, type, "sample-files/" + UUID.randomUUID() + ".pdf", owner.getId(), category.getId());
    }
}
