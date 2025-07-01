package com.studyquerydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.studyquerydsl.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.studyquerydsl.entity.QMember.member;

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

}
