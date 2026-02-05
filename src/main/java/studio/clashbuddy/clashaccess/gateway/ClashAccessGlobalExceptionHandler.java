package studio.clashbuddy.clashaccess.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.support.NotFoundException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Order(-2)
public class ClashAccessGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final I18nHelper i18nHelper;
    private static final Logger LOGGER = Logger.getLogger(ClashAccessGlobalExceptionHandler.class.getName());


    public ClashAccessGlobalExceptionHandler(I18nHelper i18nHelper) {
        this.i18nHelper = i18nHelper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An unexpected error occurred";
        String errorType = "UNEXPECTED_ERROR";

        if (ex instanceof ClashAccessDeniedException denied) {
            status = HttpStatus.valueOf(denied.getCode());
            message = i18nHelper.i18n(denied.getMessage(), new Object[0]);
            errorType = "ACCESS_DENIED";
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            errorType = "ROUTING_ERROR";
        } else if (ex instanceof NotFoundException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = i18nHelper.i18n("{clashaccess.error.un-available-service}");
            errorType = "SERVICE_UNAVAILABLE";
        } else if (ex.getCause() instanceof ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = i18nHelper.i18n("{clashaccess.error.not-connection-to-service}");
            errorType = "CONNECTION_FAILED";
        }

        String path = exchange.getRequest().getURI().getPath();
        if(errorType.equals("UNEXPECTED_ERROR")){
            LOGGER.log(
                    Level.SEVERE,
                    "Unhandled exception at path=" + path + ", message=" + ex.getMessage(),
                    ex
            );
        }

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", errorType,
                "message", message,
                "path", path
        );

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }
}