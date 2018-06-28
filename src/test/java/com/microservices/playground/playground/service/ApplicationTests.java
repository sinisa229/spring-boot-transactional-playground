package com.microservices.playground.playground.service;

import com.microservices.playground.playground.service.Application.Person;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

	@Autowired
	private Application application;

	@Before
	public void setUp() throws Exception {
		application.deleteAll();
	}

	@Test
	public void transactionalNeverServiceRollback() {
		assertThatThrownBy(() -> application.transactionalNeverServiceRollback()).hasMessage("Throwing exception");
		assertAllInDb(
				new Person(null, "transactionalNoneRollback", "transactional"),
				new Person(null, "transactionalStandardService.save", "transactional")
		);
	}

	private void assertAllInDb(final Person... people) {
		assertThat(getAllWithStrippedIds()).contains(people);
	}

	private List<Person> getAllWithStrippedIds() {
		return application.getAll().stream().map(person -> new Person(null, person.getFirstName(), person.getLastName())).collect(toList());
	}

}
