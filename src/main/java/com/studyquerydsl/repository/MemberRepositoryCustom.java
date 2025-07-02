package com.studyquerydsl.repository;

import com.studyquerydsl.dto.MemberSearchCondition;
import com.studyquerydsl.dto.MemberTeamDto;

import java.util.List;

// 1. 사용자 정의 인터페이스 작성
public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);
}
