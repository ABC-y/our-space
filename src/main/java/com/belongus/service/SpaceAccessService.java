package com.belongus.service;

import com.belongus.api.ForbiddenException;
import com.belongus.domain.AppUser;
import com.belongus.domain.CoupleSpace;
import com.belongus.domain.SpaceMember;
import com.belongus.repository.CoupleSpaceRepository;
import com.belongus.repository.SpaceMemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpaceAccessService {
    private final CoupleSpaceRepository spaceRepository;
    private final SpaceMemberRepository memberRepository;

    public SpaceAccessService(CoupleSpaceRepository spaceRepository, SpaceMemberRepository memberRepository) {
        this.spaceRepository = spaceRepository;
        this.memberRepository = memberRepository;
    }

    public CoupleSpace requireMemberSpace(Long spaceId, AppUser user) {
        CoupleSpace space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("没有找到这个空间"));
        if (!memberRepository.existsBySpaceIdAndUserId(spaceId, user.getId())) {
            throw new ForbiddenException("你没有访问这个空间的权限");
        }
        return space;
    }

    public List<SpaceMember> membersOf(Long spaceId) {
        return memberRepository.findBySpaceIdOrderByJoinedAtAsc(spaceId);
    }

    public String partnerName(CoupleSpace space, AppUser user) {
        return membersOf(space.getId()).stream()
                .map(SpaceMember::getUser)
                .filter(member -> !member.getId().equals(user.getId()))
                .findFirst()
                .map(AppUser::getDisplayName)
                .orElseThrow(() -> new IllegalArgumentException("先邀请对方加入，再写悄悄话吧"));
    }
}
