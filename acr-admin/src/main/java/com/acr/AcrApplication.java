package com.acr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI-Code-Review 应用程序启动类
 *
 * @author AI-Code-Review Team
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableAsync
public class AcrApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(AcrApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  AI-Code-Review 启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                "  __  __ ___  ____  ____   ____ _   _ _   _ \n" +
                " |  \\/  |__ \\ / ___|/ ___| / ___| | | | \\ | |\n" +
                " | |\\/| |  ) | |  _| |     | |   | |_| |  \\| |\n" +
                " | |  | | / /| |_| | |___  | |___|  _  | |\\  |\n" +
                " |_|  |_|/_/  \\____|\\____|  \\____|_| |_|_| \\_|\n" +
                "\nMiguCHN Workspace • Made with passion\n");
    }
}
