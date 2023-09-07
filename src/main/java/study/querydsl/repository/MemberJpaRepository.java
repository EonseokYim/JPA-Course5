package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
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

    /**
     * 동적 쿼리와 서능 최적화 조회 - Where절 파라미터 사용
     */
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        /**
         * 감탄이 나올 수 밖에 없다.
         * 이리 깔끔한지.. where문만 보면 이게 각각 동적쿼리가 어느 조건인지 한 눈에 파악이 가능하다. SQL과 거의 비슷한 느낌으로 어떤 쿼리가 나갈지 머리속에 한방에 그려진다.
         * 김영한은 Builder 보다 이와 같이 Where절 파라미터를 기본으로 사용하라고 권장함. 물론 builder를 쓸 때도 있긴하다.
         */
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                       testNameEq(condition.getTeamName()),
                       ageGoe(condition.getAgeGoe()),
                       ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }
    //Predicate 리턴보다 BooleanExpression로 리턴하는게 더 낫다. BooleanExpression는 compsite가 가능하기 때문.
    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression testNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    /**
     * Where절 파라미터의 정말 큰 장점은 다음과 같다.
     * 위에짠 코드를 재사용할 수 있다.
     * List<MemberTeamDto>로 리턴했지만 예시로 List<Member> 엔터티로 리턴해야하는 요구사항이 있다고 가정한다면.. 아래와 같이 그냥 Where문은 고대로 코드 재사용하면 된다.
     */
    public List<Member> searchMember(MemberSearchCondition condition) {
        return queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        testNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }
    /**
     * 그리고 null만 좀 조심한다면 위의 condition들을 조합하여 사용할 수 있다.
     */
    private BooleanExpression ageBetween(int ageLoe, int ageGoe) {
        //null check 구현됐다고 가정
        return ageLoe(ageLoe).and(ageGoe(ageGoe)); //이런식으로 condition을 조합하여 또다른 의미있는 condition을 만들 수 있다.
    }


}
