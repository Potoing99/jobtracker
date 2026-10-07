package com.capstone.jobtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // ★ 추가

@SpringBootApplication
@EnableJpaAuditing // ★ Auditing 기능 활성화 (created_at 자동 세팅)
public class JobtrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobtrackerApplication.class, args);
	}

}
