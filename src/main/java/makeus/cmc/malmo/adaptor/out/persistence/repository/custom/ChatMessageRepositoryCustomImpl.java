package makeus.cmc.malmo.adaptor.out.persistence.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.LoadCurrentMessagesPort;
import makeus.cmc.malmo.domain.value.state.SavedChatMessageState;

import java.util.List;

import static makeus.cmc.malmo.adaptor.out.persistence.entity.chat.QChatMessageEntity.chatMessageEntity;
import static makeus.cmc.malmo.adaptor.out.persistence.entity.chat.QSavedChatMessageEntity.savedChatMessageEntity;

@RequiredArgsConstructor
public class ChatMessageRepositoryCustomImpl implements ChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<LoadCurrentMessagesPort.ChatRoomMessageRepositoryDto> loadCurrentMessagesDto(Long chatRoomId, int page, int size) {
        return queryFactory.select(Projections.constructor(LoadCurrentMessagesPort.ChatRoomMessageRepositoryDto.class,
                        chatMessageEntity.id,
                        chatMessageEntity.senderType,
                        chatMessageEntity.content,
                        chatMessageEntity.createdAt,
                        savedChatMessageEntity.isNotNull()
                ))
                .from(chatMessageEntity)
                .leftJoin(savedChatMessageEntity)
                .on(savedChatMessageEntity.chatMessageEntityId.value.eq(chatMessageEntity.id)
                        .and(savedChatMessageEntity.savedChatMessageState.eq(SavedChatMessageState.ALIVE)))
                .where(chatMessageEntity.chatRoomEntityId.value.eq(chatRoomId))
                .orderBy(chatMessageEntity.createdAt.desc())
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    @Override
    public void updateChatMessageSummarizedAllTrue(Long chatRoomId) {
        queryFactory.update(chatMessageEntity)
            .set(chatMessageEntity.isSummarized, true)
            .where(chatMessageEntity.chatRoomEntityId.value.eq(chatRoomId)
                    .and(chatMessageEntity.isSummarized.eq(false)))
            .execute();
    }
}
