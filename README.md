# StelChat Android

스텔라이브 멤버들의 방송 상태와 채팅 기록을 확인하고, 방송 시작·새 채팅 알림을 받을 수 있는 안드로이드 앱입니다.

## 다운로드

- [최신 버전 다운로드 페이지](https://github.com/Eunn00/stelchat-android/releases/latest)
- [StelChat Android 0.1.3 APK 다운로드](https://github.com/Eunn00/stelchat-android/releases/download/v0.1.3/StelChat-Android-0.1.3.apk)
- [StelChat 웹사이트](https://stelchat.xyz)

Android 7.0 이상을 지원합니다. Google Play 스토어가 아닌 GitHub Releases에서 APK로 배포합니다.

## 설치 및 업데이트

1. 최신 릴리즈의 **Assets**에서 `.apk` 파일을 다운로드합니다.
2. 다운로드한 파일을 열어 설치합니다. 휴대폰에서 요청하면 다운로드에 사용한 브라우저 또는 파일 앱에 한해 ‘이 출처의 앱 설치’를 허용합니다.
3. 앱을 열고 설정에서 원하는 멤버의 알림을 켭니다. 알림을 사용하려면 휴대폰의 알림 권한도 허용해 주세요.

업데이트할 때는 같은 앱의 최신 APK를 기존 앱 위에 설치하면 됩니다.

## 주요 기능

- 멤버들의 LIVE 상태 확인과 치지직 방송 바로가기
- 멤버별·최신순 채팅 기록 확인 및 채팅방 펼쳐보기
- 읽지 않은 채팅 표시와 모두 확인
- 멤버별 방송 시작·새 채팅 알림 설정
- 특정 멤버와 채팅방 조합의 채팅 알림만 끄기
- 다크 모드와 마지막으로 선택한 멤버 기억
- 채팅 이모티콘 표시 및 기기 내 이미지 캐시

기록은 StelChat 서버에 수집된 범위에서 제공됩니다. 모든 채널의 모든 과거 채팅을 제공하는 서비스는 아닙니다.

## 알림 안내

알림 설정에서 멤버별 방송 시작·새 채팅 알림을 선택할 수 있습니다.

| 알림 방식 | 동작 |
|---|---|
| 소리 | 소리가 있는 시스템 알림 |
| 진동 | 진동을 사용하는 시스템 알림 |
| 무음 | 소리·진동 없이 알림센터에 표시 |
| 없음 | 모든 시스템 알림 끄기. 멤버별 선택은 유지 |

실제 소리·진동·팝업·잠금 화면 표시 여부는 Android의 알림 채널 설정, 방해 금지 모드, 기기 설정에 영향을 받습니다.

백그라운드 알림은 Google Firebase Cloud Messaging(FCM)을 사용합니다. Google Play 서비스와 인터넷 연결이 필요하며, 화면이 꺼져 있어도 수신할 수 있습니다. 다만 앱 강제 중지, 제조사의 절전 제한, 네트워크 상태 등에 따라 지연되거나 수신하지 못할 수 있습니다. 알림은 즉시 도착하거나 누락 없이 전달되는 것을 보장하지 않으며, 채팅 기록은 앱에서 다시 확인할 수 있습니다.

알림 연결이 완료되지 않았다는 안내가 나오면 네트워크와 휴대폰 알림 권한을 확인한 뒤, 설정의 ‘알림 연결 다시 확인’을 이용해 주세요.

## 개인정보 및 기기 저장

- 별도의 StelChat 회원가입 없이 이용합니다.
- 알림 설정, 선택한 멤버, 읽음 기록, 채팅방 알림 끄기 설정은 기기에 저장되며 다른 기기와 동기화되지 않습니다.
- 프로필과 이모티콘 이미지는 기기 내 캐시로 재사용합니다. 설정에서 캐시를 삭제할 수 있으며, Android가 캐시를 정리하면 다음 이용 시 다시 내려받습니다.
- 앱의 Android 자동 백업은 비활성화되어 있습니다.
- 알림을 사용하면 Google FCM이 알림 전달을 위해 기기 등록 토큰과 알림 구독 정보를 처리합니다. StelChat 알림 중계 서버는 기기 등록 토큰을 별도 데이터베이스에 보관하지 않고 토픽 방식으로 알림을 전송합니다.
- 앱은 StelChat 및 Google 등의 서비스와 인터넷으로 통신합니다. 따라서 웹사이트의 안내와 별개로, 앱이 어떠한 정보도 처리하지 않는다고 보장하지는 않습니다.

문의는 [StelChat 웹사이트](https://stelchat.xyz) 하단의 **Contact**를 이용해 주세요.

## 서비스 안내

StelChat은 개인이 운영하는 비공식 프로젝트입니다. NAVER Corporation, CHZZK 및 스텔라이브의 공식 앱이 아니며, 해당 서비스·권리자와의 제휴를 의미하지 않습니다. 별도의 광고/수익을 내지 않습니다.

서버 점검, 외부 서비스 변경, 수집 연결 문제 등에 따라 기록 제공이나 알림이 일시적으로 제한될 수 있습니다.

## 개발 및 직접 빌드

일반 사용자는 위의 APK만 설치하면 됩니다. 아래 내용은 소스에서 직접 빌드할 때의 안내입니다.

Node.js/npm, JDK 21 이상, Android SDK가 필요합니다. SDK 플랫폼 및 빌드 도구 버전은 `android/variables.gradle`과 Gradle 설정을 참고하세요.

```sh
npm ci
npm test
npm run sync
cd android
./gradlew assembleDebug
```

Windows에서는 마지막 명령 대신 `gradlew.bat assembleDebug`를 사용합니다. 디버그 APK는 `android/app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.

직접 빌드한 앱에서 FCM 알림을 사용하려면 Android 패키지 `xyz.stelchat.android`에 맞는 Firebase 설정이 필요합니다. `google-services.json`은 `android/app/`에 별도로 배치하며 Git에는 포함하지 않습니다. 서버용 Firebase 서비스 계정 키, 치지직 쿠키, 서명 키를 APK나 공개 저장소에 넣지 마세요. 알림 중계 서버는 앱과 별도로 운영합니다.

브라우저 UI 미리보기는 `npm run dev`, Android 네이티브 단위 테스트는 `android/gradlew -p android testDebugUnitTest`로 실행할 수 있습니다. 브라우저 미리보기만으로는 휴대폰의 시스템 알림이나 백그라운드 수신을 검증할 수 없습니다.

## 사용 기술 및 고지

Capacitor, AndroidX, Firebase Messaging 등의 오픈소스 구성 요소를 사용합니다. 공통 UI 의존성의 고지는 [THIRD_PARTY_NOTICES.md](public/desktop/THIRD_PARTY_NOTICES.md)를 참고하세요. 각 구성 요소에는 해당 프로젝트의 라이선스가 별도로 적용됩니다.
