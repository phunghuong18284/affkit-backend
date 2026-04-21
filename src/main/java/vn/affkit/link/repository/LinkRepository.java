package vn.affkit.link.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.affkit.link.entity.Link;

import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends JpaRepository<Link, UUID> {

    boolean existsByShortCode(String shortCode);

    Optional<Link> findByShortCode(String shortCode);

    Optional<Link> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    long countByUserIdAndDeletedFalse(UUID userId);

    long countByCampaignIdAndDeletedFalse(UUID campaignId);

    @Query("""
            SELECT l FROM Link l
            WHERE l.user.id = :userId
              AND l.deleted = false
              AND (:platform IS NULL OR l.platform = :platform)
            ORDER BY l.createdAt DESC
            """)
    Page<Link> findByUserFiltered(UUID userId, String platform, Pageable pageable);

    @Query("""
            SELECT l FROM Link l
            WHERE l.campaignId = :campaignId
              AND l.user.id = :userId
              AND l.deleted = false
            ORDER BY l.createdAt DESC
            """)
    Page<Link> findByCampaignIdAndUserIdAndDeletedFalse(UUID campaignId, UUID userId, Pageable pageable);
}