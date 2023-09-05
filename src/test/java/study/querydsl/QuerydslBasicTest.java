package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import static com.querydsl.jpa.JPAExpressions.select;


@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() { //각각 테스트 케이스 실행전에 실행하는 메소드

        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();
    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 시작 - JPQL vs QueryDSL
     */

    //member1 조회 - JPQL
    @Test
    public void startJPQL() {
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl1() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember member = new QMember("m");

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 기본 Q-Type 활용
     */

    //member1 조회 - QueryDSL (코드 간결화 후)
    @Test
    public void startQuerydsl2() {
        //JPAQueryFactory를 클래스의 variable로 선언하여 요약할 수 있다.
        //QMember 객체 내부에 member라는 스테틱 메소드를 제공해준다. 이를 static import하여 코드를 간결화할 수 있다.
        //아래와 같이 바로 queryFactory메소드만 사용하여 코드가 깔끔해진다.

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 검색 조건 쿼리
     */

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member) //select member from member
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member) //select member from member
                .where(
                        member.username.eq("member1"), //where()절 안에 파리미터로 줄줄이 쓰면 모두 AND로 조합된다.
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 결과 조회
     */

    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> fetchResults = queryFactory
                .selectFrom(member)
                .fetchResults(); //Deprecated --> 이유: fetchResults() 내부에서 count용 쿼리를 만들어실행을 하는데.. 단순한 select쿼리에 대해서는 count가 잘 잘동하지만.. 복잡한 쿼리의 count는 제대로 작동하지 않아서.. Depreacted 시킴.

        long total = fetchResults.getTotal();
        List<Member> content = fetchResults.getResults();
    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 정렬
     */

    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 페이징
     */

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getUsername()).isEqualTo("member4");
        assertThat(result.get(1).getUsername()).isEqualTo("member3");
    }

    @Test
    void paging2() {
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(0);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 집합
     */

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                ) //select 절에 Qmember가 아닌 직접 입력하면 querydsl의 튜플 타입으로 반환한다.
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100); //10+20+30+40
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }


    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void groupBy() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) //member에 있는 team과 team을 join
                .groupBy(team.name) //팀의 이름으로 그룹핑
                //.having() //groupBy 조건으로 having도 SQL, JPQL과 동일하게 사용할 수 있다.
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10+20)/2

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30+40)/2
    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 기본 조인
     */

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //leftJoin, rightJoin 다 가능함.
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(1).getUsername()).isEqualTo("member2");

        //위에 assertThat에서 get() index를 쓰는거보다 아래와 같이 사용하는 게 더 좋음.
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 조인 - On절
     */

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")) //member.team과 team을 조인하는데 team이름이 "teamA"인 것만 가져와 조인. //leftjoin에서만 on절을 사용하고. 그냥 join일 경우에는 on에 작성하나 where에 작성하나 똑같기 때문에 where에 쓰는게 보기 좋음.
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


    /**
     * 연관관계가 없는 엔터티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name)) //ON절은 조인한 대상을 줄이는 필터링 역할.
                //leftjoin을 join으로 바꾸면?
                .fetch();

        for (Tuple tuple : result) {
            //soutv
            System.out.println("tuple = " + tuple);
            
        }
    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 페치 조인
     */

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinFalse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//로딩이된 entity 인지 아닌지 확인해준다.
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinTrue() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() //조인 그대로 넣고 뒤에 fetchJoin을 적용하면된다.
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//로딩이된 entity 인지 아닌지 확인해준다.
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 서브 쿼리
     */

    /**
     * where절에 서브쿼리 사용
     */
    //where절에 서브쿼리 사용
    @Test
    public void subQuery() {

        //서브쿼리에서 사용하는 QMember와 메인쿼리에서 사용하는 QMember는 중복으로 사용하면 안되기 때문에 서브쿼리용 QMember를 아래와 같이 선언한다.
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

    }

    /**
     * where절에 서브쿼리 사용
     */
    //나이가 평균 이상인 회원
    @Test
    public void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe( //goe
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    //where절 in으로 서브쿼리 사용
    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in( //in절 사용
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * select절에 서브쿼리 사용
     */
    //멤버이름과 멤버의 평균나이 조회
    @Test
    public void selectSubquery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 서브쿼리 JPAExpressions Static Import 활용
     * (import static com.querydsl.jpa.JPAExpressions.select;)
     */
    @Test
    public void selectSubqueryWithStaticImport() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                                //staic import로 JPAExpressions. 생략가능
                                select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * JPQL은 FROM 절에 서브쿼리를 할 수가 없다. 즉, QueryDSL도 할 수가 없다.
     *
     * from 절의 서브쿼리 한계
     * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다. 당연히 Querydsl 도 지원하지 않는다.
     * 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다.
     * Querydsl도 하 이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.
     *
     * from 절의 서브쿼리 해결방안
     * 1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     * 3. nativeSQL을 사용한다. (성능에 막대한 영향이 있어!! 죽어도 FROM절에 서브쿼리를 써야해!!)
     */

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) Case문
     */

    @Test
    public void basicCase() {
        List<String> result = queryFactory.select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s :result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("20~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s: result) {
            System.out.println("s = " + s);
        }
    }

    //orderBy에서 Case문 함께 사용하기 예제
    @Test
    public void complexCaseWithOrderBy() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for(Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);
            System.out.println("username = " + username + " age = " + age + " rank = " + rank);
        }
    }
    //Querydsl은 자바 코드로 작성하기 때문에 rankPath처럼 복잡한 조건을 변수로 선언해서 select절, orderBy절에서 함께 사용할 수 있다.


    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션3 기본문법) 상수, 문자 더하기
     */

    /**
    상수 넣기
     */
    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A")) //상수 A를 무조건 쿼리 결과에 넣음.
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 문자열 합치기 & 숫자타입 문자로 변환
     */
    @Test
    public void concat() {
        //"username" + "_" + toString(age)
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        //iter
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    /**
    참고: member.age.stringValue() 부분이 중요한데, 문자가 아닌 다른 타입들은 stringValue() 로
         문자로 변환할 수 있다. 이 방법은 ENUM을 처리할 때도 자주 사용한다.
     */


    /**
     * 중급문법
     */

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션4 중급문법) 프로젝션과 결과 반환 - 기본
     */

    /**
     * 프로젝션 타입이 하나일 경우
     * 프로젝션이란? -> 원하는 데이터를 딱 SELECT 찍어서 가져오는 것
     */
    @Test
    public void simpleProjection() {
        List<String> result = queryFactory //프로젝션 타입이 하나이면 굉장히 심플하게 꺼내서 사용할 수 있다.
                .select(member.username)
                .from(member)
                .fetch();

        System.out.println("result = " + result);
    }

    /**
     * 프로젝션 타입이 여러개일 경우
     */
    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }
    /**
     * Tuple은 QueryDSL 라이브러리 안에 속해있는 타입이다. 웬만하면 이러한 튜플타입은 repository 레이어에서만 쓰고, 서비스나 컨트롤러 영역에서 데이터가 필요하다면 dto로 반환해서 사용하길 권장한다.
     * 그래야 나중에 QueryDSL이 아닌 새로운 기술이 나오더라도 controller, service 계층은 그대로 두고 repository만 수정하면 된다. 스프링이  보통 이런식으로 설계하라고 유도함.
     */

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션4 중급문법) 프로젝션과 결과 반환 - DTO 조회
     */

    /**
     * 프로젝션 결과 반환 - DTO 조회 By 순수JPA
     */
    @Test
    public void findDtoByJPQL() {
        //new operation을 활용한 DTO 프로젝션
        //이전에 배웠던 JPQL로 dto로 조회한다면 아래와 같이 MemberDTO()의 생성자를 통해서 결과를 프로젝션해줌.
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 순수JPA의 new operation을 활용한 DTO 프로젝션의 단점
     *
     * 순수 JPA에서 DTO를 조회할 때는 new 명령어를 사용해야함
     * DTO의 package 이름을 다 적어줘야해서 지저분함
     * 생성자 방식만 지원함
     */


    /**
     * 그런데..!! QueryDSL은 이 방법을 다 극복하고 정말 깔끔한 방법을 제공함 !!!
     *
     * 결과를 DTO 반환할 때 사용
     * 다음 3가지 방법 지원
     * 1. 프로퍼티 접근
     * 2. 필드 직접 접근
     * 3. 생성자 사용
     */

    // 1. 프로퍼티 접근
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, //Projections.bean는 dto의 Setter를 통하여 데이터를 dto로 꽂힌다.
                        member.username,
                        member.age)) //MemberDTO <---- member.username, member.age 프로젝션
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 2. 필드 직접 접근
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, //Projections.fields는 dto에 getter, setter 없어도 된다 바로 필드에다가 데이터를 꽂아버린다. (private인데 어째 바로 꽂히나요? 라이브러리가 다 하도록 지원해준다)
                        member.username,
                        member.age)) //MemberDTO <---- member.username, member.age 프로젝션
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 3. 생성자 사용
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, //Projections.constructor는 dto에 생성자를 통하여 데이터를 꽂아버린다. dto 클래스 안에 정의된 생성자와 두번째, 세번째 파라미터와 순서가 맞아떨어져야한다. (순서와 타입만 맞으면됨)
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    //필드 직접 접근 & 필드명이 매칭되지 않는 DTO
    @Test
    public void findUserDtoByField() {
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        //member.username, //member 엔터티의 컬럼명과 UserDto의 필드명이 달라서 그냥 null로 입력된다.
                        member.username.as("name"), //.as()를 추가하여 dto의 필드명과 맵핑하면 된다.
                        member.age))
                .from(member)
                .fetch();

        for (UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByFieldWithSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        //member.age
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age") // dto의 ago 필드값을 서브쿼리를 사용하여 모두 40살로 출력한다.

                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    // 3. 생성자 사용
    @Test
    public void findUserDtoByConstructor() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class, //Projections.constructor는 dto에 생성자를 통하여 데이터를 꽂아버린다. dto 클래스 안에 정의된 생성자와 두번째, 세번째 파라미터와 순서가 맞아떨어져야한다. (순서와 타입만 맞으면됨)
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }


    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션4 중급문법) 프로젝션과 결과 반환 - @QueryProjection
     */

    /**
     * 생성자를 활용하는 방식에서는 추가로 @QueryProjection 기능으로 구현할 수 있다.
     * @QueryProjection (MemberDTO 생성자에 @QueryProjection 어노테이션을 추가하면 QMemberDTO Q파일을 gradle이 빌드하여 사용할 수 있다.)
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }
    // 질문: @QueryProjection이랑 그냥 Projections.constructor를 사용하는 거랑 차이가 뭐냐?? 그냥 Projections.constructor써도 되지 않냐??
    // 답: Projections.constructor는 런타임에서 에러가 난다. 하지만 똑같은 오류를 @QueryProjection로 코딩하면 컴파일 오류가 난다. 즉, 굉장히 잘 설계가 되었다는 것이다. 오류날 확률이 줄어드는 거다. 런타임 오류는 무조건 피해야한다. 배포 후에 에러나는 실행 후에 에러가 확인된다는 것이니까!

    /**
     *     @QueryProjection이 장점이 많지만... DTO가 QueryDSL에 의존하게되는 단점이 있다.
     *     아키텍처 적으로 좀 문제가 있는데.. 이 DTO가 QueryDSL에 종속되어버린다는 것이다. 아키텍쳐의 설계에 따라 다르겠지만... dto는 controller, service, repository 여러 곳에서 함께 쓰이는데.. querydsl에 종속된다는 것이 dto가 순수하지가 않다.
     *
     *     1. 만약에 우리 아키가 어차피 우리는 QueryDSL에 어플리케이션이 많이 의존하고있는데, DTO도 의존되어도 괜찮다. 이거는 OK하면 DTO에 @QueryProjection를 쓰는거고..
     *     2. 아냐.. 그래도 혹시나 QueryDSL이 나중에 어쨰 바껴질지도 모르고... 나중에 미래를 위해서 우리는 DTO에 QueryDSL이 의존하는게 좀 찝찝하다하면 @QueryProjection를 사용하지말고 Projections.constructor, Projections.fields, Projections.bean등을 사용하자.
     */

    /*
    TODO
     ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
     섹션4 중급문법) 동적쿼리 - BooleanBuilder 사용
     */

    /**
     * 동적 쿼리를 해결하는 두가지 방식
     *   1. BooleanBuilder
     *   2. Where 다중 파라미터 사용
     */

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        //BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond)); //usernameCond값이 무조건이 있다고.. 앞에 null이 넘어오지 못하게 방어코드가 있다고 치면 처음부터 이렇게 파라미터로 셋팅하면 된다.
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
            //builder.or //조건에 맞게 or로도 처리할 수 있음.
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }


}
