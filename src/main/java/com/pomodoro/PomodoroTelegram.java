package com.pomodoro;

import static java.lang.System.out;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;

/** Приложение помогает использовать "Технику Помидора".
 *
 *   Это техника управления временем, предложенная Франческо Чирило в конце 1980-х,
 *   предполагает увеличение эффективности работы при меньших временных затратах
 *   за счет глубокой концентрации и коротких перерывов.
 *   В классической технике, отрезки времени - "помидоры" длятся полчаса: 25 минут работа и 5 минут отдыха.
 *
 *   @author Igor
 *   @version 1.0
 *   @since 1.17
 *
 * Техническое задание:
 *   1. Обеспечить взаимодействие с пользователем через Телеграм-бота.
 *   2. Вывести Приветствие, текущие параметры Помидора.
 *   3. Предложить изменить параметры или запустить Помидоры.
 *   4. Уметь ввести с клавиатуры время работы, время отдыха и количество циклов работа-отдых ("помидоров").
 *   5. Уметь вывести подсказку по программе.
 *   6. Уметь выполнить запуск и останов Помидоров.
 *   7. Визуально отображать текущее состояние по технике - работа или отдых [, с прогрессбаром?].
 *   8. Когда все циклы-помидоры будут завершены, сообщить пользователю "Работа завершена".
 *
 *  Используются библиотеки:
 *      org.springframework.boot:spring-boot-starter-data-mongodb
 *      org.telegram:telegrambots:6.1.0
 */

public class PomodoroTelegram extends TelegramLongPollingBot {
    // Имя телеграм-бота.
    private static String bot_name = "MyPomodoro77Bot";
    @Autowired
    private UserDataRepository repository;
    // Признак ведения журнала сообщений пользователей.
    private static boolean log_printed = true;

