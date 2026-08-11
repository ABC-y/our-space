package com.belongus.api;

import com.belongus.domain.AppUser;
import com.belongus.domain.CoupleSpace;
import com.belongus.domain.SpaceMember;
import com.belongus.repository.CoupleSpaceRepository;
import com.belongus.repository.SpaceMemberRepository;
import com.belongus.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/spaces")
public class CoupleSpaceController {
    private static final char[] INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final CoupleSpaceRepository spaceRepository;
    private final SpaceMemberRepository memberRepository;
    private final AuthService authService;
    private final SecureRandom secureRandom = new SecureRandom();

    public CoupleSpaceController(
            CoupleSpaceRepository spaceRepository,
            SpaceMemberRepository memberRepository,
            AuthService authService
    ) {
        this.spaceRepository = spaceRepository;
        this.memberRepository = memberRepository;
        this.authService = authService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SpaceSummary> spaces(HttpServletRequest request) {
        AppUser user = authService.requireUser(request);
        return memberRepository.findByUserIdOrderByJoinedAtDesc(user.getId()).stream()
                .map(SpaceMember::getSpace)
                .map(this::toSummary)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceSummary create(
            @Valid @RequestBody CreateSpaceRequest request,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        if (!memberRepository.findByUserIdOrderByJoinedAtDesc(user.getId()).isEmpty()) {
            throw new IllegalArgumentException("一个账号目前只能加入一个双人空间");
        }

        CoupleSpace space = spaceRepository.save(new CoupleSpace(
                request.name().trim(),
                user.getDisplayName(),
                request.relationshipStartedOn(),
                nextInviteCode()
        ));
        memberRepository.save(new SpaceMember(space, user));
        return toSummary(space);
    }

    @PostMapping("/join")
    public SpaceSummary join(@Valid @RequestBody JoinSpaceRequest request, HttpServletRequest servletRequest) {
        AppUser user = authService.requireUser(servletRequest);
        List<SpaceMember> currentMemberships = memberRepository.findByUserIdOrderByJoinedAtDesc(user.getId());
        String inviteCode = request.inviteCode().trim().toUpperCase(Locale.ROOT);
        CoupleSpace space = spaceRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("邀请码不存在，请检查后重试"));

        if (!currentMemberships.isEmpty() && currentMemberships.stream()
                .noneMatch(member -> member.getSpace().getId().equals(space.getId()))) {
            throw new IllegalArgumentException("一个账号目前只能加入一个双人空间");
        }
        if (memberRepository.existsBySpaceIdAndUserId(space.getId(), user.getId())) {
            return toSummary(space);
        }
        if (memberRepository.countBySpaceId(space.getId()) >= 2) {
            throw new IllegalArgumentException("这个双人空间已经满员了");
        }

        memberRepository.save(new SpaceMember(space, user));
        space.setMemberTwoName(user.getDisplayName());
        return toSummary(spaceRepository.save(space));
    }

    @PatchMapping("/{spaceId}")
    @Transactional
    public SpaceSummary updateRelationshipStartedOn(
            @PathVariable Long spaceId,
            @Valid @RequestBody UpdateRelationshipStartedOnRequest request,
            HttpServletRequest servletRequest
    ) {
        AppUser user = authService.requireUser(servletRequest);
        CoupleSpace space = requireMemberSpace(spaceId, user);
        space.setRelationshipStartedOn(request.relationshipStartedOn());
        return toSummary(spaceRepository.save(space));
    }

    private SpaceSummary toSummary(CoupleSpace space) {
        List<MemberResponse> members = memberRepository.findBySpaceIdOrderByJoinedAtAsc(space.getId()).stream()
                .map(member -> new MemberResponse(member.getUser().getId(), member.getUser().getDisplayName()))
                .toList();
        return new SpaceSummary(space.getId(), space.getName(), space.getInviteCode(), space.getRelationshipStartedOn(), members);
    }

    private CoupleSpace requireMemberSpace(Long spaceId, AppUser user) {
        CoupleSpace space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("没有找到这个空间"));
        boolean isMember = memberRepository.existsBySpaceIdAndUserId(spaceId, user.getId());
        if (!isMember) {
            throw new ForbiddenException("你没有修改这个空间的权限");
        }
        return space;
    }

    private String nextInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(8);
            for (int index = 0; index < 8; index++) {
                code.append(INVITE_ALPHABET[secureRandom.nextInt(INVITE_ALPHABET.length)]);
            }
            if (spaceRepository.findByInviteCode(code.toString()).isEmpty()) {
                return code.toString();
            }
        }
        throw new IllegalStateException("邀请码生成失败，请重试");
    }

    public record SpaceSummary(
            Long id,
            String name,
            String inviteCode,
            java.time.LocalDate relationshipStartedOn,
            List<MemberResponse> members
    ) {
    }

    public record MemberResponse(Long id, String displayName) {
    }
}
