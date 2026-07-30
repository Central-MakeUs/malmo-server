package makeus.cmc.malmo.config;

import java.util.List;
import java.util.stream.Stream;

final class CorsAllowedOriginResolver {

    private CorsAllowedOriginResolver() {
    }

    static List<String> merge(List<String> configuredOrigins, List<String> webReturnOrigins) {
        return Stream.concat(configuredOrigins.stream(), webReturnOrigins.stream())
                .filter(origin -> origin != null && !origin.isBlank())
                .distinct()
                .toList();
    }
}
