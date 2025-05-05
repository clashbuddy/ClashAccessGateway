package studio.clashbuddy.clashaccess.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "clashaccess.security.bypassTokenVersion", havingValue = "false", matchIfMissing = true)
public class TokenVersionRedisService {
    private final StringRedisTemplate redisTemplate;
    private final I18nHelper i18nHelper;
    private static final String KEY_PREFIX = "user:tokenVersion:";
    public TokenVersionRedisService(StringRedisTemplate redisTemplate, I18nHelper i18nHelper) {
        this.redisTemplate = redisTemplate;
        this.i18nHelper = i18nHelper;
    }

    public String getTokenVersion(String userId) {

        String key = buildKey(userId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            throw new ClashAccessDeniedException(i18nHelper.i18n("{clashaccess.error.token-version}", userId),401);
        }
        return value;
    }

    private String buildKey(String userId) {
        return KEY_PREFIX + userId;
    }


}
