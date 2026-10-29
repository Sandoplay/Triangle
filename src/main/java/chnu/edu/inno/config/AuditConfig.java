package chnu.edu.inno.config;


/*
  @author   Sandoplay
  @project   inno
  @class  AuditConfig
  @version  1.0.0
  @since 29/10/2026
*/

import com.mongodb.client.MongoClient;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.repository.mongo.MongoRepository;
import org.javers.spring.auditable.AuthorProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.Optional;

@Configuration
@EnableMongoAuditing
public class AuditConfig {

    // Spring Data auditing: @CreatedBy / @LastModifiedBy
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(System.getProperty("user.name"));
    }

    // JaVers commit author
    @Bean
    public AuthorProvider authorProvider() {
        return () -> "ADMIN";
    }

    @Bean
    public Javers javers(MongoClient mongoClient, @Value("${spring.mongodb.database}") String database) {
        return JaversBuilder.javers()
                .registerJaversRepository(new MongoRepository(mongoClient.getDatabase(database)))
                .build();
    }
}
