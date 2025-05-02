package studio.clashbuddy.clashaccess.gateway;


import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration
@ComponentScan("studio.clashbuddy.clashaccess.gateway")
public class ClashAccessGatewayAutoConfiguration {

    @Bean("clashAccessMessageSource")
    public MessageSource clashAccessMessageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:clashaccess/messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }
}
