package cbejl.mushroom.service;

import cbejl.mushroom.config.BotConfig;
import cbejl.mushroom.utils.BotMessages;
import cbejl.mushroom.gsonTranscript.GsonTasks;
import cbejl.mushroom.gsonTranscript.RecUser;
import cbejl.mushroom.utils.BotPhotos;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;
    BotMessages botMessages;
    BotPhotos botPhotos;
    YougileRequests yougileRequests;
    final String PORTFOLIO = "PORTFOLIO", REC = "REC", ABOUT = "ABOUT", CONTACTS = "CONTACTS",
            BACK = "BACK", SET_PORTFOLIO = "SET_PORTFOLIO", ASSIGNED = "ASSIGNED";

    boolean set_portfolio_flag = false;

    Map<Long, String> accepts;

    public TelegramBot(BotConfig config, BotMessages botMessages, YougileRequests yougileRequests, BotPhotos botPhotos) {
        this.config = config;
        this.botMessages = botMessages;
        this.botPhotos = botPhotos;
        this.yougileRequests = yougileRequests;
        this.accepts = new HashMap<>();
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Запуск бота"));
        listOfCommands.add(new BotCommand("/about", "О нашей студии"));
        listOfCommands.add(new BotCommand("/rec", "Запись на тату/пирсинг"));
        listOfCommands.add(new BotCommand("/assigned", "Посмотреть запись"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка при создании списка комманд!" +
                    "\nError occurred: " + e.getMessage());
        }

    }


    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            log.info("Получено сообщение: \"" + messageText + "\"" +
                    "\nОт пользователя: @" + update.getMessage().getChat().getUserName());
            switch (messageText) {
                case "/start":
                    sendPhotoMessage(chatId, botMessages.getMessage("hello"),
                            botPhotos.getPhoto("hello"), startCommandButtons(
                                    update.getMessage().getFrom().getFirstName(),
                                    update.getMessage().getFrom().getLastName(),
                                    update.getMessage().getFrom().getId()));
                    accepts.entrySet().removeIf(e -> e.getKey() == chatId);
                    break;
                case "/about":
                    sendPhotoMessage(chatId, botMessages.getMessage("about"),
                            botPhotos.getPhoto("about"), backButton());
                    accepts.entrySet().removeIf(e -> e.getKey() == chatId);
                    break;
                case "/rec":
                    sendPhotoMessage(chatId, botMessages.getMessage("recdate"),
                            botPhotos.getPhoto("recdate"), recButtons());
                    accepts.entrySet().removeIf(e -> e.getKey() == chatId);
                    break;
                case "/assigned":
                    if(!checkRec(update.getMessage().getChat().getFirstName(),
                            update.getMessage().getChat().getLastName(), chatId)) {
                        sendPhotoMessage(chatId, botMessages.getMessage("assigned") + " *"
                                        + user(update.getMessage().getChat().getFirstName(),
                                        update.getMessage().getChat().getLastName()).getContent()[0]
                                        .getDescription().split("</p><p>")[0].replace("<p>", "") + "*",
                                botPhotos.getPhoto("assigned"), backButton());
                    } else {

                        var rec = new InlineKeyboardButton();
                        rec.setText(botMessages.getMessage("b_rec"));
                        rec.setCallbackData(REC);

                        sendPhotoMessage(chatId, botMessages.getMessage("not_assigned"),
                                botPhotos.getPhoto("not_assigned"), new InlineKeyboardMarkup(Arrays.asList(Arrays.asList(rec))));
                    }
                    accepts.entrySet().removeIf(e -> e.getKey() == chatId);
                    break;
                case "/accept":
                    accepts.forEach(((aLong, s) -> {
                        if (aLong == chatId) {
                            recording(update.getMessage().getChat().getFirstName(),
                                    update.getMessage().getChat().getLastName(),
                                    update.getMessage().getChat().getUserName(),
                                    chatId, s);
                        }
                    }));
                    accepts.entrySet().removeIf(e -> e.getKey() == chatId);
                    break;
                case "/test":

                    break;
                case "/admin":
                    if(checkAdmin(chatId)) {
                        sendMessage(chatId, botMessages.getMessage("admin"), adminButtons());
                    }
                default:
                    if(messageText.contains("/accept")) {
                        accepts.forEach(((aLong, s) -> {
                            if (aLong == chatId) {
                                recording(update.getMessage().getChat().getFirstName(),
                                        update.getMessage().getChat().getLastName(),
                                        update.getMessage().getChat().getUserName(),
                                        chatId, s + "</p><p>"
                                        + messageText.replace("/accept", ""));
                            }
                        }));
                    }
                    accepts.entrySet().removeIf(e -> e.getKey() == chatId);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            System.out.println(callbackData);

            switch (callbackData) {
                case REC:
                    editPhotoMessage(chatId, (int) messageId, botMessages.getMessage("recdate"),
                            botPhotos.getPhoto("recdate"), recButtons());
                    break;
                case ABOUT:
                    editPhotoMessage(chatId, (int) messageId, botMessages.getMessage("about"),
                            botPhotos.getPhoto("about"), backButton());
                    break;
                case PORTFOLIO:
                    editPhotoMessage(chatId, (int) messageId, botMessages.getMessage("portfolio"),
                            botPhotos.getPhoto("portfolio"), backButton());
                    break;
                case CONTACTS:
                    editPhotoMessage(chatId, (int) messageId, botMessages.getMessage("contacts"),
                            botPhotos.getPhoto("contacts"), backButton());
                    break;
                case BACK:
                    editPhotoMessage(chatId, (int) messageId, botMessages.getMessage("hello"),
                            botPhotos.getPhoto("hello"), startCommandButtons(
                                    update.getCallbackQuery().getFrom().getFirstName(),
                                    update.getCallbackQuery().getFrom().getLastName(),
                                    update.getCallbackQuery().getFrom().getId()));
                    accepts.entrySet().removeIf(e -> e.getKey() == chatId);
                    break;
                case SET_PORTFOLIO:
                    sendMessage(chatId, botMessages.getMessage("set_portfolio"));
                    set_portfolio_flag = true;
                    break;
                case ASSIGNED:
                    editPhotoMessage(chatId, (int) messageId, botMessages.getMessage("assigned") + " *" +
                            user(update.getCallbackQuery().getFrom().getFirstName(),
                                    update.getCallbackQuery().getFrom().getLastName()).getContent()[0]
                                    .getDescription().split("</p><p>")[0].replace("<p>", "") + "*",
                            botPhotos.getPhoto("assigned"),
                            backButton());
                    break;
                default:
                    if(callbackData.matches("date(.*)")) {
                        String date = callbackData.split("[|]")[1];
                        editPhotoMessage(chatId, (int) messageId, botMessages.getMessage("rectime"),
                                botPhotos.getPhoto("rectime"), recTimeButtons(date));
                    } else if (callbackData.matches("time(.*)")) {
                        String[] times = new String[] {
                                callbackData.split("[|]")[1],
                                callbackData.split("[|]")[2]
                        };
                        editPhotoMessage(chatId, (int) messageId, botMessages.getMessage("recsure1") + "*"
                                        + times[0] + " *(*" + times[1] + "*)"
                                        + botMessages.getMessage("recsure2"),
                                botPhotos.getPhoto("recsure"), backButton());
                        accepts.put(chatId, times[0] + " | " + times[1]);
                    }
            }
        } else if (update.hasMessage() && !update.getMessage().getPhoto().isEmpty()
                && checkAdmin(update.getMessage().getChatId())) {
            List<PhotoSize> photos = update.getMessage().getPhoto();
            long chatId = update.getMessage().getChatId();

            if(set_portfolio_flag) {
                String f_id = photos.stream()
                        .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                        .findFirst()
                        .orElse(null).getFileId();

                sendMessage(chatId, String.valueOf(f_id));
                set_portfolio_flag = false;
            }

        }

    }

    private InlineKeyboardMarkup startCommandButtons(String firstName, String lastName, long chatId) {
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var portfolio = new InlineKeyboardButton();
        portfolio.setText(botMessages.getMessage("b_portfolio"));
        portfolio.setCallbackData(PORTFOLIO);

        var rec = new InlineKeyboardButton();
        rec.setText(botMessages.getMessage("b_rec"));
        rec.setCallbackData(REC);

        var about = new InlineKeyboardButton();
        about.setText(botMessages.getMessage("b_about"));
        about.setCallbackData(ABOUT);

        var contact = new InlineKeyboardButton();
        contact.setText(botMessages.getMessage("b_contacts"));
        contact.setCallbackData(CONTACTS);

        rowInLine.add(portfolio);
        rowInLine.add(rec);
        rowsInLine.add(new ArrayList<>(rowInLine));
        rowInLine.clear();
        rowInLine.add(about);
        rowInLine.add(contact);
        rowsInLine.add(new ArrayList<>(rowInLine));

        if (!checkRec(firstName, lastName, chatId)) {
            var assigned = new InlineKeyboardButton();
            assigned.setText(botMessages.getMessage("b_assigned"));
            assigned.setCallbackData(ASSIGNED);
            rowsInLine.add(Arrays.asList(assigned));
        }

        markupInLine.setKeyboard(rowsInLine);
        return markupInLine;
    }

    private InlineKeyboardMarkup recTimeButtons(String date) {
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        Set<Map.Entry<Map.Entry<String, Integer>, String>> set = yougileRequests.dateSearcher().entrySet();

        String[] times = Arrays.stream(set.stream().filter(x -> x.getKey().getKey().equals(date))
                .map(x -> x.getValue())
                .toList().get(0).split("</p><p>"))
                .map(x -> {
            x = x.replace("<p>","");
            x = x.replace("</p>", "");
            return x;
        }).toArray(String[]::new);

        for (String time : times) {
            var timeb = new InlineKeyboardButton();
            timeb.setText(time);
            timeb.setCallbackData("time|" + time + "|" + date);
            rowInLine.add(timeb);
            rowsInLine.add(new ArrayList<>(rowInLine));
            rowInLine.clear();
        }

        var back = new InlineKeyboardButton();
        back.setText(botMessages.getMessage("b_back"));
        back.setCallbackData(REC);

        rowInLine.add(back);
        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);

        return markupInLine;
    }

    private InlineKeyboardMarkup recButtons() {
        Set<Map.Entry<Map.Entry<String, Integer>, String>> set = yougileRequests.dateSearcher().entrySet();

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        int counter1 = 0;
        int counter2 = 0;
        for(Map.Entry<Map.Entry<String, Integer>, String> entry : set) {
            var date = new InlineKeyboardButton();
            date.setText(entry.getKey().getKey());
            date.setCallbackData("date" + entry.getKey().getValue().toString() + "|" + entry.getKey().getKey());
            counter2 += 1;

            if (counter2 == set.size()) {
                if (counter1 < 2) {
                    rowInLine.add(date);
                    rowsInLine.add(new ArrayList<>(rowInLine));
                } else {
                    rowsInLine.add(new ArrayList<>(rowInLine));
                    rowInLine.clear();
                    rowInLine.add(date);
                    rowsInLine.add(new ArrayList<>(rowInLine));
                }
            } else if (counter1 < 2) {
                rowInLine.add(date);
                counter1 += 1;
            } else {
                counter1 = 1;
                rowsInLine.add(new ArrayList<>(rowInLine));
                rowInLine.clear();
                rowInLine.add(date);
            }
        }

        rowInLine.clear();
        rowInLine.add(backButton().getKeyboard().get(0).get(0));
        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);

        return markupInLine;
    }

    private InlineKeyboardMarkup backButton() {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var back = new InlineKeyboardButton();
        back.setText(botMessages.getMessage("b_back"));
        back.setCallbackData(BACK);

        rowInLine.add(back);
        rowsInLine.add(rowInLine);
        return new InlineKeyboardMarkup(rowsInLine);
    }

    private InlineKeyboardMarkup adminButtons() {
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var setPortfolio = new InlineKeyboardButton();
        setPortfolio.setText(botMessages.getMessage("b_set_portfolio"));
        setPortfolio.setCallbackData(SET_PORTFOLIO);

        rowInLine.add(setPortfolio);
        rowsInLine.add(new ArrayList<>(rowInLine));

        return new InlineKeyboardMarkup(rowsInLine);
    }

    private void recording(String firstName, String lastName, String username, long chatId, String time) {
        if (checkRec(firstName, lastName, chatId)) {
            try {
                yougileRequests.yougilePostRequest(new URI(config.getYougileUriTasks()), generateTask(firstName
                                + " " + lastName,
                        time + "</p><p>"
                                + "https://t.me/" + username + "</p><p>"
                                + chatId));
            } catch (URISyntaxException e) {
                sendMessage(chatId, botMessages.getMessage("recerror"));
                log.error("Ошшибка отправки запроса на югил!" +
                        "\n Error occurred: " + e.getMessage());
            }
            sendPhotoMessage(chatId, botMessages.getMessage("rec"), botPhotos.getPhoto("rec"), backButton());
        } else {
            sendPhotoMessage(chatId, botMessages.getMessage("recdone") + " *" + user(firstName, lastName).getContent()[0]
                    .getDescription().split("</p><p>")[0].replace("<p>", "") + "*",
                    botPhotos.getPhoto("recdone"), backButton());
        }
    }

    private boolean checkRec(String firstName, String lastName, long chatId) {
        Gson g = new Gson();
        RecUser user = user(firstName, lastName);
        if(user.content.length > 0) {
            return !user.content[0]
                    .description.split("</p><p>")[user.content[0].description.split("</p><p>").length - 1]
                    .replace("</p>", "").replace("<p>", "")
                    .equals(String.valueOf(chatId))
                    || (user.content[0].description.split("</p><p>")[user.content[0].description.split("</p><p>").length - 1]
                    .replace("</p>", "").replace("<p>", "")
                    .equals(String.valueOf(chatId)) && user.content[0].completed);
        } else {
            return true;
        }
    }

    private boolean checkAdmin(long chatId) {
        return Arrays.stream(config.getAdmins()).anyMatch(String.valueOf(chatId)::equals);
    }

    private RecUser user(String firstName, String lastName) {
        Gson g = new Gson();
        try {
            return g.fromJson(yougileRequests.yougileGetRequest(new URI(config.getYougileUriTasks() + "?title="
                    + firstName.replace(" ", "%20")
                    + "%20" + lastName.replace(" ", "%20") + "&columnId=" + config.getColumnRecs())), RecUser.class);
        } catch (URISyntaxException e) {
            log.error("Ошибка при запросе списка записей!" +
                    "\nError occurred: " + e.getMessage());
        }
        return new RecUser();
    }

    private String generateTask(String clientName, String description) {
        GsonTasks.Checklist.Item item1 = new GsonTasks.Checklist.Item(botMessages.getMessage("yougile_subtask_1"));
        GsonTasks.Checklist.Item item2 = new GsonTasks.Checklist.Item(botMessages.getMessage("yougile_subtask_2"));

        GsonTasks.Checklist cl = new GsonTasks.Checklist(botMessages.getMessage("yougile_process"),
                new GsonTasks.Checklist.Item[]{item1, item2});

        GsonTasks agt = new GsonTasks(clientName, config.getColumnRecs(),
                description, false, false, new GsonTasks.Checklist[] {cl});

        return new Gson().toJson(agt);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(textToSend);
        message.enableMarkdown(true);

        messageExecute(message);
    }

    private void sendMessage(long chatId, String textToSend, InlineKeyboardMarkup buttons) {
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(textToSend);
        message.setReplyMarkup(buttons);
        message.enableMarkdown(true);

        messageExecute(message);
    }

    private void messageExecute(SendMessage message) {
        try {
            execute(message);
            log.info("Отправлено сообщение \"" + message.getText() + "\" в чат #" + message.getChatId());
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения!" +
                    "\nError occurred: " + e.getMessage());
        }
    }

    private void editMessage(long chatId, int messageId, String text) {
        EditMessageText message = new EditMessageText();
        message.setText(text);
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.enableMarkdown(true);

        editMessageExecute(message);
    }

    private void editMessage(long chatId, int messageId, String text, InlineKeyboardMarkup buttons) {
        EditMessageText message = new EditMessageText();
        message.setText(text);
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setReplyMarkup(buttons);
        message.enableMarkdown(true);

       editMessageExecute(message);
    }

    private void editMessageExecute(EditMessageText message) {
        try {
            execute(message);
            log.info("Отправлено сообщение \"" + message.getText() + "\" в чат #" + message.getChatId());
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения!" +
                    "\nError occurred: " + e.getMessage());
        }
    }

    private void sendPhotoMessage(long chatId, String textToSend, String photo_id) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId);
        photo.setParseMode(ParseMode.MARKDOWN);
        photo.setCaption(textToSend);
        photo.setPhoto(new InputFile(photo_id));

        sendMediaMessageExecute(photo);
    }

    private void sendPhotoMessage(long chatId, String textToSend, String photo_id, InlineKeyboardMarkup buttons) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId);
        photo.setParseMode(ParseMode.MARKDOWN);
        photo.setCaption(textToSend);
        photo.setPhoto(new InputFile(photo_id));
        photo.setReplyMarkup(buttons);

        sendMediaMessageExecute(photo);
    }

    private void editPhotoMessage(long chatId, int messageId, String text, String photo_id, InlineKeyboardMarkup buttons) {
        EditMessageMedia media = new EditMessageMedia();
        InputMedia photo = new InputMediaPhoto();
        photo.setMedia(photo_id);
        photo.setParseMode(ParseMode.MARKDOWN);
        photo.setCaption(text);

        media.setMedia(photo);
        media.setMessageId(messageId);
        media.setChatId(chatId);
        media.setReplyMarkup(buttons);

        editMediaMessageExecute(media);
    }

    private void sendMediaMessageExecute(SendPhoto media) {
        try {
            execute(media);
            log.info("Отправлено сообщение \"" + media.getCaption() + "\" в чат #" + media.getChatId());
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения!" +
                    "\nError occurred: " + e.getMessage());
        }
    }

    private void editMediaMessageExecute(EditMessageMedia media) {
        try {
            execute(media);
            log.info("Отправлено сообщение \"" + media.getMedia().getCaption() + "\" в чат #" + media.getChatId());
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения!" +
                    "\nError occurred: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getTgName();
    }

    @Override
    public String getBotToken() {
        return config.getTgToken();
    }
}
