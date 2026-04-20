package vn.affkit.link.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.affkit.link.entity.LinkTag;

import java.util.UUID;

public interface LinkTagRepository extends JpaRepository<LinkTag, UUID> {
    void deleteByLinkId(UUID linkId);
}