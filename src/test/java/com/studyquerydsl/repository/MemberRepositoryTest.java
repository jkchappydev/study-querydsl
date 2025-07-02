package com.studyquerydsl.repository;

import com.studyquerydsl.dto.MemberSearchCondition;
import com.studyquerydsl.dto.MemberTeamDto;
import com.studyquerydsl.entity.Member;
import com.studyquerydsl.entity.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MemberRepositoryTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        Assertions.assertThat(findMember).isEqualTo(member);

        List<Member> findMemberByUsername = memberRepository.findByUsername(member.getUsername());
        Assertions.assertThat(findMemberByUsername).containsExactly(member);

        List<Member> findMemberAll = memberRepository.findAll();
        Assertions.assertThat(findMemberAll).containsExactly(member);
    }

    @Test
    public void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberRepository.search(condition);
        Assertions.assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    public void searchPageComplex() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);

        Page<MemberTeamDto> result = memberRepository.searchPageComplex(condition, pageRequest);

        Assertions.assertThat(result.getSize()).isEqualTo(3);
        Assertions.assertThat(result.getContent()).extracting("username").containsExactly("member1", "member2", "member3");
    }

}