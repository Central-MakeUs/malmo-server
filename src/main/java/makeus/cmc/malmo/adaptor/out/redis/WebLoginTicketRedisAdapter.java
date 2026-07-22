package makeus.cmc.malmo.adaptor.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.exception.InvalidWebOAuthRequestException;
import makeus.cmc.malmo.application.port.out.member.WebLoginTicketPort;
import makeus.cmc.malmo.config.WebSocialLoginProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class WebLoginTicketRedisAdapter implements WebLoginTicketPort {

    private static final String KEY_PREFIX = "web-oauth:ticket:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebSocialLoginProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String issue(Ticket ticket) {
        String ticketValue = randomTicket();
        try {
            redisTemplate.opsForValue().set(
                    key(ticketValue),
                    objectMapper.writeValueAsString(ticket),
                    Duration.ofSeconds(properties.getTicketExpirationSeconds())
            );
            return ticketValue;
        } catch (JsonProcessingException e) {
            throw new InvalidWebOAuthRequestException("웹 로그인 ticket 저장에 실패했습니다.", e);
        }
    }

    @Override
    public Ticket consume(String ticket) {
        String serialized = redisTemplate.opsForValue().getAndDelete(key(ticket));
        if (serialized == null) {
            throw new InvalidWebOAuthRequestException("만료되었거나 이미 사용된 웹 로그인 ticket입니다.");
        }
        try {
            return objectMapper.readValue(serialized, Ticket.class);
        } catch (JsonProcessingException e) {
            throw new InvalidWebOAuthRequestException("웹 로그인 ticket 해석에 실패했습니다.", e);
        }
    }

    private String randomTicket() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String key(String ticket) {
        return KEY_PREFIX + ticket;
    }
}