    // нициализация приложения и чтение параметров.
    public void init() throws Exception {
        System.out.println("Выполнен запуск бота Помидоро!");
        // тестовые записи в БД.
        // repository.save(new UserData((long)101, "Igor Pets"));
        // repository.save(new UserData((long)102, "Julia Pets"));
        // repository.save(new UserData((long)103, "Татьяна Курабатова"));

        new Thread(() -> {
            try {
                run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).run();
    }

    // Тело основного потока бота Помодоро.
    public void run() throws InterruptedException {
        while (true) {
            pomodoro_loop();
            Thread.sleep(5000);
        }
    }

    /**
     *  Проверяем всех пользователей на исполнение таймера "работа-отдых".
     */
    private void pomodoro_loop() {
        // Выполняем цикл перебора всех элементов-пользователей в репозитории.
        for (UserData user_data : repository.findAll()) {
            // Проверяем всех пользователей на срабатывание таймера работы-отдыха.
            if (user_data.timerType != TimerType.NONE) {
                Instant curr_time = Instant.now();
                // Проверяем - время текущего таймера завершилось?
                if (curr_time.isAfter(user_data.time)) {
                    if (user_data.timerType == TimerType.WORK) {
                        // Отдых имеет смысл только если за ним будет еще одна работа.
                        if (user_data.cycle > 0) {
                            // Запускаем таймер отдыха.
                            user_data.time = curr_time.plus(user_data.cfgRelax, ChronoUnit.MINUTES);
                            user_data.timerType = TimerType.RELAX;
                            repository.save(user_data);
                            sendMsg(user_data.chatId, getTime() + " Время отдыха " +
                                    getMinutesString(user_data.cfgRelax) + "!");
                        } else {
                            to_stop(user_data);
                        }
                    } else {
                        // Запускаем таймер работы.
                        user_data.time = curr_time.plus(user_data.cfgWork, ChronoUnit.MINUTES);
                        user_data.timerType = TimerType.WORK;
                        user_data.cycle--;
                        repository.save(user_data);
                        sendMsgWork(user_data);
                    }
                }
            }
            //out.println("Пользователь" + user_data.userName);
        }
    }

    // Отображение прогресбара с ожиданием завершения текущей работы или отдыха.
    // String process - отображаемое название прогресбара,
    // time - время ожидания таймера в минутах,
    // size - размер прогресбара на экране в символах,
    // InterruptedException - необходимо для Thread.sleep.
    private void printProgress(String process, int time, int size) {
        // время в секундах
        int time_sec = time * 60;

        // Текущее пройденное время этапа (работа/отдых).
        float curr_tm = 0.0f;
        while (curr_tm < time_sec) {
            // Выполняем паузу в приложении на 100мс.
            // :todo убрать Thread.sleep(100);
            // увеличиваем счетчик на 100мс.
            curr_tm = curr_tm + 0.1f;
            if (curr_tm > time_sec) curr_tm = time_sec;
            // Отображаемое время этапа в формате
            float look_tm = Math.round(curr_tm * 10) / 10;
            // Вычисляем процент и сразу приводим к формату ХХ.Х%
            double percent = Math.floor(curr_tm / (float) time_sec * 1000.0f) / 10.0f;
            // Число закрашенных символово прогресбара #
            int sym = Math.round((float) size * curr_tm / (float) time_sec);
            // Пройденное время в формате ХХ.X мин.
            double time_go = Math.floor((float) time * curr_tm * 10.0f / (float) time_sec) / 10.0f;
            // System.out.println("look="+look_tm+" curr="+curr_tm + " sym="+sym); // Тестовая печать.
            // Готовим строку - заголовок прогресбара.
            String head_str = process + percent + "% " +
                    (" ").repeat(5 - (String.valueOf(percent).length()));
            // Отображаем всю строку прогрессбара поверх старой строки.
            System.out.print(head_str + "[" + ("#").repeat(sym) + ("-").repeat(size - sym) +
                    "]    ( " + time_go + "мин / " + time + "мин)" + "\r");
        }
        System.out.println();
    }

    /**
     * Return username of this bot
     */
    @Override
    public String getBotUsername()
    {
        return bot_name;
    }

    /**
     * Returns the token of the bot to be able to perform Telegram Api Requests
     *
     * @return Token of the bot
     */
    @Override
    public String getBotToken() {

        // Токен, выданный BotFather.
        return "5433494058:AAGqZTo9RbOlbwPioSlIBjXOBPXp9TLgLUg";
    }

    /**
     * Is called when bot gets registered
     */
    @Override
    public void onRegister() {
        out.println("Телеграм-бот " + bot_name + " зарегистрирован!");
        super.onRegister();
    }

    /**
     * This method is called when receiving updates via GetUpdates method
     * При каждом событии (сообщении, подключении и т.д.) в чат-боте нам придет вызов с объектом update.
     *
     * @param update Update received
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            // Выделяем объект-сообщение от пользователя.
            Message in_msg = update.getMessage();
            if (in_msg.hasText()) {
                Chat chat = in_msg.getChat();
                UserData user_data = getUserData(in_msg.getChatId(), chat);
                // Выделяем полный текст сообщения и разделяем элементы через пробел.
                String[] text = in_msg.getText().split(" ");
                // Первый элемент текста - всегда команда.
                String new_comand = text[0];
                // Если есть второй параметр, то запоминаем его отдельно.
                String new_value = text.length > 1 ? text[1] : "";
                switch (new_comand) {
                    case "/start" -> to_start(user_data);
                    case "/help" -> to_help(user_data);
                    case "/work" -> to_work(user_data, stringToInt(new_value));
                    case "/relax" -> to_relax(user_data, stringToInt(new_value));
                    case "/count" -> to_count(user_data, stringToInt(new_value));
                    case "/multi" -> to_multi(user_data, stringToInt(new_value));
                    case "/timer" -> to_timer(user_data);
                    case "/stop" -> to_stop(user_data);
                }
                log_print(text[0], user_data);
            }
        }
    }

    /**
     *  Поиск существующего или создание нового хранилища для пользовательских параметров.
     *  @param chat_id - Уникальный идентификатор чата.
     *  @param chat - Объект чата с пользователем.
     */
    private UserData getUserData(long chat_id, Chat chat) {
        UserData user_data = repository.findByChatId(chat_id);
        if (user_data == null) {
            String user_name = chat.getUserName();
            if (user_name == null) {
                String user_name1 = chat.getFirstName();
                String user_name2 = chat.getLastName();
                if (user_name1 == null && user_name2 == null) {
                    user_name = chat.getId().toString();
                } else if (user_name1 != null) {
                    user_name = user_name1;
                } else {
                    user_name = user_name2;
                }
            }
            // Создаем новый объект для хранения параметров пользователя.
            user_data = new UserData(chat_id, user_name);
            repository.save(user_data);
        }
        return user_data;
    }

    // Обработка команды "/start".
    private void to_start(UserData user_data) {
        // Приветствие при подключении новых пользователей.
        sendMsg(user_data.chatId, "Добро пожаловать в Помидоро!\n" +
                "Нажмите /help для отображения подсказки и текущих параметров");
    }

    // Обработка команды "/work".
    private void to_work(UserData user_data, int new_work_time) {
        user_data.cfgWork = new_work_time;
        repository.save(user_data);
        sendMsg(user_data.chatId, "Установлено новое время работы " + getMinutesString(new_work_time));
    }

    // Обработка команды "/work".
    private void to_relax(UserData user_data, int new_relax_time) {
        user_data.cfgRelax = new_relax_time;
        repository.save(user_data);
        sendMsg(user_data.chatId, "Установлено новое время отдыха " + getMinutesString(new_relax_time));
    }

    // Обработка команды "/multi".
    private void to_multi(UserData user_data, int new_multi) {
        user_data.cfgMulti = new_multi;
        repository.save(user_data);
        sendMsg(user_data.chatId, "Установлен новый множитель времени работы " + getMinutesString(new_multi));
    }

    // Обработка команды "/count".
    private void to_count(UserData user_data, int new_count) {
        user_data.cfgCount = new_count;
        repository.save(user_data);
        sendMsg(user_data.chatId,
            "Установлено новое количество циклов  " + new_count);
    }

    // Обработка команды "/timer".
    private void to_timer(UserData user_data) {
        user_data.time = Instant.now().plus(user_data.cfgWork, ChronoUnit.MINUTES);
        user_data.timerType = TimerType.WORK;
        user_data.cycle = user_data.cfgCount - 1;
        repository.save(user_data);
        String multi_str;
        if (user_data.cfgMulti > 1) multi_str = " множитель="+user_data.cfgMulti;
        else multi_str = "";
        sendMsg(user_data.chatId, getTime()+" Запуск таймера: работа=" + user_data.cfgWork +
                " отдых=" + user_data.cfgRelax + " циклы=" + user_data.cfgCount + multi_str);
        sendMsgWork(user_data);
    }

    private void sendMsgWork(UserData user_data) {
        sendMsg(user_data.chatId, getTime()+" Время работы " +
                getMinutesString(user_data.cfgWork) + "!");
    }

    // Обработка команды "/stop".
    private void to_stop(UserData user_data) {
        user_data.timerType = TimerType.NONE;
        user_data.cycle = 0;
        repository.save(user_data);
        sendMsg(user_data.chatId, "Работа таймера завершена!");
    }

    // Обработка команды "/help".
    private void to_help(UserData user_data) {
        String help_text = "Pomodoro - приложение для повышения личной эффективности.\n" +
                "/work <минуты> - время работы [" + user_data.cfgWork + "]\n" +
                "/relax <минуты> - время отдыха [" + user_data.cfgRelax + "]\n" +
                "/count <количество> - циклы Помидора работа-отдых [" + user_data.cfgCount + "]\n" +
                "/multi <множитель> - на каждом новом цикле время работы увеличивается [" + user_data.cfgMulti + "]\n" +
                "/timer - запустить таймер Помидоро\n" +
                "/stop - остановить таймер Помидоро\n" +
                "/help - показать подсказку.";
        // Отправляем пользователю сообщение с подсказкой .
        sendMsg(user_data.chatId, help_text);
    }

    // Журнал сообщений пользователей.
    private void log_print(String text, UserData user_data) {
        if (log_printed)
            System.out.println("Получено сообщение " + text + " от: " + user_data.userName);
    }

    // Отправка пользователю сообщения text.
    private void sendMsg(long chat_id, String text) {
        // Создаем объект-сообщение.
        SendMessage msg = new SendMessage();
        // Указываем в сообщении чат-получатель.
        msg.setChatId(chat_id);
        // Указываем текст сообщения.
        msg.setText(text);
        // Повторяем отправку до трех раз, в случае ошибки.
        for (int i = 1; i <= 3; i++)
            try {
                // Отправляем сообщение. Эта операция можеть быть выполнена с ошибкой.
                execute(msg);
                break;
            } catch (TelegramApiException ex) {
                ex.printStackTrace();
            }
    }

    // Возвращает строку вида "25 минут" с правильным окончанием.
    private String getMinutesString(int minutes) {
        // Определяем остаток от деления на 10.
        int mod_f = minutes % 10;
        if ( mod_f == 2 || mod_f == 3 || mod_f == 4) {
            return minutes + " минуты";
        } else if (mod_f == 1){
            return minutes + " минута";
        }
        // 0 5 6 7 8 9
        return minutes + " минут";
    }
    private int stringToInt(String str) {
        int res;
        try {
            // именно здесь String преобразуется в int
            res = Integer.parseInt(str.trim());
        } catch (NumberFormatException nfe) {
            res = -1;
        }
        return res;
    }

    // Возвращаем текущее время в формате "ЧЧ:ММ".
    private String getTime() {
        Calendar curr = new GregorianCalendar();
        int hour = curr.get(Calendar.HOUR_OF_DAY);
        int minute = curr.get(Calendar.MINUTE);
        String hour_str = hour < 10 ? "0"+hour: String.valueOf(hour);
        String minute_str = minute < 10 ? "0"+minute: String.valueOf(minute);
        return hour_str + ":" + minute_str;
    }
}
