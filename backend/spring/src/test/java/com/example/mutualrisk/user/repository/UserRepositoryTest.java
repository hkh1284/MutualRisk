package com.example.mutualrisk.user.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.mutualrisk.common.config.QuerydslConfig;
import com.example.mutualrisk.user.entity.User;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@ActiveProfiles("test")
@DataJpaTest
@Import({QuerydslConfig.class})
@Slf4j
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private EntityManager em;

	@BeforeEach
	void setUp(){
	}

	@DisplayName("사용자의 Oauth id를 사용하여 사용자를 찾을 수 있다")
	@Test
	void findByOauthId(){

		//given
		List<User> users = new ArrayList<>();

		User user1 = User.builder()
			.email("kim@gmail.com")
			.oauthId("12345-KAKAO")
			.nickname("김영표")
			.image("testimage")
			.build();

		User user2 = User.builder()
			.email("jo@gmail.com")
			.oauthId("54321-KAKAO")
			.nickname("조용")
			.image("testimage")
			.build();


		users.add(user1);
		users.add(user2);
		userRepository.saveAll(users);

		//when
		User resultUser = userRepository.findByOauthId("12345-KAKAO")
			.orElseThrow();

		//then
		Assertions.assertThat(resultUser.getEmail())
			.isEqualTo("kim@gmail.com");

		Assertions.assertThat(resultUser.getNickname())
			.isEqualTo("김영표");

	}

	@Test
	@DisplayName("유저 id 테스트")
	void simpleText(){

		User user1 = User.builder()
			.email("kim@gmail.com")
			.oauthId("12345-KAKAO")
			.nickname("김영표")
			.image("testimage")
			.build();

		userRepository.save(user1);

	}
}