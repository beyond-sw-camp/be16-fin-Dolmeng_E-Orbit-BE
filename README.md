# Orbit(오르빗)

> 모든 일정/협업을 하나로! 캘린더·간트·마일스톤·칸반·채팅·문서를 **실시간으로 연동**하는 애자일 기반 통합 프로젝트 플랫폼
> <p align="center">
</p>
<img width="2400" height="800" alt="image" src="https://github.com/user-attachments/assets/045e5ea7-5171-485d-880d-3e9c2110e06a" />

## 👀 팀원 구성
<table>
  <tr>
    <!-- 1행: 사진(클릭 가능) + 이름 -->
    <td align="center">
      <a href="https://github.com/EunDuk2" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/124436476?v=4" width="100px;" alt="조은성"/>
      </a><br />
      <b>조은성</b>
    </td>
        <td align="center">
      <a href="https://github.com/YoungKwanK" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/154659797?v=4" width="100px;" alt="김영관"/>
      </a><br />
      <b>김영관</b>
    </td>
    <td align="center">
      <a href="https://github.com/jominhyeong97" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/206010512?v=4" width="100px;" alt="조민형"/>
      </a><br />
      <b>조민형</b>
    </td>
    <td align="center">
      <a href="https://github.com/ifunhy" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/194198612?v=4" width="100px;" alt="김현지"/>
      </a><br />
      <b>김현지</b>
    </td>
    <td align="center">
      <a href="https://github.com/rm2001kr" target="_blank">
        <img src="https://avatars.githubusercontent.com/u/207882668?v=4" width="100px;" alt="김강산"/>
      </a><br />
      <b>김강산</b>
    </td>
  </tr>
  <tr>
    <!-- 2행: GitHub 배지 -->
    <td align="center">
      <a href="https://github.com/EunDuk2" target="_blank">
        <img src="https://img.shields.io/badge/GitHub_Profile-181717?style=flat-square&logo=github&logoColor=white"/>
      </a>
    </td>
        <td align="center">
      <a href="https://github.com/YoungKwanK" target="_blank">
        <img src="https://img.shields.io/badge/GitHub_Profile-181717?style=flat-square&logo=github&logoColor=white"/>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/jominhyeong97" target="_blank">
        <img src="https://img.shields.io/badge/GitHub_Profile-181717?style=flat-square&logo=github&logoColor=white"/>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/ifunhy" target="_blank">
        <img src="https://img.shields.io/badge/GitHub_Profile-181717?style=flat-square&logo=github&logoColor=white"/>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/rm2001kr" target="_blank">
        <img src="https://img.shields.io/badge/GitHub_Profile-181717?style=flat-square&logo=github&logoColor=white"/>
      </a>
    </td>
  </tr>
</table>

<br/>

## 📖 배경 및 필요성
현대 협업 환경은 Jira, Slack, Notion 등 기능별 SaaS 사용이 보편화되면서 **정보가 파편화**되고 잦은 **컨텍스트 전환**이 발생하는 문제를 겪고 있습니다. 또한, 기존 툴의 복잡한 UI는 사용자의 피로도를 가중시킵니다.

본 프로젝트는 이러한 파편화된 협업 기능을 **단일 플랫폼에 통합**하고, 사용자 친화적인 UI/UX를 제공하여 생산성 저하 문제를 해결하고자 합니다.

핵심 목표는 **마일스톤**(목표 추적), **간트 차트**(작업 흐름), **캘린더**(일정 관리) 기능을 유기적으로 연동하는 것입니다. 하나의 변경 사항이 모든 관련 기능에 **실시간 동기화**되도록 하여, 팀원 누구나 통일된 데이터를 기반으로 프로젝트 전체 흐름을 한눈에 파악하는 효율적인 협업 환경을 구축하는 것을 목표로 합니다.

<br/>

## ✨ 주요 기능

