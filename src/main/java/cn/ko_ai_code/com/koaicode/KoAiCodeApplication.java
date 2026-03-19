package cn.ko_ai_code.com.koaicode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.ko_ai_code.com.koaicode.mapper")
public class KoAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(KoAiCodeApplication.class, args);
    }

}
