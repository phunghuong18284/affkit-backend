package vn.affkit.campaign.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.affkit.campaign.entity.Campaign;

import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Page<Campaign> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID userId, Pageable pageable);

    Optional<Campaign> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    long countByUserIdAndDeletedFalse(UUID userId);
}