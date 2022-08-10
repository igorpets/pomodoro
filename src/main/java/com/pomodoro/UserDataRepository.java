package com.pomodoro;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface UserDataRepository extends MongoRepository<UserData, Long> {
    public UserData findByChatId(long chatId);

    public List<UserData> findByUserName(String userName);
}
