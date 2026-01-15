package back.domain.vote;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteOptions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "vote_id", nullable = false)
    private Long voteId;

    @Column(name = "option_text", nullable = false, length = 200)
    private String optionText;

    @Column(name = "option_order")
    private Integer optionOrder = 1;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Column(length = 255)
    private String location;

    // 생성자
    public VoteOptions(Long voteId, String optionText, Integer optionOrder, 
                       LocalDateTime eventDate, String location) {
        this.voteId = voteId;
        this.optionText = optionText;
        this.optionOrder = optionOrder != null ? optionOrder : 1;
        this.eventDate = eventDate;
        this.location = location;
    }

    // 도메인 메서드
    public void updateOption(String optionText, LocalDateTime eventDate, String location) {
        this.optionText = optionText;
        this.eventDate = eventDate;
        this.location = location;
    }

    public void updateOrder(Integer order) {
        this.optionOrder = order;
    }
}

