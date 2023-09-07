package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom { //인터페이스는 여러개 상속 받을 수 있다.

    List<Member> findByUsername(String username);

}
