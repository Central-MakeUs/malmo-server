package makeus.cmc.malmo.domain.value.state;

public enum ChatRoomState {
    BEFORE_INIT,  // 초기화 전 (사용자 첫 메시지 전)
    ALIVE,        // 진행 중 (사용자 첫 메시지 후)
    COMPLETED,    // 기존 완료된 채팅방 (보고서 조회용)
    DELETED       // 삭제됨 (soft delete)
}