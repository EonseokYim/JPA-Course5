package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements  MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
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

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();
/**             fetchResults(), fetchCount()는 Deprecated되었다.
                왜냐하면 해당 메소드는 그냥 단순하게 쿼리의 row를 count하는 구조로 되어있기 때문에 복잡한 쿼리는 count를 구하는데 있어서 시간이 엄청 걸리거나
                제대로 count를 계산할 수 없는 상황이 발생하기 때문이다.
                심플한 쿼리는 상관없지만.. 모두 항상 심플하지는 않기에 deprecated 되었다.
                그래서 직접 count쿼리를 짜야한다. 아래에 searchPageComplex()를 참고
                */

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> contents = queryFactory
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        //count query를 최적화할 수 있는 방법을 모색해서 직접 카운트 쿼리를 작성한다.
        //데이터가 별로없으면 그냥 시간들이지 말고 fetchResults사용하고 몇 천만 건 있으면 count 쿼리를 최적화하면 성능 향상 도움에 크다. ex. 카운트쿼리를 먼저구한다음에 만약에 0이면 컨텐츠 쿼리를 실행하지 않는다던가..등등도 도움이 된다.
        Long total = queryFactory
                .select(member.id.count())
                .from(member)
                .leftJoin(member.team, team) //count 쿼리를 만드는데 join할 필요가 있을까? 상황에 따라서는 조인이 없어도 정확히 count를 계산할 수 있다.
                .where(usernameEq(condition.getUsername()),
                        testNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetchFirst();

        return new PageImpl<>(contents, pageable, total);
    }

    /**
     * CountQuery 최적화
     *
     *
     * 위에 searchPageComplex 메소드에서 contents 쿼리랑 count 쿼리 같이 가져온다. 매번 두개의 쿼리를 항상 실행된다. 하지만 조건에 따라서 count 쿼리가 생략가능하다. (필요없을 수도 있다는 말)
     *
     * ** count 쿼리가 생략 가능한 조건 **
     *  - 페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
     *  - 마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함, 더 정확히는 마지막 페이지 이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때)
     *
     *  스프링 데이터에서 이걸 기가막히게 제공해준다. 아래의 searchPageComplexWithPageableExecutionUtils() 메소드를 참고하자.
     */

    @Override
    public Page<MemberTeamDto> searchPageComplexWithUtils(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> contents = queryFactory // content 메소드는 동일하다.
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();


        JPAQuery<Long> countQuery = queryFactory
                .select(member.id.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        testNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );//fetch제거 (fetch를 하지 않으면 쿼리가 실행되지 않는다.) 뒤에서 따로 countQuery.fetch(); //따로 분리하여 이걸 입력해야 실행된다는 말이다.

        return PageableExecutionUtils.getPage(contents, pageable, countQuery::fetchFirst); //() -> countQuery.fetchFirst() -------> countQuery::fetchFirst
        /* PageableExecutionUtils.getPage()를 사용하여 리턴한다.
            첫번째, 두번쨰 파라미터는 동일하지만 여기서 중요한건 3번쨰 파라미터이다.
            getPage()가 count 쿼리(3번째 파라미터)를 호출하여 카운터를 가져오기도 하지만 1,2번 파라미터를 참조하여 조건에 따라서 count fetch(count query)를 실행하지 않도록 처리해준다.
         */
    }
}
