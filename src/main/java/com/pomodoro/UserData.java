package com.pomodoro;

import com.pomodoro.PomodoroTelegram;
import org.springframework.data.annotation.Id;

import java.time.Instant;

/**
 * Класс для хранения параметров текущего сеанса пользователя.
 */
public class UserData {
     // Идентификатор текущей (последней) сессии чата пользователя.
     @Id
     long chatId;
     // Имя пользователя.
     String userName;
     // Время завершения текущего таймера работа-отдых.
     Instant time = null;
     // Тип текущего таймера.
     TimerType timerType = TimerType.NONE;
     //количество оставшихся циклов Помидоро.
     int cycle = 0;
     // текущие значения время работы, время отдыха, число циклов и множитель времени работы.
     int cfgWork = 25;
     int cfgRelax = 5;
     int cfgCount = 3;
     int cfgMulti = 1;
     public UserData(){}
     public UserData (long chat_id, String user_name) {
          chatId = chat_id;
          userName = user_name;
     }
     public void setChatId(long chat_id){
          chatId = chat_id;
     }
     public void setUserName(String user_name) {
          if (user_name.length()>1)
               userName = user_name;
          else
               userName = "Нет имени";
     }
     @Override
     public String toString() {
          return String.format("Пользователь [id=%s, Имя='%s']", chatId, userName);
     }
     @Override
     public int hashCode() {
          return  Long.hashCode(chatId);
     }
     @Override
     public boolean equals(Object obj) {
          if (this == obj)
               return true;

          if (obj == null)
               return false;

          if (!(obj instanceof UserData))
               return false;
          if (((UserData) obj).chatId == this.chatId)
               return true;
          return false;
     }
}

