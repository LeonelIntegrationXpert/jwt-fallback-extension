package com.leonel.mulesoft.security.jwtfallback.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.io.ByteArrayInputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Request;

public class MultiIdPJWTValidationOperations {

	@MediaType(value = ANY, strict = false)
	public boolean validateWithFallback(String token, String urlsCsv,

			@Optional(defaultValue = "false") boolean requireExpiration,
			@Optional(defaultValue = "false") boolean requireNotBefore,
			@Optional(defaultValue = "false") boolean requireJWTId,

			@Optional Map<String, String> customClaimsToValidate) {

		try {
			String jwt = token.replaceFirst("(?i)^Bearer\\s+", "").trim();
			String[] urls = urlsCsv.split(",");

			System.out.println("[JWT-Fallback] Token recebido: " + jwt);
			System.out.println("[JWT-Fallback] URLs JWKS recebidas: " + Arrays.toString(urls));

			for (String url : urls) {
				try {
					System.out.println("[JWT-Fallback] Tentando URL: " + url);
					String x5c = getFirstX5CFromJWKS(url.trim());

					if (x5c == null) {
						System.out.println("[JWT-Fallback] Nenhum x5c encontrado na URL: " + url);
						continue;
					}

					System.out.println("[JWT-Fallback] x5c extraído com sucesso da URL: " + url);

					if (validateJwt(jwt, x5c, requireExpiration, requireNotBefore, requireJWTId,
							customClaimsToValidate)) {
						System.out.println("[JWT-Fallback] Validação bem-sucedida com a URL: " + url);
						return true;
					} else {
						System.out.println("[JWT-Fallback] Validação falhou com a URL: " + url);
					}
				} catch (Exception ex) {
					System.out.println(
							"[JWT-Fallback] Erro ao tentar validar com a URL: " + url + " → " + ex.getMessage());
				}
			}

			System.out.println("[JWT-Fallback] Nenhuma das URLs resultou em validação positiva.");
			return false;
		} catch (Exception e) {
			System.out.println("[JWT-Fallback] Erro geral na validação do JWT: " + e.getMessage());
			return false;
		}
	}

	private String getFirstX5CFromJWKS(String jwksUrl) throws Exception {
		try {
			String response = Request.Get(jwksUrl).connectTimeout(3000).socketTimeout(3000).execute().returnContent()
					.asString();
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> json = mapper.readValue(response, Map.class);

			if (!json.containsKey("keys"))
				return null;

			Map<String, Object> firstKey = ((List<Map<String, Object>>) json.get("keys")).get(0);
			List<String> x5cList = (List<String>) firstKey.get("x5c");

			return x5cList != null && !x5cList.isEmpty() ? x5cList.get(0) : null;
		} catch (Exception e) {
			System.out.println("[JWT-Fallback] Erro ao buscar JWKS na URL: " + jwksUrl + " → " + e.getMessage());
			throw e;
		}
	}

	private boolean validateJwt(String jwt, String base64Cert, boolean requireExp, boolean requireNbf,
			boolean requireJti, Map<String, String> customClaimsToValidate) {
		try {
			byte[] certBytes = Base64.getDecoder().decode(base64Cert);
			CertificateFactory factory = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
			PublicKey publicKey = cert.getPublicKey();

			String[] parts = jwt.split("\\.");
			if (parts.length != 3)
				return false;

			String headerAndPayload = parts[0] + "." + parts[1];
			byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);

			String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), "UTF-8");
			Map<String, Object> payload = new ObjectMapper().readValue(payloadJson, Map.class);

			String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), "UTF-8");
			Map<String, Object> header = new ObjectMapper().readValue(headerJson, Map.class);

			String alg = (String) header.get("alg");
			String javaAlg;
			switch (alg) {
			case "RS256":
				javaAlg = "SHA256withRSA";
				break;
			case "RS384":
				javaAlg = "SHA384withRSA";
				break;
			case "RS512":
				javaAlg = "SHA512withRSA";
				break;
			default:
				throw new RuntimeException("Algoritmo não suportado: " + alg);
			}

			Signature signature = Signature.getInstance(javaAlg);
			signature.initVerify(publicKey);
			signature.update(headerAndPayload.getBytes("UTF-8"));
			boolean isVerified = signature.verify(signatureBytes);

			System.out.println("[JWT-Fallback] Assinatura válida? " + isVerified);
			if (!isVerified)
				return false;

			long now = System.currentTimeMillis() / 1000;

			if (requireExp && (!payload.containsKey("exp") || now >= ((Number) payload.get("exp")).longValue())) {
				System.out.println("[JWT-Fallback] exp inválido ou ausente");
				return false;
			}

			if (requireNbf && payload.containsKey("nbf") && now < ((Number) payload.get("nbf")).longValue()) {
				System.out.println("[JWT-Fallback] nbf ainda não atingido");
				return false;
			}

			if (requireJti && !payload.containsKey("jti")) {
				System.out.println("[JWT-Fallback] jti obrigatório mas ausente");
				return false;
			}

			if (customClaimsToValidate != null) {
				for (Map.Entry<String, String> entry : customClaimsToValidate.entrySet()) {
					String key = entry.getKey();
					String expected = entry.getValue();
					Object actual = payload.containsKey(key) ? payload.get(key) : header.get(key);

					System.out.println("[JWT-Fallback] Validando claim: " + key + " → esperado: [" + expected
							+ "], recebido: [" + actual + "]");

					if (!matchClaim(actual, expected)) {
						System.out.println("[JWT-Fallback] Claim '" + key + "' não corresponde ao esperado.");
						return false;
					}
				}
			}

			System.out.println("[JWT-Fallback] Todas as validações passaram com sucesso.");
			return true;
		} catch (Exception e) {
			System.out.println("[JWT-Fallback] Erro ao validar JWT: " + e.getMessage());
			return false;
		}
	}

	private boolean matchClaim(Object claimValue, String expectedCsv) {
		if (expectedCsv == null || expectedCsv.trim().isEmpty())
			return true;
		if (claimValue == null)
			return false;

		List<String> expectedValues = Arrays.asList(expectedCsv.split(","));
		return expectedValues.contains(claimValue.toString().trim());
	}
}
