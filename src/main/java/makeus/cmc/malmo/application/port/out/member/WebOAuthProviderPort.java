package makeus.cmc.malmo.application.port.out.member;

import java.net.URI;

public interface WebOAuthProviderPort {

    URI authorizationUri(AuthorizationRequest request);

    Identity exchange(AuthorizationCode authorizationCode);

    record AuthorizationRequest(
            String state,
            String nonce
    ) {
    }

    record AuthorizationCode(
            String code,
            String state,
            String nonce
    ) {
    }

    record Identity(
            String providerId,
            String email
    ) {
    }
}
