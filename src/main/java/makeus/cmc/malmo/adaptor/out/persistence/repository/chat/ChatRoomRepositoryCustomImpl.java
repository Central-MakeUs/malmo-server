package makeus.cmc.malmo.adaptor.out.persistence.repository.chat;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatRoomEntity;
import makeus.cmc.malmo.domain.value.state.ChatRoomState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static makeus.cmc.malmo.adaptor.out.persistence.entity.chat.QChatRoomEntity.chatRoomEntity;

@RequiredArgsConstructor
public class ChatRoomRepositoryCustomImpl implements ChatRoomRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public Page<ChatRoomEntity> loadChatRoomListByMemberId(Long memberId, String keyword, Pageable pageable) {
        // DELETED만 제외하고 조회
        BooleanExpression baseCondition = chatRoomEntity.memberEntityId.value.eq(memberId)
                .and(chatRoomEntity.chatRoomState.ne(ChatRoomState.DELETED));
        
        // 키워드 검색 (제목 기준)
        BooleanExpression keywordCondition = keyword == null || keyword.isBlank()
                ? null
                : chatRoomEntity.title.containsIgnoreCase(keyword);

        List<ChatRoomEntity> content = queryFactory.selectFrom(chatRoomEntity)
                .where(baseCondition.and(keywordCondition))
                .orderBy(chatRoomEntity.lastMessageSentTime.desc())  // 최근 활동 순
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory.select(chatRoomEntity.count())
                .from(chatRoomEntity)
                .where(baseCondition.and(keywordCondition))
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public boolean isMemberOwnerOfChatRooms(Long memberId, List<Long> chatRoomIds) {
        Long count = queryFactory.select(chatRoomEntity.count())
                .from(chatRoomEntity)
                .where(chatRoomEntity.memberEntityId.value.eq(memberId)
                        .and(chatRoomEntity.chatRoomState.ne(ChatRoomState.DELETED))
                        .and(chatRoomEntity.id.in(chatRoomIds)))
                .fetchOne();

        return count != null && count == chatRoomIds.size();
    }

    @Override
    public void deleteChatRooms(List<Long> chatRoomIds) {
        queryFactory.update(chatRoomEntity)
                .set(chatRoomEntity.chatRoomState, ChatRoomState.DELETED)
                .where(chatRoomEntity.id.in(chatRoomIds))
                .execute();
    }


    @Override
    public int countChatRoomsByMemberId(Long memberId) {
        return queryFactory
                .select(chatRoomEntity.count().intValue())
                .from(chatRoomEntity)
                .where(chatRoomEntity.memberEntityId.value.eq(memberId)
                        .and(chatRoomEntity.chatRoomState.eq(ChatRoomState.ALIVE)))
                .fetchOne();
    }
}
