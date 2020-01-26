package com.microservices.playground.playground.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

@RestController
@EnableJpaRepositories(considerNestedRepositories = true)
@SpringBootApplication
@EnableSwagger2
public class Application {

	@Autowired
	private PersonRepository personRepository;
	@Autowired
	private TransactionalNeverService transactionalNeverService;
	@Autowired
	private TransactionalStandardService transactionalStandardService;
	@Autowired
    private TransactionalStandardWithNestedTransactionalNeverService transactionalStandardWithNestedTransactionalNeverService;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

    @DeleteMapping("deleteAll")
    public void deleteAll() {
        personRepository.deleteAll();
	    return;
    }

	@GetMapping("getAll")
	public List<Person> getAll() {
		final List<Person> people = new ArrayList<>();
		personRepository.findAll().forEach(people::add);
		return people;
	}

	@GetMapping("person")
	public Person getPerson(Long id) {
		return personRepository.findById(id).orElseGet(Person::new);
	}

	@PostMapping("person")
	public Person savePerson(Person person) {
		return personRepository.save(person);
	}

	@PostMapping("transactionalNeverService")
	public Person transactionalNeverService() {
		return transactionalNeverService.save(new Person(null, "transactionalNone", "transactional"));
	}

	@PostMapping("transactionalNeverServiceRollback")
	public Person transactionalNeverServiceRollback() {
		return transactionalNeverService.saveRollback(new Person(null, "transactionalNoneRollback", "transactional"));
	}

	@PostMapping("transactionalRollback")
	public Person transactionalRollback() {
		return transactionalStandardService.transactionalRollback(new Person(null, "transactionalNoneRollback", "transactional"));
	}

    @PostMapping("transactionalStandardWithNestedTransactionalNever")
    public Person transactionalStandardWithNestedTransactionalNever() {
        return transactionalStandardWithNestedTransactionalNeverService.save(new Person(null, "transactionalStandardWithNestedTransactionalNever", "transactional"));
    }

	@Component
	@Transactional(propagation = Propagation.NEVER)
	public static class TransactionalNeverService {

		@Autowired
		private PersonRepository personRepository;
		@Autowired
		private TransactionalStandardService transactionalStandardService;

		public Person save(Person person) {
			return personRepository.save(person);
		}

		public Person saveRollback(final Person person) {
			final Person savedPerson = personRepository.save(person);
			transactionalStandardService.save(new Person(null, "transactionalStandardService.save", "transactional"));
			transactionalStandardService.transactionalRollback(new Person(null, "transactionalStandardService.throwEx", "transactional"));
			return savedPerson;
		}
	}

	@Component
	@Transactional
	public static class TransactionalStandardService {

		@Autowired
		private PersonRepository personRepository;

		public Person save(Person person) {
			return personRepository.save(person);
		}

		public Person transactionalRollback(final Person person) {
			personRepository.save(new Person(null, "transactionalRollback", "transactional"));
			throw new RuntimeException("Throwing exception");
		}
	}

    @Component
    @Transactional
    public static class TransactionalStandardWithNestedTransactionalNeverService {

        @Autowired
        private PersonRepository personRepository;
        @Autowired
        private TransactionalNeverService transactionalNeverService;

        public Person save(Person person) {
            transactionalNeverService.save(person);
            return personRepository.save(new Person(null, "transactionalStandardWithNestedTransactionalNeverService.save", "transactional"));
        }

    }

	interface PersonRepository extends CrudRepository<Person, Long> {

	}

	@Entity
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class Person {
		@Id
		@GeneratedValue
		private Long id;
		private String firstName;
		private String lastName;
	}

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2)
				.groupName("Service")
				.select()
				//.paths(input -> input.contains("transactional"))
				.apis(RequestHandlerSelectors.any())
				.paths(PathSelectors.any())
				.build().apiInfo(new ApiInfo("Transactional playground", "Transactional playground", "", "", null, "", "", new ArrayList<>()));
	}

}
