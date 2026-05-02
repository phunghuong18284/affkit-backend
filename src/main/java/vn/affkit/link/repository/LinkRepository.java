package vn.affkit.link.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.affkit.link.entity.Link;

import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends JpaRepository<Link, UUID> {

    boolean existsByShortCode(String shortCode);

    Optional<Link> findByShortCode(String shortCode);

    Optional<Link> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    long countByUserIdAndDeletedFalse(UUID userId);

    long countByCampaignIdAndDeletedFalse(UUID campaignId);

    @Query(value = """
            SELECT * FROM links
            WHERE user_id = :userId
              AND is_deleted = false
              AND (:platform IS NULL OR platform = :platform)
              AND (:search IS NULL OR title ILIKE CONCAT('%', :search, '%'))
            ORDER BY created_at DESC
            """, nativeQuery = true)
    Page<Link> findByUserFiltered(
            @Param("userId") UUID userId,
            @Param("platform") String platform,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
            SELECT l FROM Link l
            WHERE l.campaignId = :campaignId
              AND l.user.id = :userId
              AND l.deleted = false
            ORDER BY l.createdAt DESC
            """)
    Page<Link> findByCampaignIdAndUserIdAndDeletedFalse(UUID campaignId, UUID userId, Pageable pageable);
}