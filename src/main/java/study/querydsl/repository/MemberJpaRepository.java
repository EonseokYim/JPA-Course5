package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/**
 * 섹션5 실무 활용 - 순수 JPA와 QueryDsl
 */
@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
        this.em = em;
        //this.queryFactory = new JPAQueryFactory(em);
        this.queryFactory = queryFactory; //QuerydslApplication에서 SpringBean으로 등록했기 때문에 그냥 파라미터으로 인젝션 받으면 된다.
        //이렇게 파라미터로 인젝션 받으면 생성자 패턴이 @RequiredArgsConstructor로 매칭되기 때문에 이 생성자를 생략할 수 있다. (주석 설명때메 지금은 생략안함)
    }

    //순수JPA
    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }


    /**
     * findAll() JPQL vs QueryDSL 비교
     */
    //JPA
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
    //QueryDsl
    public List<Member> findAll_QueryDsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();

    }

    /**
     * findByUsername() JPQL vs QueryDSL 비교
     */
    //JPA
    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    //QueryDsl
    public List<Member> findByUseranme_QueryDsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

    /**
     * 동적 쿼리와 성능 최적화 조회 - Builder 사용
     */
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        //특징1) Builder 조건을 사용한 동적 쿼리 최적화
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(condition.getUsername())) { //hasText() == null, ""이 아니면 true
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (StringUtils.hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }


        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"), //특징2) 조인과 DTO로 한번에 쫙 조회하는 성능 최적화
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }


}
