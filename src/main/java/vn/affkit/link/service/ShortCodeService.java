package vn.affkit.link.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.affkit.link.repository.LinkRepository;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class ShortCodeService {

    // Bỏ O/0/I/l tránh nhầm lẫn khi đọc
    private static final String ALPHABET =
            "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int DEFAULT_LENGTH = 6;
    private static final int MAX_ATTEMPTS   = 3;

    private final SecureRandom    random = new SecureRandom();
    private final LinkRepository  linkRepository;

    public String generate() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String code = randomCode(DEFAULT_LENGTH);
            if (!linkRepository.existsByShortCode(code)) {
                return code;
            }
        }
        // Fallback: 7 ký tự nếu collision liên tiếp 3 lần
        return randomCode(DEFAULT_LENGTH + 1);
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}