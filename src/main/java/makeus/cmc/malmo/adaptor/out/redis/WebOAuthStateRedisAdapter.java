package makeus.cmc.malmo.adaptor.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.exception.InvalidWebOAuthRequestException;
import makeus.cmc.malmo.application.port.out.member.WebOAuthStatePort;
import makeus.cmc.malmo.config.WebSocialLoginProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class WebOAuthStateRedisAdapter implements WebOAuthStatePort {

    private static final String KEY_PREFIX = "web-oauth:state:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebSocialLoginProperties properties;

    @Override
    public void save(State state) {
        try {
            redisTemplate.opsForValue().set(
                    key(state.state()),
                    objectMapper.writeValueAsString(state),
                    Duration.ofSeconds(properties.getStateExpirationSeconds())
            );
        } catch (JsonProcessingException e) {
            throw new InvalidWebOAuthRequestException("웹 로그인 state 저장에 실패했습니다.", e);
        }
    }

    @Override
    public State consume(String state) {
        String serialized = redisTemplate.opsForValue().getAndDelete(key(state));
        if (serialized == null) {
            throw new InvalidWebOAuthRequestException("만료되었거나 이미 사용된 웹 로그인 state입니다.");
        }
        try {
            return objectMapper.readValue(serialized, State.class);
        } catch (JsonProcessingException e) {
            throw new InvalidWebOAuthRequestException("웹 로그인 state 해석에 실패했습니다.", e);
        }
    }

    private static String key(String state) {
        return KEY_PREFIX + state;
    }
}
