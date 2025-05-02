package studio.clashbuddy.clashaccess.gateway;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class JwtUtility {
    private static final Logger log = LoggerFactory.getLogger(JwtUtility.class);
    private final String secret;
    private final I18nHelper helper;
    public JwtUtility(String secret, I18nHelper helper) {
        this.secret = secret;
        this.helper = helper;
    }



    private String getToken(List<String> roles,List<String> permissions, String userId, TokenType tokenType, double duration) {
        return JWT.create()
                .withSubject(userId)
                .withExpiresAt(expireDate(duration))
                .withClaim("roles",roles)
                .withClaim("tokenType",tokenType.name())
                .withClaim("permissions",permissions)
                .sign(getAlgorithm());
    }

    private DecodedJWT verifyToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(getAlgorithm()).build();
            return verifier.verify(token);
        } catch (RuntimeException e) {
            log.warn(e.getMessage());
            throw new ClashAccessDeniedException(helper.i18n("{clashaccess.error.token-expired}"),403);
        }
    }

    public Pair<ClashAuthPayload, TokenType> validateToken(String token) {
        DecodedJWT decodedJWT = verifyTokenInHttpRequest(token);
        String userId = getUsername(decodedJWT);
        String[] roles = decodedJWT.getClaim("roles").asArray(String.class);
        String[] permissions = decodedJWT.getClaim("permissions").asArray(String.class);
        String tokenType = decodedJWT.getClaim("tokenType").asString().toUpperCase();
        return Pair.of(new ClashAuthPayload(userId,roles,permissions), TokenType.valueOf(tokenType));
    }

    public String getUsername(DecodedJWT decodedJWT) {
        return decodedJWT.getSubject();
    }


    private DecodedJWT verifyTokenInHttpRequest(String token) {
        token = token.substring("Bearer ".length());
        return verifyToken(token);
    }

    public Pair<String,String> generateJWT(String userId, String[] roles,
                                           String[] permissions, double accessMinutes, double refreshMinutes) {
        final var ACCESS_TOKEN = getToken(Arrays.stream(roles).toList(), Arrays.stream(permissions).toList(),userId, TokenType.ACCESS,accessMinutes);
        final var REFRESH_TOKEN = getToken(Arrays.stream(roles).toList(), Arrays.stream(permissions).toList(),userId, TokenType.REFRESH,refreshMinutes);
        return Pair.of(ACCESS_TOKEN, REFRESH_TOKEN);
    }

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(secret);
    }

    public Date expireDate(double expireMinutes) {
        return new Date(System.currentTimeMillis() + (long) expireMinutes * 60 * 1000);
    }

    public enum  TokenType {
        ACCESS,REFRESH
    }
}