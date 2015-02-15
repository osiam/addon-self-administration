package org.osiam.addons.self_administration.template;

import javax.annotation.*;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.context.support.*;
import org.springframework.validation.*;
import org.springframework.validation.beanvalidation.*;
import org.springframework.web.servlet.config.annotation.*;
import org.thymeleaf.spring4.*;
import org.thymeleaf.spring4.resourceresolver.*;
import org.thymeleaf.templateresolver.*;

@Configuration
public class TemplateConfiguration extends WebMvcConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateConfiguration.class);

    @Value("#{!(T(com.google.common.base.Strings).isNullOrEmpty(\"${org.osiam.external.resources.path:}\"))}")
    private Boolean externalConfigurationEnabled;

    @Value("${org.osiam.external.resources.path:}")
    private String externalConfigPath;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private SpringResourceResourceResolver thymeleafResourceResolver;

    @PostConstruct
    public void configureExternalTemplateResolving() {
        if (externalConfigurationEnabled) {

            LOG.info("Using external configured resources in path {}", externalConfigPath);

            final ITemplateResolver first = templateEngine.getTemplateResolvers().iterator().next();
            TemplateResolver f = (TemplateResolver) first;
            f.setOrder(2);

            TemplateResolver externalTemplateResolver = new TemplateResolver();
            externalTemplateResolver.setPrefix("file:" + externalConfigPath + "/templates/");
            externalTemplateResolver.setSuffix(".html");
            externalTemplateResolver.setTemplateMode("HTML5");
            externalTemplateResolver.setCharacterEncoding("UTF-8");
            externalTemplateResolver.setOrder(1);
            externalTemplateResolver.setResourceResolver(thymeleafResourceResolver);
            templateEngine.addTemplateResolver(externalTemplateResolver);
        }
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setDefaultEncoding("UTF-8");
        if (externalConfigurationEnabled) {
            messageSource.setBasenames("file:" + externalConfigPath + "/i18n/registration", "file:"
                    + externalConfigPath + "/i18n/mail", "classpath:/i18n/registration", "classpath:/i18n/mail");
        } else {
            messageSource.setBasenames("classpath:/i18n/registration", "classpath:/i18n/mail");
        }
        return messageSource;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (externalConfigurationEnabled) {
            // there is not merging, so the first file found will be used
            registry.addResourceHandler("/css/**").addResourceLocations("file:" + externalConfigPath + "/css/",
                    "classpath:/resources/css/");
        } else {
            registry.addResourceHandler("/css/**").addResourceLocations("classpath:/resources/css/");
        }
    }

    @Override
    public Validator getValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource());
        return validator;
    }
}
