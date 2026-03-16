# API 변경 사항 - 온보딩 및 프로필 필드 확장

## 개요

멤버 프로필에는 다음 3개 필드가 추가되었습니다:
- `relationshipStatus` - 연애 상태 (Enum)
- `personalityType` - 본인 MBTI (String)
- `otherPersonalityType` - 상대방 MBTI (String)

추가로, 커플 초대 코드 기반 연동 플로우는 deprecated 되었고 신규 사용자 플로우는 직접 입력 방식으로 전환되었습니다.
단, `POST /members/onboarding`에서는 이제 `relationshipStatus`만 받고 `personalityType`, `otherPersonalityType`는 받지 않습니다.

---

## 신규 Enum

### RelationshipStatus

| 값 | 설명 |
|---|------|
| `IN_RELATIONSHIP` | 연애 중 |
| `SEEING_SOMEONE` | 썸 타는 중 |
| `BREAKUP` | 이별 후 |

---

## 변경된 API

### 1. POST /members/onboarding (온보딩 - 회원가입)

**변경 전 Request:**
```json
{
  "nickname": "닉네임",
  "terms": [
    { "termsId": 1, "isAgreed": true }
  ]
}
```

**변경 후 Request:**
```json
{
  "nickname": "닉네임",
  "relationshipStatus": "IN_RELATIONSHIP",
  "terms": [
    { "termsId": 1, "isAgreed": true }
  ]
}
```

| 필드 | 타입 | 필수 여부 | 설명 |
|------|------|-----------|------|
| `nickname` | String | 필수 | 닉네임 (1-10자, 한글/영문/숫자) |
| `relationshipStatus` | Enum | 선택 | 연애 상태 |
| `terms` | Array | 필수 | 약관 동의 목록 |

> **Note:** 커플/상대 관련 정보는 초대 코드 연동이 아니라 사용자의 직접 입력을 기준으로 관리합니다.
> **Note:** `personalityType`, `otherPersonalityType`는 회원가입 요청에서는 제거되었고, 필요 시 `PATCH /members`에서 관리합니다.

---

### 2. GET /members (멤버 정보 조회)

**변경 전 Response:**
```json
{
  "memberId": 1,
  "nickname": "닉네임",
  "email": "user@example.com",
  "provider": "KAKAO",
  "loveTypeCategory": "SECURE",
  "anxietyRate": 0.3,
  "avoidanceRate": 0.2,
  "inviteCode": "ABC123",
  "isCouple": true,
  "startLoveDate": "2024-01-01",
  "loveDay": 365
}
```

**변경 후 Response:**
```json
{
  "memberId": 1,
  "nickname": "닉네임",
  "email": "user@example.com",
  "provider": "KAKAO",
  "loveTypeCategory": "SECURE",
  "anxietyRate": 0.3,
  "avoidanceRate": 0.2,
  "inviteCode": "ABC123",
  "isCouple": true,
  "startLoveDate": "2024-01-01",
  "loveDay": 365,
  "relationshipStatus": "IN_RELATIONSHIP",
  "personalityType": "INTJ",
  "otherPersonalityType": "ENFP"
}
```

| 추가된 필드 | 타입 | 설명 |
|-------------|------|------|
| `relationshipStatus` | Enum (nullable) | 연애 상태 |
| `personalityType` | String (nullable) | 본인 MBTI |
| `otherPersonalityType` | String (nullable) | 상대방 MBTI |

> **Note:** 기존 사용자의 경우 위 필드들이 `null`로 반환될 수 있습니다.
> **Note:** 응답의 `inviteCode`, `isCouple`, `startLoveDate`는 레거시 커플 연동 흐름과 연결된 필드이므로 신규 클라이언트 플로우의 기준으로 사용하지 않는 것을 권장합니다.

---

### 3. PATCH /members (멤버 정보 수정)

**변경 전 Request:**
```json
{
  "nickname": "새닉네임"
}
```

**변경 후 Request:**
```json
{
  "nickname": "새닉네임",
  "relationshipStatus": "SEEING_SOMEONE",
  "personalityType": "ENFP",
  "otherPersonalityType": "INTJ"
}
```

| 필드 | 타입 | 필수 여부 | 설명 |
|------|------|-----------|------|
| `nickname` | String | 선택 | 닉네임 (1-10자, 한글/영문/숫자) |
| `relationshipStatus` | Enum | 선택 | 연애 상태 |
| `personalityType` | String | 선택 | 본인 MBTI |
| `otherPersonalityType` | String | 선택 | 상대방 MBTI |

> **Note:** 모든 필드가 선택 사항입니다. 전달된 필드만 업데이트됩니다 (부분 업데이트 지원).

**변경 전 Response:**
```json
{
  "nickname": "새닉네임"
}
```

**변경 후 Response:**
```json
{
  "nickname": "새닉네임",
  "relationshipStatus": "SEEING_SOMEONE",
  "personalityType": "ENFP",
  "otherPersonalityType": "INTJ"
}
```

---

## 마이그레이션 가이드

### 기존 사용자 처리
- 기존 사용자들의 `relationshipStatus`, `personalityType`, `otherPersonalityType` 값은 `null`입니다.
- 클라이언트에서 `null` 값을 적절히 처리해야 합니다.

### 온보딩 화면 업데이트
1. 닉네임 입력 후 추가 정보 입력 화면 구성
2. 연애 상태 선택 (3가지 옵션)
3. MBTI 입력은 온보딩 이후 프로필 수정 플로우로 이동

### 프로필 수정 화면 업데이트
1. 기존 닉네임 수정 기능 유지
2. 연애 상태 변경 기능 추가
3. MBTI 수정 기능 추가

---

## 변경 파일 목록

### Domain Layer
- `Member.java` - 3개 필드 추가, `signUp()` 오버로드, `updateMemberProfile()` 확장

### Application Layer
- `SignUpUseCase.java` - SignUpCommand에 연애 상태 필드 추가
- `GetMemberUseCase.java` - MemberResponseDto에 3개 필드 추가
- `UpdateMemberUseCase.java` - Command/ResponseDto에 3개 필드 추가
- `SignUpService.java` - 새 signUp 메서드 호출
- `MemberCommandService.java` - updateMember에서 새 필드 처리
- `MemberInfoService.java` - 응답에 새 필드 매핑
- `MemberQueryHelper.java` - MemberInfoDto에 3개 필드 추가

### Adaptor Layer
- `MemberEntity.java` - 3개 nullable 컬럼 추가
- `MemberMapper.java` - 양방향 매핑 확장
- `MemberPersistenceAdapter.java` - RepositoryDto에 3개 필드 추가
- `MemberRepositoryCustomImpl.java` - QueryDSL select 확장
- `SignUpController.java` - RequestDto에 `relationshipStatus` 추가
- `MemberController.java` - UpdateMemberRequestDto에 3개 필드 추가

### Enum
- `RelationshipStatus.java` - 신규 생성
