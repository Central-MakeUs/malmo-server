package makeus.cmc.malmo.application.service.chat;

import makeus.cmc.malmo.application.helper.chat_room.ChatRoomCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.domain.service.ChatRoomDomainService;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static makeus.cmc.malmo.util.GlobalConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomManagementService 단위 테스트")
class ChatRoomManagementServiceTest {

    @Mock
    private ChatRoomDomainService chatRoomDomainService;

    @Mock
    private MemberQueryHelper memberQueryHelper;

    @Mock
    private ChatRoomCommandHelper chatRoomCommandHelper;

    @InjectMocks
    private ChatRoomManagementService chatRoomManagementService;

    @Nested
    @DisplayName("getSecondMessageByRelationshipStatus")
    class GetSecondMessageByRelationshipStatus {

        @Test
        @DisplayName("SEEING_SOMEONE 상태이면 마음에 두고 있는 상대 메시지를 반환한다")
        void whenSeeingSomeone_thenReturnSeeingSomeoneMessage() {
            // when
            String result = chatRoomManagementService.getSecondMessageByRelationshipStatus(RelationshipStatus.SEEING_SOMEONE);

            // then
            assertThat(result).isEqualTo(INIT_CHAT_MESSAGE_SECOND_SEEING_SOMEONE);
            assertThat(result).contains("마음에 두고 있는 상대와 있었던 상황");
        }

        @Test
        @DisplayName("IN_RELATIONSHIP 상태이면 연인 갈등 메시지를 반환한다")
        void whenInRelationship_thenReturnInRelationshipMessage() {
            // when
            String result = chatRoomManagementService.getSecondMessageByRelationshipStatus(RelationshipStatus.IN_RELATIONSHIP);

            // then
            assertThat(result).isEqualTo(INIT_CHAT_MESSAGE_SECOND_IN_RELATIONSHIP);
            assertThat(result).contains("먼저 연인과 있었던 갈등 상황");
        }

        @Test
        @DisplayName("BREAKUP 상태이면 이별 메시지를 반환한다")
        void whenBreakup_thenReturnBreakupMessage() {
            // when
            String result = chatRoomManagementService.getSecondMessageByRelationshipStatus(RelationshipStatus.BREAKUP);

            // then
            assertThat(result).isEqualTo(INIT_CHAT_MESSAGE_SECOND_BREAKUP);
            assertThat(result).contains("이별 전후로 마음에 남아 있는 상황");
        }

        @Test
        @DisplayName("null 상태이면 SEEING_SOMEONE과 동일한 메시지를 반환한다")
        void whenNull_thenReturnSeeingSomeoneMessage() {
            // when
            String result = chatRoomManagementService.getSecondMessageByRelationshipStatus(null);

            // then
            assertThat(result).isEqualTo(INIT_CHAT_MESSAGE_SECOND_SEEING_SOMEONE);
            assertThat(result).contains("마음에 두고 있는 상대와 있었던 상황");
        }

        @Test
        @DisplayName("모든 메시지는 공통 prefix를 포함한다")
        void allMessages_containCommonPrefix() {
            // when
            String seeingSomeoneMsg = chatRoomManagementService.getSecondMessageByRelationshipStatus(RelationshipStatus.SEEING_SOMEONE);
            String inRelationshipMsg = chatRoomManagementService.getSecondMessageByRelationshipStatus(RelationshipStatus.IN_RELATIONSHIP);
            String breakupMsg = chatRoomManagementService.getSecondMessageByRelationshipStatus(RelationshipStatus.BREAKUP);
            String nullMsg = chatRoomManagementService.getSecondMessageByRelationshipStatus(null);

            // then
            assertThat(seeingSomeoneMsg).contains(INIT_CHAT_MESSAGE_SECOND_PREFIX);
            assertThat(inRelationshipMsg).contains(INIT_CHAT_MESSAGE_SECOND_PREFIX);
            assertThat(breakupMsg).contains(INIT_CHAT_MESSAGE_SECOND_PREFIX);
            assertThat(nullMsg).contains(INIT_CHAT_MESSAGE_SECOND_PREFIX);
        }
    }
}
