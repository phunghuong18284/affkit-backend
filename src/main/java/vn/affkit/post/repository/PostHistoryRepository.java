package vn.affkit.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.affkit.post.entity.PostHistory;
import java.util.UUID;

public interface PostHistoryRepository extends JpaRepository<PostHistory, Long> {
    Page<PostHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}