package makeus.cmc.malmo.domain.value.state;

public enum OutboxState {
    PENDING, // 메시지 발행 대기
    SENT, // 메시지 발행 완료
    FAILED, // 메시지 발행 실패 또는 메시지 처리 실패
    DONE, // 메시지 처리 완료
    DEAD // 최대 재시도 횟수 초과 — 더 이상 재시도하지 않는 영구 실패 상태
}
