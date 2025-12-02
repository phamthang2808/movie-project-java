package com.example.thangcachep.movie_project_be.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig implements WebMvcConfigurer {

    private final FileUploadProperties fileUploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Thư mục uploads từ configuration
        Path uploadDir = Paths.get(fileUploadProperties.getUploadDir());
        String absolute = uploadDir.toFile().getAbsolutePath();

        String uploadPath = "/" + fileUploadProperties.getUploadDir() + "/**";

        registry.addResourceHandler(uploadPath)
                .addResourceLocations("file:" + absolute + "/");

        // Nếu FE có gọi kiểu http://host:8088/api/uploads/...
        registry.addResourceHandler("/api" + uploadPath)
                .addResourceLocations("file:" + absolute + "/");
    }

    /**
     * LocaleResolver - Resolve locale từ Accept-Language header
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setSupportedLocales(Arrays.asList(
                Locale.forLanguageTag("vi"),  // Tiếng Việt
                Locale.forLanguageTag("en")   // English
        ));
        localeResolver.setDefaultLocale(Locale.forLanguageTag("vi")); // Mặc định: tiếng Việt
        return localeResolver;
    }

    /**
     * MessageSource - Load messages từ i18n/messages*.properties
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600); // Cache 1 giờ
        messageSource.setFallbackToSystemLocale(false); // Không fallback về system locale
        return messageSource;
    }
}

