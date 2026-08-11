package com.belongus.config;

import com.belongus.domain.CoupleSpace;
import com.belongus.domain.Letter;
import com.belongus.domain.LetterReply;
import com.belongus.domain.Memory;
import com.belongus.repository.CoupleSpaceRepository;
import com.belongus.repository.LetterRepository;
import com.belongus.repository.MemoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

@Configuration
@Profile("demo")
public class DemoDataConfig {
    @Bean
    CommandLineRunner loadDemoData(
            CoupleSpaceRepository spaceRepository,
            MemoryRepository memoryRepository,
            LetterRepository letterRepository
    ) {
        return args -> {
            if (spaceRepository.count() > 0) {
                return;
            }

            CoupleSpace space = spaceRepository.save(new CoupleSpace(
                    "小满和阿然的日子",
                    "小满",
                    LocalDate.now().minusDays(428),
                    "DEMO2026"
            ));
            memoryRepository.save(new Memory(
                    space,
                    "那天傍晚的冰淇淋",
                    "风有点凉，但你把最后一口草莓味留给了我。后来想起那一天，还是会觉得心里很软。",
                    "https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=1200&q=85",
                    "小满",
                    LocalDate.now().minusDays(8)
            ));
            memoryRepository.save(new Memory(
                    space,
                    "我们绕远路回家",
                    "明明可以坐一站地铁，却因为聊天太开心，慢慢走过了三条街。",
                    "https://images.unsplash.com/photo-1511988617509-a57c8a288659?auto=format&fit=crop&w=1200&q=85",
                    "阿然",
                    LocalDate.now().minusDays(23)
            ));

            Letter letter = new Letter(
                    space,
                    "小满",
                    "阿然",
                    "有些话当面说总会害羞。谢谢你总是认真听我那些乱七八糟的小情绪，也谢谢你一直在。"
            );
            letter.addReply(new LetterReply(letter, "阿然", "我也一直都在。以后害羞的话，就慢慢写给我看。"));
            letterRepository.save(letter);
        };
    }
}
