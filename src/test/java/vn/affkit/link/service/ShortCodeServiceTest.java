package vn.affkit.link.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.affkit.link.repository.LinkRepository;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortCodeService Tests")
class ShortCodeServiceTest {

    @Mock LinkRepository linkRepository;

    @InjectMocks ShortCodeService shortCodeService;

    // Bộ ký tự hợp lệ (không có O, 0, I, l)
    private static final String VALID_ALPHABET =
            "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("✅ Không có collision → trả về shortCode 6 ký tự")
        void generate_noCollision_returns6Chars() {
            when(linkRepository.existsByShortCode(anyString())).thenReturn(false);

            String code = shortCodeService.generate();

            assertThat(code).hasSize(6);
        }

        @Test
        @DisplayName("✅ Chỉ chứa ký tự trong bộ alphabet hợp lệ")
        void generate_onlyValidChars() {
            when(linkRepository.existsByShortCode(anyString())).thenReturn(false);

            String code = shortCodeService.generate();

            for (char c : code.toCharArray()) {
                assertThat(VALID_ALPHABET).contains(String.valueOf(c));
            }
        }

        @Test
        @DisplayName("✅ Không chứa ký tự dễ nhầm: O, 0, I, l")
        void generate_noConfusingChars() {
            when(linkRepository.existsByShortCode(anyString())).thenReturn(false);

            String code = shortCodeService.generate();

            assertThat(code).doesNotContain("O", "0", "I", "l");
        }

        @Test
        @DisplayName("✅ Collision 3 lần liên tiếp → fallback 7 ký tự")
        void generate_3Collisions_returns7Chars() {
            // 3 lần đầu đều collision, lần fallback không check
            when(linkRepository.existsByShortCode(anyString()))
                    .thenReturn(true)   // lần 1
                    .thenReturn(true)   // lần 2
                    .thenReturn(true);  // lần 3 → fallback 7 ký tự

            String code = shortCodeService.generate();

            assertThat(code).hasSize(7);
        }

        @Test
        @DisplayName("✅ Collision 1 lần → retry thành công, trả 6 ký tự")
        void generate_1Collision_retrySuccess() {
            when(linkRepository.existsByShortCode(anyString()))
                    .thenReturn(true)   // lần 1: collision
                    .thenReturn(false); // lần 2: thành công

            String code = shortCodeService.generate();

            assertThat(code).hasSize(6);
        }

        @RepeatedTest(100)
        @DisplayName("✅ Gọi 100 lần → luôn trả về ít nhất 6 ký tự")
        void generate_always_atLeast6Chars() {
            when(linkRepository.existsByShortCode(anyString())).thenReturn(false);

            String code = shortCodeService.generate();

            assertThat(code.length()).isGreaterThanOrEqualTo(6);
        }

        @Test
        @DisplayName("✅ Gọi 500 lần → không có duplicate (xác suất collision thực tế)")
        void generate_500Times_noDuplicates() {
            when(linkRepository.existsByShortCode(anyString())).thenReturn(false);

            Set<String> codes = new HashSet<>();
            for (int i = 0; i < 500; i++) {
                codes.add(shortCodeService.generate());
            }

            // 500 lần generate không nên có duplicate
            assertThat(codes).hasSize(500);
        }
    }
}
