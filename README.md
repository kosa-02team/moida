# MOIDA

## Project 소개

MOIDA는 모임 관리와 정산을 하나의 플랫폼에서 해결할 수 있는 **All-in-One 모임 관리 서비스**입니다.

기존에는 카카오톡, 네이버 밴드, 엑셀 등 여러 도구를 오가며 모임을 관리해야 했습니다.  
MOIDA는 일정 관리, 정산, 데이터 관리를 하나의 서비스로 통합하여  
모임 운영의 **관리 피로도를 줄이고 데이터 정합성을 보장하는 것**을 목표로 합니다.

🔗 **[MOIDA 발표 자료 보기](https://www.canva.com/design/DAG_KiYG0l0/eLCF9qsVQp2_8jjlim2bCQ/view?utm_content=DAG_KiYG0l0&utm_campaign=designshare&utm_medium=link2&utm_source=uniquelinks&utlId=h6224ef9b99)**

---

## 기술 스택

### Frontend
- React
- TypeScript
- TailwindCSS
- Vite

### Backend
- Java
- Spring Boot
- Spring MVC
- Spring Security
- JPA

### Data Infra
- MySQL
- ChromaDB (Vector DB)

### External API
- Gemini API
- Open Banking API (Stub 기반 Interface 구현)

### Collaboration
- GitHub
- Notion
- Discord

---

## 주요 기능

- **모임 생성 및 관리**  
  모임 생성 및 멤버 관리 기능 제공

- **자동 정산 시스템**  
  은행 거래 내역 기반 자동 정산 및 장부 생성

- **거래 내역 조회 및 관리**  
  수입 / 지출 내역 기록 및 조회

- **일정 관리 기능**  
  모임 일정 생성 및 관리

- **데이터 정합성 보장**  
  실제 거래 내역 기반 데이터 처리로 입력 오류 방지

---

## 아키텍처
<p align="center">
  <img width="463" height="332" alt="image" src="https://github.com/user-attachments/assets/3ac518ec-fc1e-4c97-aa3f-de1d811d58f9" />
</p>

## 실행 방법

### 1. Repository Clone

```bash
git clone https://github.com/your-repository/moida.git
cd moida
```
### 2. Database 실행

Docker 컨테이너 실행

```bash
docker compose up -d
```

### 3. Backend 실행
```bash
cd backend
./gradlew bootRun
```

### 4. Frontend 실행
```bash
cd frontend
npm install
npm run dev
```



