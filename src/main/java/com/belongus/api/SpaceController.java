package com.belongus.api;

import com.belongus.domain.AppUser;
import com.belongus.domain.CoupleSpace;
import com.belongus.domain.Letter;
import com.belongus.domain.LetterReply;
import com.belongus.domain.Memory;
import com.belongus.domain.SpaceMember;
import com.belongus.repository.LetterRepository;
import com.belongus.repository.MemoryRepository;
import com.belongus.repository.SpaceMemberRepository;
import com.belongus.service.AuthService;
import com.belongus.service.SpaceAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class SpaceController {
    private final MemoryRepository memoryRepository;
    private final LetterRepository letterRepository;
    private final SpaceMemberRepository memberRepository;
    private final AuthService authService;
    private final SpaceAccessService spaceAccessService;

    @Value("${app.storage.location:uploads}")
    private String storageLocation;

    public SpaceController(
            MemoryRepository memoryRepository,
            LetterRepository letterRepository,
            SpaceMemberRepository memberRepository,
            AuthService authService,
            SpaceAccessService spaceAccessService
    ) {
        this.memoryRepository = memoryRepository;
        this.letterRepository = letterRepository;
        this.memberRepository = memberRepository;
        this.authService = authService;
        this.spaceAccessService = spaceAccessService;
    }

    @GetMapping("/bootstrap")
    @Transactional(readOnly = true)
    public BootstrapResponse bootstrap(HttpServletRequest request) {
        AppUser user = authService.requireUser(request);
        AuthController.AuthResponse userResponse = new AuthController.AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName()
        );
        SpaceMember membership = memberRepository.findByUserIdOrderByJoinedAtDesc(user.getId()).stream()
                .findFirst()
                .orElse(null);
        if (membership == null) {
            return new BootstrapResponse(userResponse, null, List.of(), List.of());
        }

        CoupleSpace space = membership.getSpace();
        List<MemoryResponse> memories = memoryRepository.findBySpaceIdOrderByOccurredOnDescCreatedAtDesc(space.getId())
                .stream().map(this::toMemoryResponse).toList();
        List<LetterResponse> letters = letterRepository.findBySpaceIdOrderByCreatedAtDesc(space.getId())
                .stream().map(this::toLetterResponse).toList();
        return new BootstrapResponse(userResponse, toSpaceResponse(space), memories, letters);
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(@RequestParam Long spaceId, HttpServletRequest request) {
        AppUser user = authService.requireUser(request);
        CoupleSpace space = spaceAccessService.requireMemberSpace(spaceId, user);
        List<MemoryResponse> memories = memoryRepository.findBySpaceIdOrderByOccurredOnDescCreatedAtDesc(spaceId)
                .stream().map(this::toMemoryResponse).toList();
        List<LetterResponse> letters = letterRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId)
                .stream().map(this::toLetterResponse).toList();
        return new DashboardResponse(toSpaceResponse(space), memories, letters);
    }

    @GetMapping("/memories")
    public List<MemoryResponse> memories(@RequestParam Long spaceId, HttpServletRequest request) {
        AppUser user = authService.requireUser(request);
        spaceAccessService.requireMemberSpace(spaceId, user);
        return memoryRepository.findBySpaceIdOrderByOccurredOnDescCreatedAtDesc(spaceId)
                .stream().map(this::toMemoryResponse).toList();
    }

    @PostMapping("/memories")
    @ResponseStatus(HttpStatus.CREATED)
    public MemoryResponse createMemory(
            @RequestParam Long spaceId,
            @Valid @RequestBody MemoryRequest request,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        CoupleSpace space = spaceAccessService.requireMemberSpace(spaceId, user);
        Memory memory = new Memory(
                space,
                request.title().trim(),
                request.content().trim(),
                blankToNull(request.imageUrl()),
                user.getDisplayName(),
                request.occurredOn()
        );
        return toMemoryResponse(memoryRepository.save(memory));
    }

    @PatchMapping("/memories/{memoryId}")
    @Transactional
    public MemoryResponse updateMemory(
            @PathVariable Long memoryId,
            @RequestParam Long spaceId,
            @Valid @RequestBody MemoryRequest request,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        CoupleSpace space = spaceAccessService.requireMemberSpace(spaceId, user);
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new IllegalArgumentException("这条回忆已经不在这里了"));
        if (!memory.getSpace().getId().equals(space.getId())) {
            throw new ForbiddenException("这条回忆不属于当前空间");
        }
        String previousImageUrl = memory.getImageUrl();
        String nextImageUrl = blankToNull(request.imageUrl());
        memory.update(
                request.title().trim(),
                request.content().trim(),
                nextImageUrl,
                request.occurredOn()
        );
        MemoryResponse response = toMemoryResponse(memoryRepository.save(memory));
        if (!Objects.equals(previousImageUrl, nextImageUrl)) {
            deleteStoredImageIfUnused(previousImageUrl);
        }
        return response;
    }

    @DeleteMapping("/memories/{memoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteMemory(
            @PathVariable Long memoryId,
            @RequestParam Long spaceId,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        CoupleSpace space = spaceAccessService.requireMemberSpace(spaceId, user);
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new IllegalArgumentException("这条回忆已经不在这里了"));
        if (!memory.getSpace().getId().equals(space.getId())) {
            throw new ForbiddenException("这条回忆不属于当前空间");
        }
        String imageUrl = memory.getImageUrl();
        memoryRepository.delete(memory);
        memoryRepository.flush();
        deleteStoredImageIfUnused(imageUrl);
    }

    @GetMapping("/letters")
    @Transactional(readOnly = true)
    public List<LetterResponse> letters(@RequestParam Long spaceId, HttpServletRequest request) {
        AppUser user = authService.requireUser(request);
        spaceAccessService.requireMemberSpace(spaceId, user);
        return letterRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId)
                .stream().map(this::toLetterResponse).toList();
    }

    @PostMapping("/letters")
    @ResponseStatus(HttpStatus.CREATED)
    public LetterResponse createLetter(
            @RequestParam Long spaceId,
            @Valid @RequestBody LetterRequest request,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        CoupleSpace space = spaceAccessService.requireMemberSpace(spaceId, user);
        Letter letter = new Letter(
                space,
                user.getDisplayName(),
                spaceAccessService.partnerName(space, user),
                request.content().trim()
        );
        return toLetterResponse(letterRepository.save(letter));
    }

    @PatchMapping("/letters/{letterId}")
    @Transactional
    public LetterResponse updateLetter(
            @PathVariable Long letterId,
            @RequestParam Long spaceId,
            @Valid @RequestBody LetterRequest request,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        Letter letter = requireLetterInSpace(letterId, spaceId, user);
        letter.updateContent(request.content().trim());
        return toLetterResponse(letterRepository.save(letter));
    }

    @DeleteMapping("/letters/{letterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteLetter(
            @PathVariable Long letterId,
            @RequestParam Long spaceId,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        Letter letter = requireLetterInSpace(letterId, spaceId, user);
        letterRepository.delete(letter);
    }

    @PostMapping("/letters/{letterId}/replies")
    @Transactional
    public LetterResponse reply(
            @PathVariable Long letterId,
            @RequestParam Long spaceId,
            @Valid @RequestBody ReplyRequest request,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        Letter letter = requireLetterInSpace(letterId, spaceId, user);
        letter.addReply(new LetterReply(letter, user.getDisplayName(), request.content().trim()));
        return toLetterResponse(letterRepository.save(letter));
    }

    @PatchMapping("/letters/{letterId}/replies/{replyId}")
    @Transactional
    public LetterResponse updateReply(
            @PathVariable Long letterId,
            @PathVariable Long replyId,
            @RequestParam Long spaceId,
            @Valid @RequestBody ReplyRequest request,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        Letter letter = requireLetterInSpace(letterId, spaceId, user);
        LetterReply reply = letter.findReply(replyId);
        reply.updateContent(request.content().trim());
        return toLetterResponse(letterRepository.save(letter));
    }

    @DeleteMapping("/letters/{letterId}/replies/{replyId}")
    @Transactional
    public LetterResponse deleteReply(
            @PathVariable Long letterId,
            @PathVariable Long replyId,
            @RequestParam Long spaceId,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        Letter letter = requireLetterInSpace(letterId, spaceId, user);
        letter.removeReply(letter.findReply(replyId));
        return toLetterResponse(letterRepository.save(letter));
    }

    private Letter requireLetterInSpace(Long letterId, Long spaceId, AppUser user) {
        CoupleSpace space = spaceAccessService.requireMemberSpace(spaceId, user);
        Letter letter = letterRepository.findById(letterId)
                .orElseThrow(() -> new IllegalArgumentException("这封信已经不在这里了"));
        if (!letter.getSpace().getId().equals(space.getId())) {
            throw new ForbiddenException("这封信不属于当前空间");
        }
        return letter;
    }

    private void deleteStoredImageIfUnused(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/api/uploads/") || memoryRepository.existsByImageUrl(imageUrl)) {
            return;
        }
        String filename = imageUrl.substring("/api/uploads/".length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return;
        }
        Path directory = Path.of(storageLocation).toAbsolutePath().normalize();
        Path file = directory.resolve(filename).normalize();
        if (!file.startsWith(directory)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // A missing or locked image file should not stop the text record from being updated.
        }
    }

    private SpaceResponse toSpaceResponse(CoupleSpace space) {
        List<MemberResponse> members = spaceAccessService.membersOf(space.getId()).stream()
                .map(member -> new MemberResponse(member.getUser().getId(), member.getUser().getDisplayName()))
                .toList();
        long daysTogether = ChronoUnit.DAYS.between(space.getRelationshipStartedOn(), LocalDate.now()) + 1;
        return new SpaceResponse(
                space.getId(),
                space.getName(),
                space.getInviteCode(),
                space.getRelationshipStartedOn(),
                daysTogether,
                members
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private MemoryResponse toMemoryResponse(Memory memory) {
        return new MemoryResponse(
                memory.getId(),
                memory.getTitle(),
                memory.getContent(),
                memory.getImageUrl(),
                memory.getAuthorName(),
                memory.getOccurredOn()
        );
    }

    private LetterResponse toLetterResponse(Letter letter) {
        List<ReplyResponse> replies = letter.getReplies().stream()
                .map(reply -> new ReplyResponse(reply.getId(), reply.getAuthorName(), reply.getContent(), reply.getCreatedAt()))
                .toList();
        return new LetterResponse(
                letter.getId(),
                letter.getSenderName(),
                letter.getRecipientName(),
                letter.getContent(),
                letter.getStatus(),
                letter.getCreatedAt(),
                replies
        );
    }

    public record DashboardResponse(SpaceResponse space, List<MemoryResponse> memories, List<LetterResponse> letters) {
    }

    public record BootstrapResponse(
            AuthController.AuthResponse user,
            SpaceResponse space,
            List<MemoryResponse> memories,
            List<LetterResponse> letters
    ) {
    }

    public record SpaceResponse(
            Long id,
            String name,
            String inviteCode,
            LocalDate relationshipStartedOn,
            long daysTogether,
            List<MemberResponse> members
    ) {
    }

    public record MemberResponse(Long id, String displayName) {
    }

    public record MemoryResponse(Long id, String title, String content, String imageUrl, String authorName,
                                 LocalDate occurredOn) {
    }

    public record LetterResponse(Long id, String senderName, String recipientName, String content, String status,
                                 java.time.LocalDateTime createdAt, List<ReplyResponse> replies) {
    }

    public record ReplyResponse(Long id, String authorName, String content, java.time.LocalDateTime createdAt) {
    }
}