### 1) 📅 통합 일정 관리
- 개인/프로젝트/팀 **통합 캘린더** (월/주/일 뷰)
- 일정 상태/중요도 색상 구분, 날짜 클릭 상세/수정
- **간트 차트·마일스톤과 양방향 연동**, To-Do(개인 작업)까지 한 화면에서

### 2) ⏰ 체계적인 알림
- 마감/회의 등 **예약 알림** (예: 1일 전, 15분 전)
- WebSocket/SSE 기반 **실시간 동기화**
- 워크스페이스/프로젝트/스톤/태스크 **메타데이터 포함 푸시**

### 3) 🛠 스크럼 & 칸반
- 스프린트·태스크·이슈를 **칸반 보드**로 관리
- 진행률/리드타임 등 생산성 지표 시각화

### 4) 💬 실시간 채팅·피드백
- 워크스페이스/프로젝트 **전용 채팅방 자동 개설(옵션)**
- 멘션·명령어·파일 공유, 메시지 내 날짜 → **캘린더 일정 등록**

### 5) 📁 통합 문서 관리
- 워크스페이스/프로젝트/스톤별 문서 및 파일 관리 시스템
- **계층적 폴더 구조**로 문서와 파일을 체계적으로 관리
- AWS S3 기반 파일 저장 
- Kafka 이벤트 기반 **검색 인덱스 자동 동기화**
- 워크스페이스별 스토리지 사용량 추적 및 관리
- 문서/폴더/파일 이동 및 구조 변경 기능

### 6) 📝 실시간 공동 작성
- WebSocket(STOMP) 기반 **실시간 동시 편집** 지원
- Redis Pub/Sub을 통한 편집 변경사항 **실시간 브로드캐스팅**
- 라인 단위 잠금(Lock) 메커니즘으로 **동시 편집 충돌 방지**
- 커서 위치 실시간 동기화 및 온라인 사용자 목록 표시
- **배치 업데이트** 처리 (라인 생성/수정/삭제 일괄 처리)

### 7) 🔍 자동완성 & 검색
- Elasticsearch 기반의 **통합 검색** 및 검색어 **자동완성**
- Nori 한글 형태소 분석기 및 Edge N-gram을 활용한 **정확한 한글 검색**
- 검색어 **하이라이팅** (제목/내용/문서 라인별 하이라이트 표시)
- matchBoolPrefix 기반 **자동완성 제안** (최대 10개 결과)
- 권한 기반 검색 필터링 (사용자별 접근 권한에 따른 결과 제한)

### 8) 🤖 AI 어시스턴트
- 사전 회의 주제 기반 자료 추천
- 회의록/채팅 요약, 태스크 추출

<br/>

## 📋 프로젝트 산출물

