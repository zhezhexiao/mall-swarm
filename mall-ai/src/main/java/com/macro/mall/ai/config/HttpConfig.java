package com.macro.mall.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;

/**
 * 强制 JDK HttpClient 走 HTTP/1.1 — DeepSeek API 在 HTTP/2 下返回 RST_STREAM。
 */
@Configuration
public class HttpConfig {

    @Bean
    public RestClient.Builder restClientBuilder() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new java.security.SecureRandom());

        HttpClient jdkClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .sslContext(sslContext)
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkClient);

        return RestClient.builder().requestFactory(factory);
    }
}
