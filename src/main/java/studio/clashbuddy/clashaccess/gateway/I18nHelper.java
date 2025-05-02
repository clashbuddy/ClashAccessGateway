package studio.clashbuddy.clashaccess.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class I18nHelper {

    @Autowired(required = false)
    private MessageSource messageSource;
    @Autowired
    private MessageSource clashAccessMessageSource;

    public String i18n(String message, Object... args) {
        if (message == null) return "";

        if (message.startsWith("{") && message.endsWith("}")) {
            var locale = LocaleContextHolder.getLocale();
            String code = message.substring(1, message.length() - 1);
            if (messageSource != null) {
                try {
                    return messageSource.getMessage(code, args, locale);
                } catch (NoSuchMessageException e) {
                    try{
                         return clashAccessMessageSource.getMessage(code, args, locale);
                    }catch (NoSuchMessageException es) {
                        return code;
                    }
                }
            }
            return code;
        }

        return message;
    }
}