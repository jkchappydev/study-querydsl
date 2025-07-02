package com.studyquerydsl.repository;

import com.studyquerydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // MemberJpaRepository
    // save(Member member), findById(Long id), findAll() <- Spring Data JPA에서 기본으로 제공
    // findByUsername(String username) 은 제공하지 않음
    List<Member> findByUsername(String memberName); // 메서드 쿼리
}