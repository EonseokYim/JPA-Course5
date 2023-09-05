package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor //기본생성자가 없으면 querydsl 프로젝션할 수 없다.
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection //DTO에다가 바로 이 어노테이션을 붙이고 gradle -> compileQueryDSL를 하면.... 이걸하면 DTO도 Q파일을 만들어준다,. 미쳤다 ㄷㄷ.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
