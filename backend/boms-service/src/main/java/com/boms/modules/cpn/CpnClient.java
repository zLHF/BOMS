package com.boms.modules.cpn;

import com.boms.modules.cpn.dto.CpnBaseResp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;

/** 算力平台 HTTP 客户端（签名 + 调用）。 */
@Slf4j
@Component
public class CpnClient {

    private final CpnConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    public CpnClient(CpnConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /* ---------- 身份校验（code → token） ---------- */

    public String authCheck(String code) {
        String url = config.getGateway() + "/extral/" + config.getAppKey() + "/appAuthCheck";
        Map<String, String> body = Map.of(
                "appID", config.getAppKey(),
                "appSecret", config.getAppSecret(),
                "code", code
        );
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, body, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.path("code").asInt() == 20000) {
                return root.path("data").asText();
            }
            log.warn("[CpnClient] authCheck failed: {}", resp.getBody());
            return null;
        } catch (Exception e) {
            log.error("[CpnClient] authCheck error: {}", e.getMessage());
            return null;
        }
    }

    /* ---------- 用户信息（token → user + tenant） ---------- */

    public JsonNode getUserInfoByToken(String token) {
        String url = config.getGateway() + "/api/extral/" + config.getAppKey() + "/getUserInfoByToken";
        Map<String, String> body = Map.of("appID", config.getAppKey());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (root.path("code").asInt() == 20000) {
                return root.path("data");
            }
            log.warn("[CpnClient] getUserInfoByToken failed: {}", resp.getBody());
            return null;
        } catch (Exception e) {
            log.error("[CpnClient] getUserInfoByToken error: {}", e.getMessage());
            return null;
        }
    }

    /* ---------- 心跳上报 ---------- */

    public boolean heartbeat() {
        String url = config.getGateway() + "/api/extra/v1/application/heartbeat";
        try {
            HttpHeaders headers = buildSignHeaders("");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            boolean ok = root.path("code").asInt() == 20000;
            if (ok) {
                log.debug("[CpnClient] heartbeat ok");
            } else {
                log.warn("[CpnClient] heartbeat failed: {}", resp.getBody());
            }
            return ok;
        } catch (Exception e) {
            log.error("[CpnClient] heartbeat error: {}", e.getMessage());
            return false;
        }
    }

    /* ---------- 签名工具 ---------- */

    /**
     * 构建签名认证请求头。
     * 算法：MD5(body) → 拼接 x-body&x-random&x-time → HMAC-SHA256(appSecret, 拼接串)
     */
    private HttpHeaders buildSignHeaders(String body) {
        String xTime = String.valueOf(System.currentTimeMillis());
        String xRandom = randomHex(16);
        String xBody = md5(body != null ? body : "");

        String signSrc = xBody + "&" + xRandom + "&" + xTime;
        String xSign = hmacSha256(config.getAppSecret(), signSrc);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("appKey", config.getAppKey());
        headers.set("x-time", xTime);
        headers.set("x-random", xRandom);
        headers.set("x-sign", xSign);
        return headers;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return "";
        }
    }

    private String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }

    private String randomHex(int len) {
        byte[] bytes = new byte[len / 2];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes).substring(0, len);
    }
}
