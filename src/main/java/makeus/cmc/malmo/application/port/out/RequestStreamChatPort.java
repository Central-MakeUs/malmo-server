package makeus.cmc.malmo.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface RequestStreamChatPort {
    void streamChat(
            List<Map<String, String>> messages,
            Consumer<String> onData,
            Consumer<String> onCompleteFullResponse,
            Consumer<String> onError);
}
