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
        String tokenVersion;
        String userId2;
        try {
            tokenVersion = decodedJWT.getClaim("tokenVersion").asString().toUpperCase();
        }catch (Exception e){
            tokenVersion = null;
        }
        try {
            userId2 = decodedJWT.getClaim("userId2").asString();
        }catch (Exception e){
            userId2 = null;
        }

        return Pair.of(new ClashAuthPayload(userId,userId2,roles,permissions,tokenVersion), TokenType.valueOf(tokenType));
    }

    public String getUsername(DecodedJWT decodedJWT) {
        return decodedJWT.getSubject();
    }


    private DecodedJWT verifyTokenInHttpRequest(String token) {
        token = token.substring("Bearer ".length());
        return verifyToken(token);
    }


    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(secret);
    }


    public enum  TokenType {
        ACCESS,REFRESH
    }
}