package com.pomodoro;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

// Точка входа в приложение.
public class PomodoroSpring {
    public static void main(String[] args) {
        new SpringApplicationBuilder(PomodoroConfiguration.class)
                .web(WebApplicationType.NONE) // .REACTIVE, .SERVLET
                .run(args);    
    }
}
