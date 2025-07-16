package makeus.cmc.malmo.domain.model.member;

import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.id.MemberId;

import java.time.LocalDateTime;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class MemberMemory {
    private Long id;
    private MemberId memberId;
    private String content;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public static MemberMemory from(Long id, MemberId memberId, String content,
                                  LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime deletedAt) {
        return MemberMemory.builder()
                .id(id)
                .memberId(memberId)
                .content(content)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public static MemberMemory createMemberMemory(MemberId memberId, String content) {
        return MemberMemory.builder()
                .memberId(memberId)
                .content(content)
                .build();
    }
}