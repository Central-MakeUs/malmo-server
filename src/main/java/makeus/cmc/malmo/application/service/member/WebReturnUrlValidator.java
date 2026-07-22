package makeus.cmc.malmo.application.service.member;

import makeus.cmc.malmo.application.exception.InvalidWebOAuthRequestException;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class WebReturnUrlValidator {

    private final Set<String> allowedOrigins;

    public WebReturnUrlValidator(List<String> allowedOrigins) {
        this.allowedOrigins = new HashSet<>();
        allowedOrigins.stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .forEach(origin -> this.allowedOrigins.add(normalizeOrigin(URI.create(origin))));
    }

    public void validate(String returnUrl) {
        try {
            URI uri = URI.create(returnUrl);
            if (!uri.isAbsolute() || uri.getUserInfo() != null || uri.getFragment() != null
                    || !allowedOrigins.contains(normalizeOrigin(uri))) {
                throw new InvalidWebOAuthRequestException("허용되지 않은 웹 로그인 반환 URL입니다.");
            }
        } catch (InvalidWebOAuthRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new InvalidWebOAuthRequestException("허용되지 않은 웹 로그인 반환 URL입니다.", e);
        }
    }

    private static String normalizeOrigin(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            throw new IllegalArgumentException("올바른 origin 형식이 아닙니다.");
        }
        int port = uri.getPort();
        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (!"https".equals(normalizedScheme)
                && !("http".equals(normalizedScheme) && "localhost".equals(normalizedHost))) {
            throw new IllegalArgumentException("HTTPS origin만 허용됩니다.");
        }
        boolean defaultPort = port == -1
                || ("https".equals(normalizedScheme) && port == 443)
                || ("http".equals(normalizedScheme) && port == 80);
        return normalizedScheme + "://" + normalizedHost + (defaultPort ? "" : ":" + port);
    }
}
