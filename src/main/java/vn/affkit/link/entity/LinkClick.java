package vn.affkit.link.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "link_clicks")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "device_type", length = 20)
    private String deviceType;  // MOBILE / DESKTOP / TABLET

    @Column(name = "source", length = 20)
    private String source;      // ZALO / TELEGRAM / FACEBOOK / DIRECT / OTHER
}