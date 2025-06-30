package com.studyquerydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data // 기본 생성자 안 만들어줌
@NoArgsConstructor
public class MemberDto {

    private String username;

    private int age;

    // 가능하긴 하나 좋지 못한 방식이다. DTO는 기본적으로 순수 값 기반 필드로 구성하는것이 좋다.
    /*
        public MemberDto(Member member) {
            this.username = member.getUsername();
            this.age = member.getAge();
        }
    */

    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }

}
