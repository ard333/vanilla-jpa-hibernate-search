package app.main;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import app.main.model.Foo;
import app.main.model.FooRepository;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

@SpringBootApplication(proxyBeanMethods = false, exclude = SpringDataWebAutoConfiguration.class)
public class SampleApplication {

	private FooRepository entities;

	@PersistenceContext
	private EntityManager entityManager;

	public SampleApplication(FooRepository entities) {
		this.entities = entities;
	}

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			Optional<Foo> foo = entities.findById(1L);
			if (!foo.isPresent()) {
				entities.save(new Foo("Hello"));
			}
		};
	}

	@Bean
	@SuppressWarnings(value = {"unchecked"})
	public RouterFunction<?> userEndpoints() {
		return route(GET("/"), request -> {
			FullTextEntityManager fullTextEM = Search.getFullTextEntityManager(entityManager);
			Query q = fullTextEM.getSearchFactory()
								.buildQueryBuilder()
								.forEntity(Foo.class)
								.get()
								.keyword()
								.onField("value")
								.matching("Hello")
								.createQuery();

			FullTextQuery fullTextQ = fullTextEM.createFullTextQuery(q, Foo.class);
			fullTextQ.setFirstResult(0);
			fullTextQ.setMaxResults(1);
			List<Foo> result = fullTextQ.getResultList();
			return ok().body(result.size() == 1 ? result.get(0) : new Foo());
		});
	}

	@SuppressWarnings(value = {"unused"})
	private Foo findOne() {
		return entities.findById(1L).get();
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleApplication.class, args);
	}

}
