package com.studyquerydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyquerydsl.entity.Member;
import com.studyquerydsl.entity.QMember;
import com.studyquerydsl.entity.QTeam;
import com.studyquerydsl.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static com.studyquerydsl.entity.QMember.*;
import static com.studyquerydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
@ActiveProfiles(value = "local")
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

}