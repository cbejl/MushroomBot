package cbejl.mushroom.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {
    @Value("${tg.messages.path}")
    private String messagesPath;

    @Value("${tg.photos.path}")
    private String photos;

    @Value("${tg.name}")
    private String tgName;

    @Value("${tg.token}")
    private String tgToken;

    @Value("${yougile.token}")
    private String yougileToken;

    @Value("${yougile.column.recs}")
    private String columnRecs;

    @Value("${yougile.uri.tasks}")
    private String yougileUriTasks;

    @Value("${yougile.column.schedule}")
        private String columnSchedule;

    @Value("${tg.adminsId}")
        private String[] admins;

}
