package studio.clashbuddy.clashaccess.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Component
public class ClashAccessAuthorizationFilter extends AbstractGatewayFilterFactory<ClashAccessAuthorizationFilter.Config> implements Ordered {
    private final I18nHelper i18nHelper;
    private final JwtUtility jwtUtility;
    private final ClashAccessGatewayProperties properties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final TokenVersionRedisService tokenVersionRedisService;

    public ClashAccessAuthorizationFilter(I18nHelper i18nHelper, ClashAccessGatewayProperties properties, TokenVersionRedisService tokenVersionRedisService) {
        super(Config.class);
        this.i18nHelper = i18nHelper;
        this.properties = properties;
        this.tokenVersionRedisService = tokenVersionRedisService;
        this.jwtUtility = new JwtUtility(properties.getJwtSecret(), i18nHelper);
    }

    private boolean isAllowedPath(ServerHttpRequest request) {
        var endingPoint = request.getURI().getPath();
        if (properties.getPublicPaths().stream().anyMatch(path -> antPathMatcher.match(path, endingPoint)))
            return true;
        var upgrade = request.getHeaders().get("upgrade");
        if (upgrade == null) return false;
        return upgrade.contains("websocket");
    }

    private String extractToken(HttpHeaders headers) {
        if (!headers.containsKey(HttpHeaders.AUTHORIZATION))
            throw new ClashAccessDeniedException(i18nHelper.i18n("{clashaccess.error.missing.auth-header}"), 401);
        String token = Objects.requireNonNull(headers.get(HttpHeaders.AUTHORIZATION)).get(0);
        if (token == null || !token.startsWith("Bearer "))
            throw new ClashAccessDeniedException(i18nHelper.i18n("{clashaccess.error.missing.token-not-bearer}"), 401);
        return token;
    }


    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            if (isAllowedPath(exchange.getRequest()))
                return chain.filter(exchange);
            var token = extractToken(exchange.getRequest().getHeaders());
            var jwtSession = jwtUtility.validateToken(token);
            var payload = jwtSession.getFirstItem();
            var type = jwtSession.getSecondItem();
            if (!type.equals(JwtUtility.TokenType.ACCESS))
                throw new ClashAccessDeniedException(i18nHelper.i18n("{clashaccess.error.missing.token-is-not-access}"), 403);

            if (!properties.isBypassTokenVersion()) {
                if (payload.getTokenVersion() == null)
                    throw new ClashAccessDeniedException(i18nHelper.i18n("{clashaccess.error.missing.token-is-not-access}"), 403);
                var tokenVersion = tokenVersionRedisService.getTokenVersion(payload.getUserId());
                if (!payload.getTokenVersion().equals(tokenVersion))
                    throw new ClashAccessDeniedException(i18nHelper.i18n("{clashaccess.error.token-version-out-dated}"), 403);
            }

            List<String> roles = new ArrayList<>(Arrays.asList(payload.getRoles()));
            List<String> permissions = new ArrayList<>(Arrays.asList(payload.getPermissions()));

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .headers(httpHeaders -> {
                        httpHeaders.remove(HttpHeaders.AUTHORIZATION);
                        httpHeaders.remove("x-ca-uid");
                        httpHeaders.remove("x-ca-urs");
                        httpHeaders.remove("x-ca-ups");

                        httpHeaders.set("x-ca-uid", payload.getUserId());
                        httpHeaders.addAll("x-ca-urs",roles);
                        httpHeaders.addAll("x-ca-ups", permissions);
                    })
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            return chain.filter(mutatedExchange);
        };
    }

    @Override
    public int getOrder() {
        return 1;
    }

    public static class Config {
    }


}