| 구분 | 링크 |
| :--- | :--- |
| **Figma** | [🔗 Figma 디자인 보기](https://www.figma.com/design/f8NVDb2aljFlzoDFRRo8bl/Orbit?node-id=0-1&p=f&t=sfF5y7Eb2MaNDDP3-0) |
| **API 명세서** | [🔗 API 명세서 보기](https://docs.google.com/spreadsheets/d/1GIcKtK_Tk5H-6UdCrks_X5xXr9MtWWH2bNvIPpIW59k/edit?gid=1634200920#gid=1634200920) |
| **WBS** | [🔗 WBS 보기](https://docs.google.com/spreadsheets/d/1GIcKtK_Tk5H-6UdCrks_X5xXr9MtWWH2bNvIPpIW59k/edit?gid=2008137453#gid=2008137453) |
| **요구사항 명세서** | [🔗 요구사항 명세서 보기](https://docs.google.com/spreadsheets/d/1GIcKtK_Tk5H-6UdCrks_X5xXr9MtWWH2bNvIPpIW59k/edit?gid=0#gid=0) |

<br/>

## 🧱 ERD

  <img width="5240" height="2943" alt="Final-Dolmeng-E-msa (1)" src="https://github.com/user-attachments/assets/c36920e1-2bee-41e5-bc56-be1aeb23153d" />


<br/>


## 기능 화면

<details id="home">
  <summary><b>🏠 홈</b></summary>
  <br/>
  <img width="1919" height="945" alt="홈화면" src="https://github.com/user-attachments/assets/febd3178-8c65-489c-984d-1731f5248051" />
</details>

<details id="my-schedule">
  <summary><b>📅 내일정</b></summary>
  <br/>
  <details>
    <summary><b>내 일정 홈</b></summary>
    <br/>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/2ea0aa72-9c3d-416c-bf47-cdff6f727641" />   
  </details>
  <br/>
  <details>
    <summary><b>프로젝트 캘린더</b></summary>
    <br/>
    <p><b>프로젝트 캘린더 조회</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/e0ebff3a-0108-4bef-afbe-78b646ad40e1" />  
    <p><b>프로젝트 캘린더 조회 - 숨김</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/2497021e-04d1-4119-94b3-50e17cdd2f60" />   
  </details>
  
  <details>
    <summary><b>공유 캘린더</b></summary>
    <br/>
    <p><b>공유 캘린더 조회</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/91feca78-3584-44f3-bc35-084eed3e9d81" /> 
    <p><b>일정 등록</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/fe4ff621-899b-4991-83fc-259e277f16ae" />   
    <p><b>일정 수정</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/c4a3b3ca-9470-43fe-ad8a-cade5e355a55" />   
    <p><b>일정 삭제</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/4fce0e34-d756-4b7f-b8b7-f93d1c161c22" />   
    <p><b>공유 캘린더 구독</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/fcf37dbc-ce4e-40eb-aeb1-2985440df39e" />   
    <p><b>공유 캘린더 구독 삭제</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/af506ba1-6725-46f7-a05d-e0c898ce28b0" />   
    <p><b>공유 캘린더 숨김</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/a35e651a-b1f3-4bf1-baee-10a14cf10f75" />   
  </details>

 <details>
    <summary><b>todo</b></summary>
    <br/>
    <p><b>todo 등록</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/efdfaa2f-c6e8-4491-8034-a001d0b81457" /> 
    <p><b>todo 수정</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/6b9bfacf-7b68-4ff0-a0df-fbba53445b7b" />   
    <p><b>todo 삭제</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/54166e94-024d-4bef-b8e5-f74f6020a155" />   
    <p><b>todo 완료, 미완료</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/1459b27a-4b7a-4118-897f-19ab1110e542" />   
  </details>
</details>

<details id="drive">
  <summary><b>📁 문서함</b></summary>
  <br/>
  <details>
    <summary><strong>폴더/파일/문서 생성</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/c11b292d-1990-469d-848f-bbe3b072e024" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/4afb999c-6b39-4a24-845d-c365e3fbc97e" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/87aa75a7-4a72-4afd-96b0-7fb2f0ac7503" />
  </details>
  <details>
    <summary><strong>폴더/파일/문서 조회</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/a048a2af-6429-462d-99d5-cfb2d3b91ccd" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/39f4fcf7-d640-42c8-b909-47dfaad681c9" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/65f98a67-6a7a-4f3e-97e2-9087e0b2e4e8" />
  </details>
  <details>
    <summary><strong>폴더/파일/문서 상세 조회</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/304dc758-4d2d-44e8-8fb4-38aff452ba1b" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/de5dec62-fe2e-43eb-b979-1ebbe2bc2e24" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/22190ad5-26fe-4c48-bddf-3a11b0e5fee9" />
  </details>
   <details>
    <summary><strong>폴더/문서 제목 변경</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/8960f969-b92f-4f6f-bdc4-9c4bd0eea429" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/31fc8ec7-6ca4-4015-8232-4b4d8aa6d7ea" />
  </details>
   <details>
    <summary><strong>프로젝트 문서함으로 옮기기</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/1fa69e55-7255-483c-af6f-037e0bc55f38" />
  </details>
  <details>
    <summary><strong>스톤 문서함으로 옮기기</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/4f075dc5-6360-4701-95da-9f73582b79f1" />
  </details>
  <details>
    <summary><strong>폴더/문서/파일 삭제</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/8d4381ad-7471-49eb-86b6-d23f22cf8820" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/8b4781f5-0c98-4733-8863-77307ca29ecd" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/6863f2f8-c749-46c4-b107-bfa91f74dab6" />
  </details>
  <details>
    <summary><strong>트리뷰, 브레드크럼 동작</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/7677faef-5ee2-4ba0-a912-5933e1d80cf2" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/181529df-2a14-4998-b05d-d34209afd303" />
  </details>
  <details>
    <summary><strong>정렬, 필터</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/e66bd977-ca61-48ad-a77e-685a943f94a9" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/e114b20b-9bfc-492c-98a0-63576bad169d" />

  </details>

  
</details>
<details id="document">
  <summary><b>📄 실시간 문서 편집</b></summary>
  <br/>
  <details>
    <summary><strong>동시 문서 편집</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/ba839f4a-8351-4d44-82bd-6523bb7684af" />
  </details>
  <details>
    <summary><strong>툴바 기능</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/875830b6-057f-470a-92e9-bd6beb33735a" />
  </details>
  <details>
    <summary><strong>문서 다운로드</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/0153aaea-ec64-43cd-abf0-b4246932f9a6" />

  </details>
  <details>
    <summary><strong>제목 변경</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/2f04c506-5e1d-48bb-a68e-f136cde6c5dd" />
  </details>


  
</details>

<!-- 프로젝트: 하위 토글 + GIF 공간 -->
<details id="project">
  <summary><b>🗂 프로젝트</b></summary>
  <br/>

  <!-- 프로젝트 -->
  <details>
    <summary><b>프로젝트</b></summary>
    <br/>
    <p><b>프로젝트 생성</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/15d6ebed-e1b2-409c-89fb-c9102feeeb6c" />
    <p><b>프로젝트 수정</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/46fd102d-e5c9-4d60-8eba-f32c4f07373b" />
    <p><b>프로젝트 삭제</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/a7b3d0b6-40f6-4db7-80e6-1f1b5cf043c5" />
    <p><b>프로젝트 대시보드 조회</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/5c6a5aac-988d-4a28-953e-900e6b92a98f" />
    <p><b>프로젝트 간트차트 조회</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/0dd15885-7db9-40e1-967c-38039292f124" />
    <p><b>프로젝트 간트차트 조회</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/80a84b7a-a7cb-4b78-8945-e0c3889b84d8" />
  </details>

  <!-- 스톤 -->
  <details>
    <summary><b>스톤</b></summary>
    <br/>
    <p><b>스톤 생성</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/11c3c345-8bce-4e60-a745-6adf57fb8594" />  
    <p><b>스톤 수정</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/7888fdb5-5a3d-4e00-a3bd-35fd5f049406" />  
    <p><b>스톤 삭제</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/c16a0501-9b03-4dcf-8713-d2eb7d12dde5" />  
    <p><b>스톤 조회(트리)</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/989db430-a8a1-4820-8d93-d33e8707ce6e" />  
    <p><b>스톤 상세조회</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/6b272b29-b8b4-4fad-a830-c4b862b58e87" />  
    <p><b>스톤 완료</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/8e04103e-ad70-4ac5-b0e4-2223f85c1e87" />  
    <p><b>스톤 참여자 변경</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/c6b421d6-fcdf-4be7-b098-d0512e73cd9e" />  
    <p><b>스톤 담당자 변경</b></p>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/2d35e13e-f548-4eb4-963a-5f231376883b" />  
  </details>

  <!-- 태스크 -->
  <details>
    <summary><b>태스크</b></summary>
    <br/>
    <p><b>태스크 생성</b></p>
    <img width="75%" alt="Task Create" src="https://github.com/user-attachments/assets/f69a112f-7e5a-40c2-80b1-2b40bf1e6449" />
    <p><b>태스크 삭제</b></p>
    <img width="75%" alt="Task Delete" src="https://github.com/user-attachments/assets/749d8131-c6ec-456c-bda4-a5283d7b0065" />
    <p><b>태스크 완료</b></p>
    <img width="75%" alt="Task Done" src="https://github.com/user-attachments/assets/08725f12-ad87-43af-b4d2-023150b4d04d" />
    <p><b>태스크 완료 취소</b></p>
    <img width="75%" alt="Task Cancel Done" src="https://github.com/user-attachments/assets/06e95071-8780-4da1-93b2-4257d6194999" />
  </details>
</details>

<!-- 관리자페이지: 하위 토글 + GIF 공간 -->
<details id="admin">
  <summary><b>🛠 관리자페이지</b></summary>
  <br/>

  <!-- 권한 그룹 -->
  <details>
    <summary><b>권한 그룹</b></summary>
    <br/>
    <p><b>권한 그룹 생성</b></p>
    <img width="75%" alt="Access Group Create" src="https://github.com/user-attachments/assets/0a431fdd-2b23-4b08-a8ec-b54e70a2fadc" />    
    <p><b>권한 그룹 수정</b></p>
    <img width="75%" alt="Access Group Update" src="https://github.com/user-attachments/assets/9e720b0a-ed46-4741-93ee-59f3f4775866" />    
    <p><b>권한 그룹 사용자 수정</b></p>
    <img width="75%" alt="Access Group Users Update" src="https://github.com/user-attachments/assets/fe525430-d331-463c-9af5-a619912f34b4" />  
    <p><b>권한 그룹 상세목록 조회</b></p>
    <img width="75%" alt="Access Group Detail" src="https://github.com/user-attachments/assets/a968e582-b647-40b9-bbec-950a319d5809" />
    <p><b>권한 그룹 삭제</b></p>
    <img width="75%" alt="Access Group Delete" src="https://github.com/user-attachments/assets/c39c985c-4ebc-4361-b730-13d775ef72a6" />
  </details>

  <!-- 워크스페이스 -->
  <details>
    <summary><b>워크스페이스</b></summary>
    <br/>
    <p><b>워크스페이스 생성</b></p>
    <img width="75%" alt="Workspace Create" src="https://github.com/user-attachments/assets/b240fa57-5fce-4b20-9074-65d363fb793b" />
    <p><b>워크스페이스 수정</b></p>
    <img width="75%" alt="Workspace Update" src="https://github.com/user-attachments/assets/e520871d-796e-4296-9ae1-02e6d81d9340" /> 
    <p><b>워크스페이스 삭제</b></p>
    <img width="75%" alt="Workspace Delete" src="https://github.com/user-attachments/assets/bdbfce05-27da-4724-8992-b0b2c0fa7e55" />
  </details>

  <!-- 사용자 그룹 -->
  <details>
    <summary><b>사용자 그룹</b></summary>
    <br/>
    <p><b>사용자 그룹 생성</b></p>
    <img width="75%" alt="User Group Create" src="https://github.com/user-attachments/assets/d569201b-ebd6-4616-9db3-654987a876bb" />  
    <p><b>사용자 그룹 수정</b></p>
    <img width="75%" alt="User Group Update" src="https://github.com/user-attachments/assets/6379e2e5-3577-4c75-870b-2decf113bcb8" /> 
    <p><b>사용자 그룹 상세목록 조회</b></p>
    <img width="75%" alt="User Group Detail" src="https://github.com/user-attachments/assets/5a9af53b-e3c2-4c4f-a066-fe9cc7213dc7" />  
    <p><b>사용자 그룹 삭제</b></p>
    <img width="75%" alt="User Group Delete" src="https://github.com/user-attachments/assets/47f7710e-3fbb-4429-824b-22144c8b53de" />    
    <p><b>사용자 그룹 검색</b></p>
    <img width="75%" alt="User Group Search" src="https://github.com/user-attachments/assets/50bc45f0-f8f6-4022-959a-e505ed727779" />
  </details>
</details>

<details id="mypage">
  <summary><b>👤 마이페이지</b></summary>
  <br/>
  <details>
    <summary><strong>마이페이지 조회</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/340676cb-c1af-4a60-8f77-1e7f5054994f" />
  </details>

  <details>
    <summary><strong>마이페이지 조회</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/9264da67-59a2-4487-bf03-5e0b45e5208f" />
  </details>
</details>

<details id="chatbot">
  <summary><b>⌨️ 채팅</b></summary>
  <br/>
    <details>
    <summary><strong>채팅방 생성 및 입장</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/aff29cd9-22a6-4345-835c-3d062d6af17d" />
  </details>

  <details>
    <summary><strong>채팅 기능</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/13bd5d2a-c6e1-4e65-928b-737447c1c714" />
  </details>

  <details>
    <summary><strong>채팅 이미지 업로드 및 이모티콘 사용</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/6db070a7-d649-4978-87be-53a265b4e5a7" />
  </details>

  <details>
    <summary><strong>채팅 요약 미리보기</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/4ba348eb-3d93-4f45-9a1a-3b0c09472dc4" />
  </details>
</details>

<details id="chatbot">
  <summary><b>🗣️ 화상회의</b></summary>
  <br/>
    <details>
    <summary><strong>화상 회의 참여</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/a048a832-6430-414d-909c-37d1a1abe5e5" />
  </details>
  
  <details>
    <summary><strong>음소거/화면 송출 중지</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/d02c0075-0cb7-4ded-bca7-a04be03f841d" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/1e88cc03-dde9-424f-b718-655be4073852" />
  </details>

  <details>
    <summary><strong>화면 공유</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/e9eeb29c-9d20-4ddd-b7f3-c80cf9a951a1" />
  </details>

  <details>
    <summary><strong>화상 회의 나가기</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/3cc6d6db-c424-4984-aef3-c076c865a176" />
  </details>

  <details>
    <summary><strong>화면 이동 및 전체화면</strong></summary>
    <br>
    <img width="75%" alt="Image" src="" />
  </details>

  <details>
    <summary><strong>화상 회의, 채팅 동시 작업</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/3fe3b5c5-ba09-464b-8782-9c89812088d9" />
  </details>

  <details>
    <summary><strong>화상 회의 전체 화면</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/b86fbee3-d5c4-42cb-a8e2-52f64b2bf563" />
  </details>

  <details>
    <summary><strong>화상 회의 참여자 화면 이동</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/b238d1cc-58ca-41cd-b47d-dd9a46efb8ff" />
  </details>
</details>

<details id="chatbot">
  <summary><b>🤖 챗봇</b></summary>
  <br/>
  <p><b>사용가이드</b></p>
      <img width="75%" alt="User Group Search" src="https://github.com/user-attachments/assets/98ea0494-d452-4776-b72b-52906151dbf6" />
  <p><b>일정 등록</b></p>
    <img width="75%" alt="User Group Create" src="https://github.com/user-attachments/assets/ab6faa3d-5352-43bf-948a-625cfc7ef36e" />  
  <p><b>읽지 않은 채팅방 메시지 요약</b></p>
    <img width="75%" alt="User Group Update" src="https://github.com/user-attachments/assets/e9575c0a-d687-4cb4-98ca-7aa4ab00fa03" /> 
  <p><b>일정 브리핑</b></p>
    <img width="75%" alt="User Group Delete" src="https://github.com/user-attachments/assets/b0b448a0-cf5d-47d7-871a-87c43fce30a4" />    
  <p><b>프로젝트 요약</b></p>
    <img width="75%" alt="User Group Search" src="https://github.com/user-attachments/assets/ec8f0157-2127-47cc-bbaa-738c5665e797" />
  <p><b>일상 대화</b></p>
      <img width="75%" alt="User Group Search" src="https://github.com/user-attachments/assets/a9478b7d-c2fd-461f-82c7-fc0d70bd4fe0" />

</details>

<details id="search">
  <summary><b>🔎 검색</b></summary>
  
  <details>
    <summary><strong>파일/문서/스톤/테스크 검색</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/a4d2d473-3ea3-46c4-9c84-fee8adb70a50" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/afb7ef80-1d80-4f05-ae95-aff33c9b7ce9" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/fa8f5334-5aee-4676-a2fe-5094dc9f62d9" />
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/6a7ae9b9-a5d6-4ee1-9b70-eaa1872fb636" />

  </details>

  <details>
    <summary><strong>자동완성</strong></summary>
    <br>
    <img width="75%" alt="Image" src="https://github.com/user-attachments/assets/380e7868-b2ed-4b80-bb00-9f88fa0ab29c" />
  </details>
</details>

<!-- ===== /Feature Toggle Gallery ===== -->




## 🏗️ 시스템 아키텍처
<details>
  <summary><b>시스템 아키텍처 보기</b></summary>
  <img width="1137" height="1197" alt="시스템 아키텍처 최종본 drawio" src="https://github.com/user-attachments/assets/6803e342-9925-43ad-b4e7-22b0a48516db" />

</details>
  

<br/>

## ⚒️ 기술 스택

**Frontend**  
![Vue.js](https://img.shields.io/badge/Vue.js-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Pinia](https://img.shields.io/badge/Pinia-FFB300?style=for-the-badge&logo=pinia&logoColor=black)
![Vue Router](https://img.shields.io/badge/Vue_Router-4FC08D?style=for-the-badge&logo=vuerouter&logoColor=white)
![Vuetify](https://img.shields.io/badge/Vuetify-1867C0?style=for-the-badge&logo=vuetify&logoColor=white)
![MDI](https://img.shields.io/badge/MDI-000000?style=for-the-badge&logo=materialdesignicons&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white)
![FullCalendar](https://img.shields.io/badge/FullCalendar-3178C6?style=for-the-badge&logo=fullcalendar&logoColor=white)
![Chart.js](https://img.shields.io/badge/Chart.js-FF6384?style=for-the-badge&logo=chartdotjs&logoColor=white)
![ApexCharts](https://img.shields.io/badge/ApexCharts-008FFB?style=for-the-badge&logo=apexcharts&logoColor=white)
![Vue Flow](https://img.shields.io/badge/Vue_Flow-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)
![Tiptap](https://img.shields.io/badge/Tiptap-000000?style=for-the-badge&logo=tiptap&logoColor=white)
![ProseMirror](https://img.shields.io/badge/ProseMirror-000000?style=for-the-badge&logo=prosemirror&logoColor=white)
![STOMP.js](https://img.shields.io/badge/STOMP.js-grey?style=for-the-badge&logo=websocket&logoColor=white)
![SockJS](https://img.shields.io/badge/SockJS-grey?style=for-the-badge&logo=javascript&logoColor=white)
![OpenVidu](https://img.shields.io/badge/OpenVidu-273A8B?style=for-the-badge&logo=openvidu&logoColor=white)
![Axios](https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Docx](https://img.shields.io/badge/docx-2B579A?style=for-the-badge&logo=microsoftword&logoColor=white)
![Lodash](https://img.shields.io/badge/Lodash-3492FF?style=for-the-badge&logo=lodash&logoColor=white)
![UUID](https://img.shields.io/badge/UUID-grey?style=for-the-badge)
![Core-JS](https://img.shields.io/badge/Core_JS-F7E018?style=for-the-badge&logo=javascript&logoColor=black)
![File Saver](https://img.shields.io/badge/File_Saver-blue?style=for-the-badge)


**Backend**  
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring WebFlux](https://img.shields.io/badge/Spring_WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud_Gateway-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Netflix Eureka](https://img.shields.io/badge/Netflix_Eureka-6DB33F?style=for-the-badge&logo=netflix&logoColor=white)
![OpenFeign](https://img.shields.io/badge/OpenFeign-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Resilience4j](https://img.shields.io/badge/Resilience4j-8A2BE2?style=for-the-badge)
<br>
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=websocket&logoColor=white)
![STOMP](https://img.shields.io/badge/STOMP-grey?style=for-the-badge)
![OpenVidu](https://img.shields.io/badge/OpenVidu-273A8B?style=for-the-badge&logo=openvidu&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
<br>
![Lombok](https://img.shields.io/badge/Lombok-000000?style=for-the-badge&logo=lombok&logoColor=white)
![Jackson](https://img.shields.io/badge/Jackson-grey?style=for-the-badge)
![Apache Tika](https://img.shields.io/badge/Apache_Tika-D22128?style=for-the-badge&logo=apache&logoColor=white)
![JSoup](https://img.shields.io/badge/JSoup-FF9900?style=for-the-badge)
![Apache HttpClient](https://img.shields.io/badge/Apache_HttpClient-D22128?style=for-the-badge&logo=apache&logoColor=white)

**Database**  
![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Redis Stack](https://img.shields.io/badge/Redis_Stack-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white)

**Deployment & DevOps**  
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)
![n8n](https://img.shields.io/badge/n8n-1A1A1A?style=for-the-badge&logo=n8n&logoColor=white)
<br>
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Maven Central](https://img.shields.io/badge/Maven_Central-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![GitHub Packages](https://img.shields.io/badge/GitHub_Packages-000000?style=for-the-badge&logo=github&logoColor=white)

**Collaboration & Tools**

![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)
![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)
![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white)
![Asana](https://img.shields.io/badge/Asana-8D8D8D?style=for-the-badge&logo=asana&logoColor=white)
![ERDCloud](https://img.shields.io/badge/ERDCloud-0099FF?style=for-the-badge&logo=erdcloud&logoColor=white)


<br/>

## 📝 프로젝트 회고

| 팀원 | 회고 내용 |
|------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 조은성 |  |
| 김영관 | 이번 프로젝트에서 문서함, 통합 검색, 실시간 문서 편집이라는 세 가지 핵심 도메인을 개발했습니다.<br>문서함: 처음엔 단순 `parentId` 구조로 계층형 폴더를 설계했지만, 조상 경로 조회 시 발생하는 재귀 쿼리와 N+1 능 이슈에 직면했습니다. <br>이는 MariaDB의 `WITH RECURSIVE`를 도입해 단일 네이티브 쿼리로 해결하며 견고한 데이터 뼈대를 완성할 수 있었습니다.<br>통합 검색: Elasticsearch의 기본 `analyzer`는 한글 검색에 한계가 명확했습니다. <br>`Nori` 플러그인을 도입해 형태소 분석을 적용하고 나서야 비로소 제대로 된 검색이 가능했습니다. <br>이 과정에서 '정확도' 중심의 검색과 '성능' 중심의 자동완성은 쿼리 접근 방식과 데이터 모델링 자체가 달라야 함을 깨달았습니다.<br>실시간 편집은 가장 어려웠던 부분이었습니다.문서 전체 내용을 STOMP로 전송하니, 메시지 크기 한계로 인한 메모리 이슈와 편집 내용이 덮어써지는 동시성 문제가 발생했고 한 글자가 수정될 때마다 전송할 시 "안녕하세요" 5글자에 DB 트랜잭션 5개가 몰리는 등, DB 과부하라는 새로운 문제에 직면했습니다. <br> '라인(Line) 단위'로 변경 사항을 식별하고, 여러 수정을 '배치(Batch) 업데이트'로 묶어 처리하도록 구현했습니다.<br> 이로써 STOMP 메시지 크기와 DB 트랜잭션 횟수를 동시에 최적화하여 성능과 동시성을 확보했습니다. |
| 조민형 |  |
| 김현지 |  |
| 김강산 |  |

