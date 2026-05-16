package vn.affkit.commission.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CommissionResponse(
        @JsonProperty("total") long totalCount,
        List<Transaction> data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(
            @JsonProperty("order_id") String orderId,
            @JsonProperty("transaction_time") long transactionTime,
            @JsonProperty("commission") double commission,
            @JsonProperty("transaction_value") double transactionValue,
            @JsonProperty("status") int status,
            @JsonProperty("merchant_name") String merchantName,
            @JsonProperty("campaign_name") String campaignName
    ) {}
}