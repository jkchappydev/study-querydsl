package com.studyquerydsl.repository;

import com.studyquerydsl.dto.MemberSearchCondition;
import com.studyquerydsl.dto.MemberTeamDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

// 1. 사용자 정의 인터페이스 작성
public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition condition);

    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);

}
