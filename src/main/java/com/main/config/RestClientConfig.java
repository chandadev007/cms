package com.main.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

        @Bean
        public RestClient camdxRestClient(
                        @Value("${cms.camdx.api.username}") String camdxUser,
                        @Value("${cms.camdx.api.password}") String camdxPassword,
                        @Value("${cms.app.restclient.connection_timeout}") String connectionTimeout,
                        @Value("${cms.app.restclient.socket_timeout}") String socketTimeout) {

                ConnectionConfig connectionConfig = ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofMilliseconds(Long.parseLong(connectionTimeout)))
                                .setSocketTimeout(Timeout.ofMilliseconds(Long.parseLong(socketTimeout)))
                                .build();

                PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
                connectionManager.setMaxTotal(50);
                connectionManager.setDefaultMaxPerRoute(20);
                connectionManager.setDefaultConnectionConfig(connectionConfig);

                CloseableHttpClient httpClient = HttpClients.custom()
                                .setConnectionManager(connectionManager)
                                .evictIdleConnections(org.apache.hc.core5.util.TimeValue.ofSeconds(30))
                                .build();

                HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                                httpClient);

                return RestClient.builder()
                                .requestFactory(requestFactory)
                                .requestInterceptor(new BasicAuthenticationInterceptor(camdxUser, camdxPassword))
                                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                                .build();
        }

        @Bean
        public RestClient fileManagementRestClient(@Value("${cms.file-management.user}") String fileUser,
                        @Value("${cms.file-management.password}") String filePassword,
                        @Value("${cms.app.restclient.connection_timeout}") String connectionTimeout,
                        @Value("${cms.app.restclient.socket_timeout}") String socketTimeout) {

                ConnectionConfig connectionConfig = ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofMilliseconds(Long.parseLong(connectionTimeout)))
                                .setSocketTimeout(Timeout.ofMilliseconds(Long.parseLong(socketTimeout)))
                                .build();

                PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
                connectionManager.setMaxTotal(50);
                connectionManager.setDefaultMaxPerRoute(20);
                connectionManager.setDefaultConnectionConfig(connectionConfig);

                CloseableHttpClient httpClient = HttpClients.custom()
                                .setConnectionManager(connectionManager)
                                .evictIdleConnections(org.apache.hc.core5.util.TimeValue.ofSeconds(30))
                                .build();

                HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                                httpClient);

                return RestClient.builder()
                                .requestFactory(requestFactory)
                                .requestInterceptor(new BasicAuthenticationInterceptor(fileUser, filePassword))
                                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                                .build();
        }

        @Bean
        public RestClient appManagementRestClient(
                        @Value("${cms.app.restclient.connection_timeout}") String connectionTimeout,
                        @Value("${cms.app.restclient.socket_timeout}") String socketTimeout) {

                ConnectionConfig connectionConfig = ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofMilliseconds(Long.parseLong(connectionTimeout)))
                                .setSocketTimeout(Timeout.ofMilliseconds(Long.parseLong(socketTimeout)))
                                .build();

                PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
                connectionManager.setMaxTotal(50);
                connectionManager.setDefaultMaxPerRoute(20);
                connectionManager.setDefaultConnectionConfig(connectionConfig);

                CloseableHttpClient httpClient = HttpClients.custom()
                                .setConnectionManager(connectionManager)
                                .evictIdleConnections(org.apache.hc.core5.util.TimeValue.ofSeconds(30))
                                .build();

                HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                                httpClient);

                return RestClient.builder()
                                .requestFactory(requestFactory)
                                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                                .build();
        }
}