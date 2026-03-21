# 💌 말모 Malmo - AI 연애 상담, 마음 질문

## 🧐 프로젝트 소개

> **"연인은 왜 저런 반응을 할까?", "이럴 땐 어떻게 대응해야 하지?"**

말모(Malmo)는 연인 사이의 갈등과 고민에서 출발한 **애착 유형 기반 AI 연애 갈등 상담 앱**입니다.

MZ세대는 연인과의 갈등 원인으로 '의사소통 방식'과 '성향 차이'를 가장 많이 꼽았으며, 자신과 연인을 이해하려는 니즈가 높습니다.  
말모(Malmo)는 사용자와 연인의 애착 유형 데이터를 기반으로 갈등 상황을 분석하고, 관계 개선을 위한 맞춤형 조언을 제공하는 서비스입니다.

---

## ✨ 주요 기능

### 1. 애착 유형 진단

- ECR 검사 문항 기반 애착 유형 진단
- 사용자가 직접 입력한 커플/상대 정보 기반으로 결과를 해석하고 AI 상담에 활용

### 2. AI 갈등 상담

- 채팅으로 갈등 상황 입력 → AI가 애착 유형 기반 상담 제공
- 상담 종료 후, 요약 리포트 제공

### 3. 커플 질문

- 매일 새로운 커플 질문 제공
- 누적 답변은 AI 상담 분석에 활용 + 커플 레벨 상승 요소 제공

---

## 🖼️ 스크린샷

<div align="center">
   <img width="800" alt="스크린샷" src="https://github.com/user-attachments/assets/e0960f87-1ba4-453c-ab97-2dd3254727de" />
   <img width="800" alt="Frame 1948756838" src="https://github.com/user-attachments/assets/4d61401f-020c-4da2-8260-465331d66fa4" />
</div>

---

## 🛠️ 기술 스택

| Category          | Tools & Technologies |
|-------------------|-----------------------|
| **Frameworks**    | ![Spring](https://img.shields.io/badge/Spring-6DB33F?style=flat&logo=spring&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat&logo=springsecurity&logoColor=white) |
| **Language**      | ![Java](https://img.shields.io/badge/Java%2017-007396?style=flat&logo=openjdk&logoColor=white) |
| **Persistence**   | ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-007396?style=flat&logo=hibernate&logoColor=white) ![QueryDSL](https://img.shields.io/badge/QueryDSL-59666C?style=flat) |
| **Database**      | ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white) |
| **Cloud (AWS)**   | ![EC2](https://img.shields.io/badge/EC2-FF9900?style=flat&logo=amazon-ec2&logoColor=white) ![S3](https://img.shields.io/badge/S3-569A31?style=flat&logo=amazon-s3&logoColor=white) ![RDS](https://img.shields.io/badge/RDS-527FFF?style=flat&logo=amazon-rds&logoColor=white) |
| **Messaging**     | ![redis](https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white) |
| **Container & DevOps** | ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white) |
| **Monitoring**    | ![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=flat&logo=prometheus&logoColor=white) ![Grafana](https://img.shields.io/badge/Grafana-F46800?style=flat&logo=grafana&logoColor=white) |
| **Documentation** | ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black) |
| **Testing**       | ![JUnit5](https://img.shields.io/badge/JUnit%205-25A162?style=flat&logo=junit5&logoColor=white) ![Mockito](https://img.shields.io/badge/Mockito-007396?style=flat&logo=java&logoColor=white) |
| **CI/CD**         | ![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat&logo=githubactions&logoColor=white) |



---

## LLM / CI-CD 설정

- 현재 기본 LLM 구현체는 OpenAI client이며, 기본 모델은 `gpt-5.4-mini`입니다.
- Gemini 구현체도 코드에 유지되며, 필요 시 `@Primary`를 이동해 전환할 수 있습니다.
- QA/Prod 배포 workflow는 API Key, Base URL, Model 같은 민감/배포 환경별 값만 환경변수로 주입합니다.
- `GEMINI_API_KEY`
- `GEMINI_MODEL`
- `GEMINI_BASE_URL`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `OPENAI_BASE_URL`
- `OPENAI_STATUS_URL`
- `reasoning-effort`는 환경변수가 아니라 profile YML에 직접 선언합니다.
- 권장 YML 예시
```yml
openai:
  api:
    reasoning-effort:
      default: medium
      scenarios:
        structured-chat: low
        free-conversation: low
        validation: none
        summary: none
        auxiliary-extraction: none

gemini:
  api:
    reasoning-effort:
      default: high
      scenarios:
        structured-chat: medium
        free-conversation: low
        validation: low
        summary: low
        auxiliary-extraction: low
```
- 롤백 절차
- OpenAI 구현체에 `@Primary`를 옮깁니다.
- 필요 시 `OPENAI_MODEL`만 조정합니다.
- 재배포합니다. reasoning effort 조정은 `application-*.yml` 수정으로 처리합니다.

---

## 🚎 Architecture

<div align="center">
   <img width="700" height="500" alt="malmo_arch drawio" src="https://github.com/user-attachments/assets/1b9b735a-6441-4c18-83b5-fff7647f6345" />
</div>

---

## 📈 DataBase Schema

<img width="3840" height="2566" alt="malmo-erd" src="https://github.com/user-attachments/assets/c848c59a-910d-48e1-9078-7ed61c2bc211" />


---

## 📄 라이선스

이 프로젝트는 [MIT License](./LICENSE)를 따릅니다.  
© 2025 Malmo. All rights reserved.
