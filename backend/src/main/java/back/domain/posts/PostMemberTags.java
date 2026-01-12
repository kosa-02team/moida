package back.domain.posts;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMemberTags {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_member_tag_id")
    private Long postMemberTagId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name="member_id")
    private Long memberId;

    private PostMemberTags(Long postId, Long memberId) {
        this.postId = postId;
        this.memberId = memberId;
    }

    public static PostMemberTags of(Long postId, Long memberId) {
        return new PostMemberTags(postId, memberId);
    }

}
