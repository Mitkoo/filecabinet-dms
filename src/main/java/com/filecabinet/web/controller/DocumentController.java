package com.filecabinet.web.controller;

import com.filecabinet.category.service.CategoryService;
import com.filecabinet.document.model.Document;
import com.filecabinet.document.model.DocumentStatus;
import com.filecabinet.document.model.DocumentType;
import com.filecabinet.document.service.DocumentService;
import com.filecabinet.user.model.Role;
import com.filecabinet.user.model.User;
import com.filecabinet.user.service.UserService;
import com.filecabinet.web.dto.DocumentFieldForm;
import com.filecabinet.web.dto.UploadForm;
import com.filecabinet.web.interceptor.SessionAuthInterceptor;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final CategoryService categoryService;
    private final UserService userService;

    @GetMapping("/documents")
    public String list(@RequestParam(required = false) String q,
                        @RequestParam(required = false) DocumentType type,
                        @RequestParam(required = false) UUID categoryId,
                        @RequestParam(required = false) DocumentStatus status,
                        HttpSession session, Model model) {
        User currentUser = currentUser(session);

        List<Document> documents = currentUser.getRole() == Role.ADMIN
                ? documentService.findAll()
                : documentService.findByOwner(currentUser.getId());

        documents = documents.stream()
                .filter(doc -> q == null || q.isBlank() || doc.getTitle().toLowerCase().contains(q.toLowerCase()))
                .filter(doc -> type == null || doc.getDocumentType() == type)
                .filter(doc -> categoryId == null || doc.getCategory().getId().equals(categoryId))
                .filter(doc -> status == null || doc.getStatus() == status)
                .toList();

        model.addAttribute("documents", documents);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }

    @GetMapping("/documents/{id}")
    public String detail(@PathVariable UUID id, HttpSession session, Model model) {
        User currentUser = currentUser(session);
        Document document = documentService.findById(id);

        model.addAttribute("document", document);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("currentUser", currentUser);
        if (!model.containsAttribute("editForm")) {
            model.addAttribute("editForm", toEditForm(document));
        }
        if (!model.containsAttribute("fieldForm")) {
            model.addAttribute("fieldForm", new DocumentFieldForm());
        }
        model.addAttribute("fields", documentService.findFields(id));
        return "document-detail";
    }

    @PostMapping("/documents/{id}/edit")
    public String edit(@PathVariable UUID id, @Valid @ModelAttribute("editForm") UploadForm form, BindingResult bindingResult,
                        HttpSession session, Model model) {
        if (bindingResult.hasErrors()) {
            return detail(id, session, model);
        }

        documentService.update(id, form.getTitle(), form.getDocumentType(), documentService.findById(id).getFilePath(), form.getCategoryId());
        return "redirect:/documents/" + id;
    }

    @PostMapping("/documents/{id}/status")
    public String updateStatus(@PathVariable UUID id, @RequestParam DocumentStatus status,
                                HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = currentUser(session);
        if (currentUser.getRole() != Role.ADMIN) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only admins can approve or reject documents.");
            return "redirect:/documents/" + id;
        }

        documentService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Document " + status.name().toLowerCase() + ".");
        return "redirect:/documents/" + id;
    }

    @PostMapping("/documents/{id}/fields")
    public String addField(@PathVariable UUID id, @Valid @ModelAttribute("fieldForm") DocumentFieldForm form, BindingResult bindingResult,
                            HttpSession session, Model model) {
        if (bindingResult.hasErrors()) {
            return detail(id, session, model);
        }

        documentService.addField(id, form.getFieldName(), form.getFieldValue());
        return "redirect:/documents/" + id;
    }

    @PostMapping("/documents/{id}/fields/{fieldId}/delete")
    public String deleteField(@PathVariable UUID id, @PathVariable UUID fieldId, RedirectAttributes redirectAttributes) {
        documentService.removeField(id, fieldId);
        redirectAttributes.addFlashAttribute("successMessage", "Field removed.");
        return "redirect:/documents/" + id;
    }

    @PostMapping("/documents/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        documentService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Document deleted.");
        return "redirect:/documents";
    }

    @GetMapping("/documents/new")
    public String newDocumentForm(HttpSession session, Model model) {
        if (!model.containsAttribute("uploadForm")) {
            model.addAttribute("uploadForm", new UploadForm());
        }
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("currentUser", currentUser(session));
        model.addAttribute("activePage", "upload");
        return "upload";
    }

    @PostMapping("/documents")
    public String upload(@Valid @ModelAttribute("uploadForm") UploadForm form, BindingResult bindingResult,
                          @RequestParam("file") MultipartFile file, HttpSession session, Model model,
                          RedirectAttributes redirectAttributes) {
        User currentUser = currentUser(session);

        if (file.isEmpty()) {
            bindingResult.reject("file.required", "Please choose a file to upload.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activePage", "upload");
            return "upload";
        }

        String filePath = storeFile(file);
        documentService.create(form.getTitle(), form.getDocumentType(), filePath, currentUser.getId(), form.getCategoryId());

        redirectAttributes.addFlashAttribute("successMessage", "Document uploaded successfully.");
        return "redirect:/documents";
    }

    private UploadForm toEditForm(Document document) {
        UploadForm editForm = new UploadForm();
        editForm.setTitle(document.getTitle());
        editForm.setDocumentType(document.getDocumentType());
        editForm.setCategoryId(document.getCategory().getId());
        return editForm;
    }

    private String storeFile(MultipartFile file) {
        try {
            Path uploadDir = Paths.get("uploads");
            Files.createDirectories(uploadDir);
            String storedName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path target = uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store uploaded file", ex);
        }
    }

    private User currentUser(HttpSession session) {
        UUID userId = (UUID) session.getAttribute(SessionAuthInterceptor.SESSION_USER_ID);
        return userService.findById(userId);
    }
}
