package com.example.mocktester.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Optionally exposes a plain HTTP port next to HTTPS (APP_HTTP_ENABLED=true). */
@Configuration
public class HttpConnectorConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> extraHttpConnector(
            @Value("${app.http.enabled:false}") boolean httpEnabled,
            @Value("${server.ssl.enabled:true}") boolean sslEnabled,
            @Value("${app.http.port:8080}") int httpPort) {
        return factory -> {
            if (httpEnabled && sslEnabled) {
                Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                connector.setPort(httpPort);
                factory.addAdditionalTomcatConnectors(connector);
            }
        };
    }
}
