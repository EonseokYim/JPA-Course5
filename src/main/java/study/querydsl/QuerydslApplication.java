package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QuerydslApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuerydslApplication.class, args);
    }


    /* 레포지토리의 생성자에서
       this.queryFactory = new JPAQueryFactory(em);
       선언하여 사용해도 되고
       아래와 같이 Bean으로 등록하고 사용해도 된다.
    * */
    @Bean
    JPAQueryFactory jpaQueryFactory(EntityManager em) { //그냥 스프링빈으로 등록하는구나!!
        return new JPAQueryFactory(em);
    }
}
