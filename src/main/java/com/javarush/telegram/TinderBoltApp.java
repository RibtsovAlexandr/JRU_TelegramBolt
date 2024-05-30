package com.javarush.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "JRU_***t"; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = "6771707449:***Q"; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = "sk-proj-****"; //TODO: добавь токен ChatGPT в кавычках

    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentMode = null;
    private ArrayList<String> list = new ArrayList<>();
    private UserInfo me, she;
    private int questionCount;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();

        if (message.equals("/start")){
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String startText = loadMessage("main");
            sendTextMessage(startText);

            showMainMenu("главное меню бота", "/start",
                    "генерация Tinder-профля \uD83D\uDE0E", "/profile",
                    "сообщение для знакомства \uD83E\uDD70", "/opener",
                    "переписка от вашего имени \uD83D\uDE08", "/message",
                    "переписка со звездами \uD83D\uDD25", "/date",
                    "задать вопрос чату GPT \uD83E\uDDE0", "/gpt");
            return;
        }
        if (message.equals("/gpt")){
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String welcomeGPTText = loadMessage("gpt");
            sendTextMessage(welcomeGPTText);
            return;
        }

        if (currentMode == DialogMode.GPT && !isMessageCommand()){
            String prompt = loadPrompt("gpt");
            //String answer = "!! under construction !!";
            Message msg = sendTextMessage("Подождите немного пока готовися ответ...");
            String answer = chatGPT.sendMessage(prompt, message);
            updateTextMessage(msg, answer);
            return;
        }
        // обработчик DATE режимов
        if (message.equals("/date")){
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String welcomeDateText = loadMessage("date");
            sendTextButtonsMessage( welcomeDateText,
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райан Гослинг","date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }
        if (currentMode == DialogMode.DATE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")){
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор! \nТвоя задача пригласить на свидание ❤\uFE0F за 5 сообщений! :)");
                String date_promt = loadPrompt(query);
                chatGPT.setPrompt(date_promt);
                return;
            };

            Message msg = sendTextMessage("Подождите немного пока вам набирают текст...");
            String answer = chatGPT.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }
        // обработчик MESSAGE режимов
        if (message.equals("/message")){
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage( "Пришлите в чат вашу переписку:",
                    "Следующее сообщение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }
        if (currentMode == DialogMode.MESSAGE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")){
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);
                Message msg = sendTextMessage("Подождите немного пока *ChatGPT* думает...");
                String messageAnswer = chatGPT.sendMessage(prompt, userChatHistory);
                updateTextMessage(msg, messageAnswer);
                return;
            }

            list.add(message);
            return;
        }

        // обработчик PROFILE
        if (message.equals("/profile")){
             currentMode = DialogMode.PROFILE;
             sendPhotoMessage("profile");

             me = new UserInfo();
             questionCount = 0;
             sendTextMessage("Создание профиля Tinder. \nСообщите информацию о себе. \n" +
//                     "Представьтесь пожалуйста. Ваше имя?");
                "Вы мужчина/женщина?");
             return;
        }
        if (currentMode == DialogMode.PROFILE && !isMessageCommand()){
            switch (questionCount) {
                case 0:
                    me.sex = message;
                    questionCount = 1;
                    sendTextMessage("Как вас зовут?");
                    return;
                case 1:
                    me.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько Вам лет?");
                    return;
                case 2:
                    me.age = message;
                    questionCount = 3;
                    sendTextMessage("Кем вы работаете?");
                    return;
                case 3:
                    me.occupation = message;
                    questionCount = 4;
                    sendTextMessage("Опишите Ваше(и) хобби...");
                    return;
                case 4:
                    me.hobby = message;
                    questionCount = 5;
                    sendTextMessage("Что Вам *НЕ* нравится в людях?");
                    return;
                case 5:
                    me.annoys = message;
                    questionCount = 6;
                    sendTextMessage("Цель знакомства.");
                    return;
                case 6:
                    me.goals = message;
                    String aboutMyself = me.toString();
                    String promptProfile = loadPrompt("profile");
                    Message msg = sendTextMessage("Подождите немного пока *ChatGPT* создаёт вам профиль \uD83E\uDDE0...");
                    String answer = chatGPT.sendMessage(promptProfile, aboutMyself);
                    updateTextMessage(msg, answer);
                    return;
                default:
//                    КодВыбораПоУмолчанию;
                    break;
            }
            return;
        }

        // обработчик OPENER режимов
        if (message.equals("/opener")){
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");
//            sendTextMessage("Пришли мне информацию о человеке для знакомства.");
            she = new UserInfo();
            questionCount = 1;
            sendTextMessage("Пришли мне информацию о человеке для знакомства. \n" +
                    "Сообщите информацию о ней. \n" +
                    "Как её зовут?");
            she.sex = "Девушка";
            return;
        }
        if (currentMode == DialogMode.OPENER && !isMessageCommand()){
            switch (questionCount) {
                case 1:
                    she.name = message;
                    questionCount = 2;
                    sendTextMessage("Сколько ей лет?");
                    return;
                case 2:
                    she.age = message;
                    questionCount = 3;
                    sendTextMessage("Кем она работает?");
                    return;
                case 3:
                    she.occupation = message;
                    questionCount = 4;
                    sendTextMessage("Опишите её хобби...");
                    return;
                case 4:
                    she.hobby = message;
                    questionCount = 5;
                    sendTextMessage("Что ей *НЕ* нравится в людях?");
                    return;
                case 5:
                    she.annoys = message;
                    questionCount = 6;
                    sendTextMessage("Возможная цель её знакомства.");
                    return;
                case 6:
                    she.goals = message;
                    String aboutShe = she.toString();
                    String promptProfile = loadPrompt("opener");
                    Message msg = sendTextMessage("Подождите немного пока *ChatGPT* создаёт сообщение для человечка \uD83E\uDDE0...");
                    String answer = chatGPT.sendMessage(promptProfile, aboutShe);
                    updateTextMessage(msg, answer);
                    return;
                default:
                    break;
            }
            return;
        }
        // общая часть диалога
        sendTextMessage("*Привет!*");
        sendTextButtonsMessage("Выберите режим работы:", "Старт", "/start"
                ,"Стоп", "/bye");
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
