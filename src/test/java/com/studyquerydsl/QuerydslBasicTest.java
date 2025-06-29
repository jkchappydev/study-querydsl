package com.studyquerydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyquerydsl.entity.Member;
import com.studyquerydsl.entity.QMember;
import com.studyquerydsl.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import static com.studyquerydsl.entity.QMember.*;

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

}
