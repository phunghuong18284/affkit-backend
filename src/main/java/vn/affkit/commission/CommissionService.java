package vn.affkit.commission;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.affkit.auth.entity.User;
import vn.affkit.commission.dto.CommissionResponse;
import vn.affkit.common.exception.AppException;
import vn.affkit.common.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class CommissionService {

    private static final String AT_BASE = "https://api.accesstrade.vn/v1";
    private final RestTemplate restTemplate;

    public CommissionResponse getTransactions(User user, int page, int limit) {
        if (user.getAccesstradeApiKey() == null || user.getAccesstradeApiKey().isBlank()) {
            throw new AppException(ErrorCode.ACCESSTRADE_KEY_NOT_FOUND);
        }

        String url = AT_BASE + "/transactions?limit=" + limit + "&page=" + page;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token " + user.getAccesstradeApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<CommissionResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, CommissionResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new AppException(ErrorCode.ACCESSTRADE_API_ERROR);
        }
    }
}