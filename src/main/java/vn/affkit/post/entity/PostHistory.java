package vn.affkit.post.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_history")
public class PostHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "product_url", nullable = false, columnDefinition = "TEXT")
    private String productUrl;

    @Column(name = "product_name", columnDefinition = "TEXT")
    private String productName;

    @Column(name = "product_price")
    private Long productPrice;

    @Column(name = "product_image", columnDefinition = "TEXT")
    private String productImage;

    @Column(name = "post_zalo", columnDefinition = "TEXT")
    private String postZalo;

    @Column(name = "post_facebook", columnDefinition = "TEXT")
    private String postFacebook;

    @Column(name = "post_telegram", columnDefinition = "TEXT")
    private String postTelegram;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String v) { this.productUrl = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public Long getProductPrice() { return productPrice; }
    public void setProductPrice(Long v) { this.productPrice = v; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String v) { this.productImage = v; }
    public String getPostZalo() { return postZalo; }
    public void setPostZalo(String v) { this.postZalo = v; }
    public String getPostFacebook() { return postFacebook; }
    public void setPostFacebook(String v) { this.postFacebook = v; }
    public String getPostTelegram() { return postTelegram; }
    public void setPostTelegram(String v) { this.postTelegram = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}