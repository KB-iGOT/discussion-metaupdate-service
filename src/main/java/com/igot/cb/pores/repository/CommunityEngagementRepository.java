package com.igot.cb.pores.repository;

import com.igot.cb.pores.entity.CommunityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityEngagementRepository extends JpaRepository<com.igot.cb.pores.entity.CommunityEntity, String> {

    Optional<CommunityEntity> findByCommunityIdAndIsActive(String communityId, boolean isActive);
}
