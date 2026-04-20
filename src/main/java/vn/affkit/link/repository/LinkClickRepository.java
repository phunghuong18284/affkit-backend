package vn.affkit.link.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.affkit.link.entity.LinkClick;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LinkClickRepository extends JpaRepository<LinkClick, Long> {

    @Query("SELECT lc FROM LinkClick lc " +
            "WHERE lc.linkId IN (" +
            "  SELECT l.id FROM Link l WHERE l.user.id = :userId AND l.deleted = false" +
            ") " +
            "AND lc.clickedAt BETWEEN :from AND :to")
    List<LinkClick> findByUserIdAndClickedAtBetween(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    List<LinkClick> findByLinkIdAndClickedAtBetween(UUID linkId, Instant from, Instant to);
}