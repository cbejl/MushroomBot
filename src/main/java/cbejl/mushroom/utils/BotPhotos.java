package cbejl.mushroom.utils;

import cbejl.mushroom.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

@Slf4j
@Component
public class BotPhotos {
    final BotConfig config;
    Map<String, String> photosMap;
    public BotPhotos(BotConfig config){
        this.config = config;
        this.photosMap = getPhotosMap();
    }

    private Map<String, String> getPhotosMap() {
        try (InputStream io = new FileInputStream(config.getPhotos())) {
            return new Yaml().load(io);
        } catch (IOException e) {
            log.error("Ошибка в чтении фото id из файла!" +
                    "\nError occurred: " + e.getMessage());
        }
        return null;
    }

    public String getPhoto(String type) {
        return photosMap.get(type);
    }
}
