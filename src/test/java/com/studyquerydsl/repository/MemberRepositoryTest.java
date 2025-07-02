package com.studyquerydsl.repository;

import com.studyquerydsl.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

}