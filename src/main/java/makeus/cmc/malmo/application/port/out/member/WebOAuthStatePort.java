package makeus.cmc.malmo.application.port.out.member;

import makeus.cmc.malmo.domain.value.type.Provider;

public interface WebOAuthStatePort {

    void save(State state);

    State consume(String state);

    record State(
            Provider provider,
            String state,
            String nonce,
            String codeVerifier,
            String returnUrl,
            String deviceId
    ) {
    }
}
