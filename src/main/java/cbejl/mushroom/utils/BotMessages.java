package cbejl.mushroom.utils;

import cbejl.mushroom.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


@Slf4j
@Component
public class BotMessages {
    final BotConfig config;
    Map<String, String> messageMap;
    public BotMessages(BotConfig config){
        this.config = config;
        this.messageMap = getMessagesMap();
    }

    private Map<String, String> getMessagesMap() {
        try (InputStream io = new FileInputStream(config.getMessagesPath())) {
            return new Yaml().load(io);
        } catch (IOException e) {
            log.error("Ошибка в чтении сообщений из файла!" +
                    "\nError occurred: " + e.getMessage());
        }
        return null;
    }

    public String getMessage(String message) {
        return messageMap.get(message);
    }
}
