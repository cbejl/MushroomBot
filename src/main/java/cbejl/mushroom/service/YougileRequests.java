package cbejl.mushroom.service;

import cbejl.mushroom.config.BotConfig;
import cbejl.mushroom.gsonTranscript.RecUser;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.jvnet.hk2.internal.Collector;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class YougileRequests {
    final BotConfig botConfig;

    public YougileRequests(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    public void yougilePostRequest(URI uri, String json) {
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + botConfig.getYougileToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        try {
            HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
           log.error("Ошибка! Ошибка! Не отправляется! Чёта IOException!" +
                   "\nError occured: " + e.getMessage());
        } catch (InterruptedException e) {
            log.error("Ошибка! Ошибка! Не отправляется! Чёта InterruptedException!" +
                    "\nError occured: " + e.getMessage());
        }
    }

    public String yougileGetRequest(URI uri) {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + botConfig.getYougileToken())
                .header("Content-Type", "application/json").GET()
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        try {
            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
            return getResponse.body();
        } catch (IOException e) {
            log.error("Ошибка! Ошибка! Не отправляется! Чёта IOException!" +
                    "\nError occured: " + e.getMessage());
        } catch (InterruptedException e) {
            log.error("Ошибка! Ошибка! Не отправляется! Чёта InterruptedException!" +
                    "\nError occured: " + e.getMessage());
        }
        return "";
    }

    public Map<Map.Entry<String, Integer>, String> dateSearcher() {
        try {
            Gson g = new Gson();
            String scheduleTasksJson = yougileGetRequest(new URI(botConfig.getYougileUriTasks()
                    + "?columnId=" + botConfig.getColumnSchedule()));
            RecUser scheduleTasks = g.fromJson(scheduleTasksJson, RecUser.class);
            RecUser.Content[] content = scheduleTasks.getContent();


            Map<Map.Entry<String, Integer>, String> schedule = new LinkedHashMap<>();
            for (int i = content.length - 1; i >= 0; i--) {
                if(!content[i].completed) {
                    Map<String, Integer> taskMap = new LinkedHashMap<>();
                    taskMap.put(content[i].title, i);
                    schedule.put(taskMap.entrySet().stream().findFirst().get(), content[i].getDescription());
                }
            }

            return schedule;

        } catch (URISyntaxException e) {
            log.error("Плохо ссылку на расписание ввёл..." +
                    "\nError occurred: " + e.getMessage());
        }
        return null;
    }

    private void mapReverser(Map<Map.Entry<String, Integer>, String> map) {
        Map<Integer, List<String>> sortedMap =
                new TreeMap<Integer, List<String>>(Collections.reverseOrder());
    }

}
