package study.querydsl.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter //운영에서는 엔터티에 Setter를 사용하지 말아야함.
@NoArgsConstructor(access = AccessLevel.PROTECTED) //protected 기본생성자를 만들어줌 (JPA를 사용하려면 기본생성자가 필요하다.)
@ToString(of = {"id", "username", "age"}) //team과 같이 연관관계는 ToString에 넣으면 무한루프에 빠지기 때문에 넣으면 안된다.
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
        this.team = team;
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username) {
        this(username, 0, null);
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
