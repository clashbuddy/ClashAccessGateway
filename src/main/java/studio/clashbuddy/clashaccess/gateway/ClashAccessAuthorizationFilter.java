package studio.clashbuddy.clashaccess.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;

import java.util.Objects;


@Component
public class ClashAccessAuthorizationFilter extends AbstractGatewayFilterFactory<ClashAccessAuthorizationFilter.Config> implements Ordered {
    private final I18nHelper i18nHelper;
    private final JwtUtility jwtUtility;
    private final ClashAccessGatewayProperties properties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    public ClashAccessAuthorizationFilter(I18nHelper i18nHelper, ClashAccessGatewayProperties properties) {
        super(Config.class);
        this.i18nHelper = i18nHelper;
        this.properties = properties;
        this.jwtUtility = new JwtUtility(properties.getJwtSecret(),i18nHelper);
    }

    private boolean isAllowedPath(ServerHttpRequest request) {
        var endingPoint = request.getURI().getPath();
        if(properties.getPublicPaths().stream().anyMatch(path-> antPathMatcher.match(path,endingPoint)))
            return true;
        var upgrade  = request.getHeaders().get("upgrade");
        if(upgrade == null) return false;
        return upgrade.contains("websocket");
    }

    private String extractToken(HttpHeaders headers){
        if(!headers.containsKey(HttpHeaders.AUTHORIZATION))
            throw new ClashAccessDeniedException(i18nHelper.i18n("{clashaccess.error.missing.auth-header}"),401);
        String token = Objects.requireNonNull(headers.get(HttpHeaders.AUTHORIZATION)).get(0);
        if(token == null || !token.startsWith("Bearer "))
            throw new ClashAccessDeniedException(i18nHelper.i18n("{clashaccess.error.missing.token-not-bearer}"),401);
        return  token;
    }


    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            if(isAllowedPath(exchange.getRequest()))
                return chain.filter(exchange);
            var token = extractToken(exchange.getRequest().getHeaders());
            var jwtSession = jwtUtility.validateToken(token);
            var payload = jwtSession.getFirstItem();
            var type = jwtSession.getSecondItem();
            if(!type.equals(JwtUtility.TokenType.ACCESS))
                throw new ClashAccessDeniedException(i18nHelper.i18n("{clashaccess.error.missing.token-is-not-access}"),403);
            exchange.getRequest()
                    .mutate()
                    .header("x-ca-uid",payload.getUserId())
                    .header("x-ca-urs",payload.getRoles())
                    .header("x-ca-ups", payload.getPermissions());
            return chain.filter(exchange);
        };
    }

    @Override
    public int getOrder() {
        return 1;
    }

    public static class Config{}




}
