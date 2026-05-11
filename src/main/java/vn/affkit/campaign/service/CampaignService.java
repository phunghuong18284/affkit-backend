package vn.affkit.campaign.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.affkit.auth.entity.User;
import vn.affkit.auth.repository.UserRepository;
import vn.affkit.campaign.dto.CampaignResponse;
import vn.affkit.campaign.dto.CreateCampaignRequest;
import vn.affkit.campaign.dto.UpdateCampaignRequest;
import vn.affkit.campaign.entity.Campaign;
import vn.affkit.campaign.repository.CampaignRepository;
import vn.affkit.common.exception.AppException;
import vn.affkit.common.exception.ErrorCode;
import vn.affkit.link.dto.LinkResponse;
import vn.affkit.link.repository.LinkClickRepository;
import vn.affkit.link.repository.LinkRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository  campaignRepository;
    private final UserRepository      userRepository;
    private final LinkRepository      linkRepository;
    private final LinkClickRepository linkClickRepository;

    @Value("${app.short-url-base:http://localhost:8080/go/}")
    private String shortUrlBase;

    @Transactional
    public CampaignResponse create(UUID userId, CreateCampaignRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Campaign campaign = Campaign.builder()
                .user(user)
                .name(req.name().trim())
                .description(req.description())
                .build();

        campaignRepository.save(campaign);
        return CampaignResponse.from(campaign, 0L, 0L);
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> list(UUID userId, int page, int size) {
        return campaignRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
                        userId, PageRequest.of(page, size))
                .map(c -> CampaignResponse.from(
                        c,
                        linkClickRepository.countByCampaignId(c.getId()),
                        linkRepository.countByCampaignIdAndDeletedFalse(c.getId())
                ));
    }

    @Transactional(readOnly = true)
    public CampaignResponse getById(UUID userId, UUID campaignId) {
        Campaign campaign = campaignRepository
                .findByIdAndUserIdAndDeletedFalse(campaignId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));
        return CampaignResponse.from(
                campaign,
                linkClickRepository.countByCampaignId(campaignId),
                linkRepository.countByCampaignIdAndDeletedFalse(campaignId)
        );
    }

    @Transactional
    public CampaignResponse update(UUID userId, UUID campaignId, UpdateCampaignRequest req) {
        Campaign campaign = campaignRepository
                .findByIdAndUserIdAndDeletedFalse(campaignId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));

        if (req.name() != null && !req.name().isBlank()) {
            campaign.setName(req.name().trim());
        }
        if (req.description() != null) {
            campaign.setDescription(req.description());
        }

        campaignRepository.save(campaign);
        return CampaignResponse.from(
                campaign,
                linkClickRepository.countByCampaignId(campaignId),
                linkRepository.countByCampaignIdAndDeletedFalse(campaignId)
        );
    }

    @Transactional
    public void delete(UUID userId, UUID campaignId) {
        Campaign campaign = campaignRepository
                .findByIdAndUserIdAndDeletedFalse(campaignId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));

        campaign.setDeleted(true);
        campaign.setDeletedAt(Instant.now());
        campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public Page<LinkResponse> getLinks(UUID userId, UUID campaignId, int page, int size) {
        campaignRepository
                .findByIdAndUserIdAndDeletedFalse(campaignId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));

        return linkRepository
                .findByCampaignIdAndUserIdAndDeletedFalse(campaignId, userId, PageRequest.of(page, size))
                .map(l -> LinkResponse.from(l, shortUrlBase));
    }
}