package com.pomodoro;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Стартовый класс Телеграм-бота "Помидоро".
 * Обеспечивает взаимодействие с Telegram API,
 * начальную инициализацию и регистрацию бота.
 * t.me/MyPomodoro77Bot
 * Описание Bot API: https://core.telegram.org/bots/api
 * Token: 5433494058:AAGqZTo9RbOlbwPioSlIBjXOBPXp9TLgLUg
 *
 * @author Igor
 */

@SpringBootApplication(scanBasePackages={"com.pomodoro"})
@Configuration
public class PomodoroConfiguration {

    @Bean
    public PomodoroTelegram pomodoroBot(){
        return new PomodoroTelegram();
    }

    @Bean
    public TelegramBotsApi botsApi(PomodoroTelegram pomodoroBot) throws TelegramApiException, Exception {
        // Объект для взаимодействия с Telegram API.
        var botsApi = new TelegramBotsApi(DefaultBotSession.class);
        // Создаем единственный экземпляр основного класса.
        //var pomodoroBot = new PomodoroTelegram();
        // Регистрируем нового бота в Телеграм.
        botsApi.registerBot(pomodoroBot);
        // Инициализируем объект - внутри бесконечный цикл.
        pomodoroBot.init();
        return botsApi;
    }
}
