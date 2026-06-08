package com.daku.diary;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DiaryApplication {

	// 서버(특히 UTC인 GCP VM)에 상관없이 항상 한국 시간(KST)으로 동작하도록 고정
	@PostConstruct
	public void setTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		SpringApplication.run(DiaryApplication.class, args);
	}

}
