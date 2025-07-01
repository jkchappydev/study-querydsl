package com.studyquerydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyquerydsl.dto.MemberSearchCondition;
import com.studyquerydsl.dto.MemberTeamDto;
import com.studyquerydsl.dto.QMemberTeamDto;
import com.studyquerydsl.entity.Member;
import com.studyquerydsl.entity.QTeam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.studyquerydsl.entity.QMember.member;
import static com.studyquerydsl.entity.QTeam.*;
import static org.springframework.util.StringUtils.*;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    @PersistenceContext
    private EntityManager em;
    private final JPAQueryFactory queryFactory;

    // JPAQueryFactory를 Spring Bean으로 등록 후, 바로 injection 받아도 된다.
    // 또한, @RequiredArgsConstructor 사용 가능해짐
    /*
        public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
            this.em = em;
            this.queryFactory = queryFactory;
        }
    */

    // 등록
    public void save(Member member) {
        em.persist(member);
    }

    // id로 조회
    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember); // 조회 결과가 null 일 수도 있음
    }

    // 전체 조회 - 순수 JPA
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

    // 전체 조회 - querydsl
    public List<Member> findAll_Querydsl() {
        return queryFactory
                .select(member)
                .from(member)
                .fetch();
    }

    // 이름으로 조회 - 순수 JPA
    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    // 이름으로 조회 - querydsl
    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq(username))
                .fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if(hasText(condition.getUsername())) { // StringUtils.hasText() : null, "" 둘 다 체크
            builder.and(member.username.eq(condition.getUsername()));
        }
        if(hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if(condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if(condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression ageBetween(Integer ageLoe, Integer ageGoe) {
        return ageGoe(ageGoe).and(ageLoe(ageLoe)); //  m1_0.age >= 20 and m1_0.age <= 40
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

}
