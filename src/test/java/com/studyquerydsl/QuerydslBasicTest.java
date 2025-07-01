package com.studyquerydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyquerydsl.dto.MemberDto;
import com.studyquerydsl.dto.QMemberDto;
import com.studyquerydsl.dto.UserDto;
import com.studyquerydsl.entity.Member;
import com.studyquerydsl.entity.QMember;
import com.studyquerydsl.entity.Team;
import jakarta.persistence.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static com.studyquerydsl.entity.QMember.member;
import static com.studyquerydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
@ActiveProfiles(value = "local")
// @Rollback(false)
public class QuerydslBasicTest {

    @PersistenceContext
    private EntityManager em;

    // 따로 필드 레벨로 빼도 무방
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
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

    @Test
    public void startJPQL() {
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        // 1.
        /*QMember m = new QMember("m"); // alias 변경
        Member findMember = queryFactory.select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();*/

        // 2.
        /*QMember m = QMember.member;
        Member findMember = queryFactory.select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();*/

        // 3. QMember.member 를 option + enter로 static import (권장)
        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // ==== 검색 조건 쿼리 ====
    @Test // eq(param) ===> '='
    public void searchEq() {
        Member findMember = queryFactory
                .selectFrom(member) // select(member).from(member) 하나로 합침
                .where(member.username.eq("member1") // username = 'member1'
                        .and(member.age.eq(10))
                )
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test // ne(param),  eq(param).not() ===> '!='
    public void searchNotEq() {
        Member findMember1 = queryFactory
                .selectFrom(member)
                .where(member.username.ne("member1") // username != 'member1'
                        .and(member.age.eq(10))
                )
                .fetchOne();

        Member findMember2 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").not() // username != 'member1'
                        .and(member.age.eq(10))
                )
                .fetchOne();

        Assertions.assertThat(findMember1).isNull();
        Assertions.assertThat(findMember2).isNull();
    }

    @Test // isNotNull()
    public void searchIsNotNull() {
        List<Member> findMember = queryFactory
                .selectFrom(member)
                .where(member.username.isNotNull())
                .fetch();

        Assertions.assertThat(findMember).isNotNull();
    }

    @Test // in(param1, param2, ...)
    public void searchIn() {
        List<Member> findMember = queryFactory
                .selectFrom(member)
                .where(member.age.in(10, 30)) // age in(10, 30)
                .fetch();

        Assertions.assertThat(findMember.get(0).getUsername()).isEqualTo("member1");
    }

    @Test // notIn(param1, param2, ...)
    public void searchNotIn() {
        List<Member> findMember = queryFactory
                .selectFrom(member)
                .where(member.age.notIn(10, 20, 30)) // age not in(10, 20, 30)
                .fetch();

        Assertions.assertThat(findMember.get(0).getUsername()).isEqualTo("member4");
    }

    @Test // between(param1, param2)
    public void searchBetween() {
        List<Member> findMember = queryFactory
                .selectFrom(member)
                .where(member.age.between(20, 30)) // age between 20 and 30
                .fetch();

        Assertions.assertThat(findMember.get(0).getUsername()).isEqualTo("member2");
    }

    @Test // goe(param) ===> '>='
    public void searchGreaterOrEqual() {
        List<Member> findMember = queryFactory
                .selectFrom(member)
                .where(member.age.goe(30)) // age >= 30
                .fetch();

        Assertions.assertThat(findMember.get(0).getUsername()).isEqualTo("member3");
    }

    @Test // gt(param) ===> '>'
    public void searchGreaterThan() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.age.gt(30)) // age > 30
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member4");
    }

    @Test // loe(param) ===> '<='
    public void searchLessOrEqual() {
        List<Member> findMember = queryFactory
                .selectFrom(member)
                .where(member.age.loe(30)) // age <= 30
                .fetch();

        Assertions.assertThat(findMember.get(0).getUsername()).isEqualTo("member1");
    }

    @Test // lt(param) ===> '<'
    public void searchLessThan() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.age.lt(20)) // age < 20
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test // like(param)
    public void searchLike() {
        List<Member> findMembers = queryFactory
                .selectFrom(member)
                .where(member.username.like("member%")) // username like 'member%'1 escape '!'
                .fetch();

        Assertions.assertThat(findMembers).hasSize(4);
    }

    @Test // contains()
    public void searchContains() {
        List<Member> findMembers = queryFactory
                .selectFrom(member)
                .where(member.username.contains("member")) // username like '%member%'1 escape '!'
                .fetch();

        Assertions.assertThat(findMembers).hasSize(4);
    }

    @Test // startsWith()
    public void searchStartsWith() {
        List<Member> findMembers = queryFactory
                .selectFrom(member)
                .where(member.username.startsWith("member")) // username like '%member'1 escape '!'
                .fetch();

        Assertions.assertThat(findMembers).hasSize(4);
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member) // select(member).from(member) 하나로 합침
                .where(member.username.eq("member1"),
                        member.age.eq(10) // .and(member.age.eq(10))는 ,로 연결할 수도 있다.
                )
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    // ==== 결과 조회 ====
    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();// .limit(1).fetchOne();

        // fetchResult 는 페이징 + 카운트 쿼리인데 deprecated됨. ('fetchResults()' is deprecated)
        // 따라서, 직접 페이징 조건 추가 및 따로 count 쿼리를 구성해야 한다.
        int page = 0;     // 현재 페이지 (0부터 시작)
        int size = 10;    // 페이지당 항목 수

        // 페이징 쿼리
        List<Member> content = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(page * size)
                .limit(size)
                .fetch();

        // 전체 개수 // fetchCount는 카운트 쿼리인데 deprecated됨. ('fetchCount()' is deprecated)
        // 따라서, 직접 count 쿼리를 구성해야 한다.
        // 카운트 쿼리
        Long total = queryFactory
                .select(member.count())
                .from(member)
                .fetchOne();

        // Page 객체로 wrapping
        Pageable pageable = PageRequest.of(page, size);
        Page<Member> resultPage = new PageImpl<>(content, pageable, total);

        System.out.println("전체 개수: " + resultPage.getTotalElements());
        System.out.println("첫 페이지 데이터: " + resultPage.getContent());
    }

    // ==== 정렬 ====
    @Test
    public void sort() {
        // 1. 나이 내림차순
        // 2. 회원 이름 오름차순
        // 3. 회원 이름이 null은 마지막에 출력 -> nullsLast() (반대는 nullsFirst())
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
        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }

    // ==== 페이징 ====
    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.asc())
                .offset(1) // 1개를 건너 뜀
                .limit(2) // 최대 2개 조회
                .fetch();

        // member1 = Member(id=2, username=member2, age=20)
        // member1 = Member(id=3, username=member3, age=30)
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }

        Assertions.assertThat(result).hasSize(2);
    }

    @Test
    public void paging2() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.asc())
                .offset(1) // 1개를 건너 뜀
                .limit(2) // 최대 2개 조회
                .fetch();

        Long total = queryFactory
                .select(member.count())
                .from(member)
                .fetchOne();

        Pageable pageable = PageRequest.of(0, 2);
        Page<Member> resultPage = new PageImpl<>(result, pageable, total);

        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(resultPage.getTotalElements()).isEqualTo(4);
        Assertions.assertThat(resultPage.getContent()).hasSize(2);
    }

    // ==== 집합 ====
    @Test
    public void aggregation() {
        // QueryDSL에서 .select()에 여러 필드를 넣으면, 반환 타입은 Tuple로 받아야 한다.
        // 실무에서는 DTO로 가져온다.
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sumAggregate(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sumAggregate())).isEqualTo(100);
        Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    public void groupBy() {
        // 팀의 이름과, 각 팀의 평균 연령
        List<Tuple> result = queryFactory
                .select(
                        team.name, // QTeam.team static import
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .having(member.age.avg().goe(30)) // 평균 나이 30 이상만
                .fetch();

        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(result.get(0).get(member.age.avg())).isEqualTo(35);
    }

    // ==== 조인 ====
    @Test
    public void join() {
        // teamA에 소속된 모든 회원

        List<Member> memberList = queryFactory
                .selectFrom(member)
                .join(member.team, team) // QMember.member.team, QTeam.team
                // = .innerJoin()
                // + .leftJoin()
                // + .rightJoin()
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(memberList)
                .extracting("username")
                .containsExactly("member1", "member2");

    }

    @Test
    public void thetaJoin() {
        // 연관관계 없어도 조인 가능
        // 회원의 이름이 팀 이름과 같은 회원 조회
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> memberList = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        for (Member member : memberList) {
            System.out.println("member.getUsername() = " + member.getUsername());
        }

        Assertions.assertThat(memberList)
                .extracting("username")
                .containsExactly("teamA", "teamB");

    }

    @Test
    public void joinOnFiltering() {
        // 회원과 팀을 조인하면서 팀 이름이 teamA 팀만 조인, 회원은 모두 조회
        // JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        // tuple = [Member(id=1, username=member1, age=10), Team(id=1, name=teamA)]
        // tuple = [Member(id=2, username=member2, age=20), Team(id=1, name=teamA)]
        // tuple = [Member(id=3, username=member3, age=30), null]
        // tuple = [Member(id=4, username=member4, age=40), null]
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // on 절을 활용해 조인 대상을 필터링 할 때, 외부조인이 아니라 내부조인(inner join)을 사용하면, where절에서 필터링 하는 것과 기능이 동일하다.
        List<Tuple> result2 = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
                .on(team.name.eq("teamA"))
                // .where(team.name.eq("teamA")) // 같다.
                .fetch();
    }

    @Test
    public void joinOnNoRelation() {
        // 연관관계 없는 엔티티 외부 조인
        // 회원의 이름이 팀 이름과 같은 회원 조회 (* 회원 이름과 팀 이름은 연관관계가 없음)
        // 세타 조인은 left join이 안되는데 이렇게 하면 된다.
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                // .leftJoin(member.team, team) -> 연관관계 기반 조인 (ON절 자동 생성됨: team_id = team.id)
                // 내부적으로 member.team_id = team.id로 조인됨
                // 보통은 .leftJoin(member.team, team) 으로 한다. 이렇게 하면 on절에 id가 들어간다. 그러면 조인하는 대상이 id로 매칭된다.
                // 하지만, member.team을 제거하면 더이상 id로 매칭을 안하고, on절의 member.username.eq(team.name)으로 매칭된다.
                /*
                    SELECT m.*, t.*
                    FROM member m
                    LEFT JOIN team t
                           ON m.username = t.name;*/
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        // tuple = [Member(id=1, username=member1, age=10), null]
        // tuple = [Member(id=2, username=member2, age=20), null]
        // tuple = [Member(id=3, username=member3, age=30), null]
        // tuple = [Member(id=4, username=member4, age=40), null]
        // tuple = [Member(id=5, username=teamA, age=0), Team(id=1, name=teamA)]
        // tuple = [Member(id=6, username=teamB, age=0), Team(id=2, name=teamB)]
        // tuple = [Member(id=7, username=teamC, age=0), null]
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // .join(team)
        // .on(member.username.eq(team.name)) 이면
        // tuple = [Member(id=5, username=teamA, age=0), Team(id=1, name=teamA)]
        // tuple = [Member(id=6, username=teamB, age=0), Team(id=2, name=teamB)]
    }

    // ==== 패치 조인 ====
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        // JPA가 특정 연관 객체를 '실제로 로딩했는지' 확인
        // 즉, findMember.getTeam()을 실행했을 때, Team 프록시 객체를 실제 Team 엔티티로 초기화했는지 확인
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("패치 조인 미적용").isFalse(); // false
    }

    @Test
    public void fetchJoinUse() {
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .join(member.team, team).fetchJoin() // 패치 조인 적용
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("패치 조인 적용").isTrue(); // true
    }

    // ==== 서브 쿼리 ====
    @Test
    public void subQuery() {
        // 나이가 가장 많은 회원을 조회
        QMember memberSub = new QMember("memberSub");  // 서브쿼리 밖에 있는 alias와 겹치면 안되기 때문에 생성해야 한다.

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        // 서브쿼리는 JPAExpressions.select() 사용한다. (static import)
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    @Test
    public void subQueryGoe() {
        // 나이가 평균 이상인 모든 회원 조회
        /*
            SELECT *
                FROM member
            WHERE age >= (SELECT AVG(age) FROM member)
        */
        QMember memberSub = new QMember("memberSub");  // 서브쿼리 밖에 있는 alias와 겹치면 안되기 때문에 생성해야 한다.

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        // 서브쿼리는 JPAExpressions.select() 사용한다. (static import)
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result)
                .extracting("username").hasSize(2);
    }

    @Test
    public void subQueryIn() {
        // 평균보다 큰 나이를 가진 사람만 골라서 조회
        /*
            SELECT *
                FROM member
            WHERE age IN (
                SELECT age FROM member
                WHERE age > (SELECT AVG(age) FROM member)
            )
        */
        QMember memberSub = new QMember("memberSub");
        
        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(
                                        select(memberSub.age.avg())
                                                .from(memberSub)
                                ))
                ))
                .fetch();

        Assertions.assertThat(members)
                .extracting("username")
                .containsExactly("member3", "member4");
    }

    @Test
    public void selectSubQuery() {
        // 모든 회원의 이름과 함께, 전체 회원 평균 나이
        /*
            SELECT m.username,
                (SELECT AVG(m2.age) FROM Member m2)
            FROM Member m
        */
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory.select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        Assertions.assertThat(result).hasSize(4);
    }

    // ==== CASE 문 ====
    @Test
    public void simpleCase() { // 단순한 조건
        List<String> result = queryFactory
                .select(member.age.when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() { // 복잡한 조건: CaseBuilder 사용
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void orderByCase() {
        // 1. 0 ~ 30살이 아닌 회원을 먼저 출력
        // 2. 0 ~ 20살인 회원 출력
        // 3. 20 ~ 30살인 회원 출력
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    // ==== 상수, 문자 더하기 ====
    @Test
    public void constant() { // 상수
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A")) // 상수 A 출력 (JPQL에서는 미출력)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() { // 문자 더하기 (문자 + 문자는 .concat(xxx).concat(xxx) / 문자 + 숫자는 .concat(xxx).concat(xxx.stringValue()))
        // {username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    // ==== 프로젝션과 결과 반환 - 기본 ====
    @Test
    public void simpleProjection() {
        List<Member> result = queryFactory // 객체 타입 또한 프로젝션 대상이 하나다.
                .select(member)
                .from(member)
                .fetch();

        for (Member member : result) {
            System.out.println("member = " + member);
        }
    }

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

    // ==== 프로젝션과 결과 반환 - DTO 조회 ====
    @Test
    public void findDtoByJPQL() { // 순수 JPA 방식 (new operation 사용)
        List<MemberDto> result = em.createQuery("select new com.studyquerydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 1. 프로퍼티 접근 (getter, setter 필요하다)
    // 2. 필드 (getter, setter 불필요)
    // 3. 생성자 접근
    @Test
    public void findDtoByQuerydslSetter() { // 1. 프로퍼티 접근 (setter)
        // select()에 Projections.bean(반환할 클래스 타입, 반환할 필드1, 반환할 필드2, ...)
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, // 기본 생성자 필요하다.
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByQuerydslField() { // 2. 필드
        // select()에 Projections.fields(반환할 클래스 타입, 반환할 필드1, 반환할 필드2, ...)
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username, // 필드명이 정확히 일치해야 한다. (다를 경우 .as() 로 맞춰야 한다.)
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByQuerydslConstructor() { // 3. 생성자 접근
        // select()에 Projections.constructor(반환할 클래스 타입, 반환할 필드1, 반환할 필드2, ...)
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username, // MemberDto와 Member 엔티티의 필드 타입들이 정확히 일치해야 한다. 해당 필드를 파라미터로 받는 생성자가 있어야 한다.
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByQuerydslField() {

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), // UserDto는 username이 아닌 name, 따라서 as("name")으로 맞춰야한다.
                        // ExpressionUtils.as(member.username, "name"), // ExpressionUtils.as(source, alias) 가능. 이때 별칭을 UserDto의 필드명과 일치하게 작성해야 한다.
                        member.age))
                .from(member)
                .fetch();

        // as("name") 사용 안했을 때는 매칭되지 않아서 실행은 되지만 null로 나온다.
        // userDto = UserDto(name=null, age=10)
        // userDto = UserDto(name=null, age=20)
        // userDto = UserDto(name=null, age=30)
        // userDto = UserDto(name=null, age=40)
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findUserDtoByQuerydslField2() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(select(memberSub.age.max()) // 조회할때 나이를 그냥 최대값으로
                                        .from(memberSub), "age") // select()절의 서브쿼리에 별칭을 붙힐 때 반드시 ExpressionUtils.as(source, alias) 사용. (.as()사용 불가능) 이때, 별칭을 UserDto의 필드명과 일치해야한다.
                ))
                .from(member)
                .fetch();

        // userDto = UserDto(name=member1, age=40)
        // userDto = UserDto(name=member2, age=40)
        // userDto = UserDto(name=member3, age=40)
        // userDto = UserDto(name=member4, age=40)
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    // ==== 프로젝션과 결과 반환 - @QueryProjection ====
    // MemberDto 특정 파라미터를 갖는 생성자를 명시적으로 만들고 그 위에 @QueryProjection을 붙이면, QueryDSL이 해당 생성자를 기반으로 Q타입을 생성
    @Test
    public void findDtoByQuerydslQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // ==== 동적 쿼리 - BooleanBuilder ====
    @Test
    public void dynamicQueryBooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        // BooleanBuilder builder = new BooleanBuilder();

        // member.username 은 필수값
        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
        /* 필수값으로 설정했는데 조건식 넣으면 m1_0.username=? 쿼리 두번 나감
            if(usernameCond != null) {
                builder.and(member.username.eq(usernameCond));
            }
        */

        if(ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .select(member)
                .from(member)
                .where(builder)
                // .where(builder.and(xxx).or(xxx)) // builder도 .and(), .or() 가능하다
                .fetch();
    }

    // ==== 동적 쿼리 - Where 다중 파라미터 사용 ====
    @Test
    public void dynamicQueryWhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .select(member)
                .from(member)
                // .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    // 동적 조건을 메서드로 분리해두면 다양한 쿼리에서 재활용 가능
    private List<MemberDto> searchMember3(String usernameCond, Integer ageCond) {
        return queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                // .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    // 두 가지 모두 포함하는 동적조건을 생성할 수도 있다.
    // 이때, Predicate 반환 타입이 아닌, BooleanExpression을 반환 타입으로 해야한다.
    // Predicate와 BooleanExpression
    // BooleanExpression은 Predicate의 구현체 클래스
    // BooleanExpression은 .and(), .or()로 추가 조립이 가능하다. (Predicate는 불가능)
    // 따라서, 단순 조건이면 Predicate 반환 타입을 사용해도 무방하나, 조건을 조합해야 하는 경우는 BooleanExpression을 반환 타입으로 해야한다.
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        // return usernameEq(usernameCond).and(ageEq(ageCond)); // null 처리를 따로 해야한다. (usernameCond, ageCond 둘 다 없으면 NPE 발생)

        BooleanExpression username = usernameEq(usernameCond);
        BooleanExpression age = ageEq(ageCond);

        if (username != null && age != null) {
            return username.and(age);
        } else if (username != null) {
            return username;
        } else if (age != null) {
            return age;
        } else {
            return null; // 아무 조건도 없으면 null 반환 → where 절에서 무시됨
        }
    }
    
    // ==== 수정, 삭제 배치 쿼리 ====
    @Test
    // @Commit
    public void bulkUpdate() {
        // 나이가 28살 미만인 회원의 이름을 "비회원"으로 변경
        // bulk 연산은 영속성 컨텍스트의 상태를 무시하고 바로 DB로 보낸다.
        // 이러면 DB 상태와 영속성 컨텍스트의 상태가 달라져버리는 문제 발생.
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // bulk 연산 이후에는 무조건 영속성 컨텍스트 초기화
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    // @Commit
    public void bulkAdd() { // 더하기
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) // 뺄거면 add() 에 -1
                .execute();
    }

    @Test
    // @Commit
    public void bulkMultiply() { // 곱하기
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    public void bulkDelete() {
        // 나이가 18살 초과인 회원을 삭제
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    // ==== SQL function 호출하기 ====
    @Test
    public void sqlFunctionReplace() {
        /*
            select replace(member0_.username, 'member', 'M')
            from member member0_;
        */
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})", member.username, "member", "M"
                        )
                ).from(member)
                .fetch();

        // s = M1
        // s = M2
        // s = M3
        // s = M4
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunctionLower() {
        // username이 전부 소문자인 회원 조회
        /*
            select m.username
            from member m
            where m.username = lower(m.username);
        */
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        // Expressions.stringTemplate("function('lower', {0})", member.username)
                        // ANSI 표준 함수는 아래와 같이 사용 가능
                        member.username.lower()
                ))
                .fetch();

        // s = member1
        // s = member2
        // s = member3
        // s = member4
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

}