package makeus.cmc.malmo.adaptor.out.oidc;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import makeus.cmc.malmo.adaptor.out.exception.OidcIdTokenException;

import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class WebOidcTokenValidator {

    private final String issuer;
    private final String audience;
    private final JwkProvider jwkProvider;
    private final Clock clock;

    public WebOidcTokenValidator(String issuer, String audience, String jwksUri, Clock clock) {
        this.issuer = issuer;
        this.audience = audience;
        this.clock = clock;
        try {
            this.jwkProvider = new JwkProviderBuilder(URI.create(jwksUri).toURL())
                    .cached(10, 60, TimeUnit.MINUTES)
                    .rateLimited(10, 1, TimeUnit.MINUTES)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("웹 OIDC JWKS URI 설정이 올바르지 않습니다.", e);
        }
    }

    public Claims validate(String idToken, String expectedNonce) {
        try {
            DecodedJWT jwt = JWT.decode(idToken);
            Jwk jwk = jwkProvider.get(jwt.getKeyId());
            Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null).verify(jwt);

            Date expiresAt = jwt.getExpiresAt();
            if (!Objects.equals(issuer, jwt.getIssuer())
                    || jwt.getAudience() == null
                    || !jwt.getAudience().contains(audience)
                    || expiresAt == null
                    || !expiresAt.after(Date.from(clock.instant()))
                    || !Objects.equals(expectedNonce, jwt.getClaim("nonce").asString())) {
                throw new OidcIdTokenException("Invalid web OIDC ID token claims");
            }
            if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
                throw new OidcIdTokenException("Missing subject in web OIDC ID token");
            }
            return new Claims(jwt.getSubject(), jwt.getClaim("email").asString());
        } catch (OidcIdTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new OidcIdTokenException("Failed to validate web OIDC ID token", e);
        }
    }

    public record Claims(String subject, String email) {
    }
}
