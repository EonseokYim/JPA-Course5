package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
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
        List<MemberTeamDto> results = queryFactory
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

        return new PageImpl<>(results, pageable, total);
    }
}
