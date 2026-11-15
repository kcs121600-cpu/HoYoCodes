# HoYo Codes — 설치 후 업데이트를 편하게 받는 법

이 앱은 새 코드 자체를 **인터넷에서 받아오기 때문에 코드를 수집할 때마다 APK 업데이트가 필요하지 않습니다**. 앱 기능이 변경되었을 때만 릴리스 설치가 필요합니다.

## 개인용 업데이트 채널 최초 1회 설정

1. GitHub에서 **공개 저장소**를 하나 만듭니다. 예: `myname/HoYoCodes` (이름은 예시). 이 프로젝트를 올립니다. 계정 비밀번호나 HoYo 로그인 정보는 저장소에 올리지 않습니다.
2. **Android 릴리스 서명키(JKS)**를 한 번 만들고 안전한 장소에 별도 백업합니다. 빌드에 쓸 키가 바뀌면 설치된 앱 위에 업데이트할 수 없습니다. 키를 잃어버리면 업데이트를 못할 수 있습니다.
3. GitHub 저장소의 **Settings > Secrets and variables > Actions**에 `HOYO_KEYSTORE_B64`, `HOYO_KEYSTORE_PASSWORD`, `HOYO_KEY_ALIAS`, `HOYO_KEY_PASSWORD`를 저장합니다.
4. `.github/workflows/publish-release.yml`이 포함된 코드를 저장소에 올리고, **Actions > Publish signed APK (manual) > Run workflow**에서 `1.2.0`을 입력합니다. 같은 값으로 `app/build.gradle`의 `versionName`이 설정되어 있어야 합니다. `versionCode`는 다음 버전마다 증가시킵니다.
5. Actions가 성공하면 **Releases**에 서명된 `HoYoCodes-release.apk`가 올라옵니다.
6. Y700에서 그 APK를 처음 설치합니다. 설치된 앱의 **설정 > GitHub 업데이트 저장소 설정**에 `myname/HoYoCodes`를 입력합니다.
7. 이후에는 앱이 정기적으로 릴리스의 새 버전을 확인하고, 새 버전이 있으면 **업데이트 알림**을 표시합니다. 알림을 눌러 공식 GitHub 다운로드 링크를 열고 **Android 설치 승인**을 하면 업데이트됩니다.

## Windows에서 서명키 만들기

Android Studio의 JDK(Java 17+)를 사용할 수 있습니다. `keytool`을 PATH에서 실행 가능하게 한 뒤 아래 명령을 실행합니다.

```sh
keytool -genkeypair -v -keystore hoyocodes-release.jks -alias hoyocodes -keyalg RSA -keysize 3072 -validity 10000
```

입력한 **keystore 비밀번호와 key 비밀번호**를 안전하게 기록하고, `hoyocodes-release.jks`를 별도 백업하세요.

Windows PowerShell에서 GitHub Secret으로 등록할 Base64를 생성하려면:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\경로\hoyocodes-release.jks")) | Set-Clipboard
```

생성 결과를 GitHub Actions Secret `HOYO_KEYSTORE_B64`에 붙여넣고, 비밀번호와 별칭도 각각 Secret으로 저장하세요. **개인 채팅·공개 저장소·APK 내부에 키 파일이나 비밀번호를 넣지 마세요.**

## 중요한 한계

- 자동으로 *새 버전을 확인하고 알리는 것*이지, **무인 자동 설치**가 아닙니다. Android에서 설치 확인이 필요할 수 있습니다. Play 스토어 자동 업데이트와는 다릅니다.
- 새 버전 확인은 연결한 **공개 GitHub Releases**만 지원합니다. 저장소를 연결하지 않으면 업데이트 확인은 비활성화됩니다.
- 업데이트를 인지하고 다운로드하기 위해 **휴대폰 인터넷 연결**이 필요합니다. `JobScheduler`는 절전 정책상 실행이 늦어질 수 있습니다.
- 첫 설치 시 **debug APK를 사용했다면 서명된 release APK로 서명키가 달라서 곧바로 업그레이드가 되지 않을 수 있습니다.** 이 경우 기존 앱에서 JSON 백업을 먼저 내보낸 후 앱을 제거하고, release 버전을 설치한 뒤 JSON 백업을 복원해야 합니다.
- 이후 **계속 같은 JKS/alias/서명키**로 APK를 배포해야 사용한 코드 기록을 유지하면서 업그레이드됩니다.
- 외부 저장소에서 변경된 APK를 함부로 설치하지 않도록, 반드시 **본인 GitHub 저장소**를 설정하세요. Android 시스템이 기존 앱과 다른 서명의 업데이트를 거부합니다.

## 방송 코드 추가 수집

기본 수집 API(hoyo-codes 및 Ennead)에 더해 `https://gamecal.nv5.me/api/events?game=starrail`와 `https://gamecal.nv5.me/api/events?game=zzz`를 확인합니다. 이 서드파티 게임 캘린더는 **미요우셔(米游社) 공식 방송 API/공식 계정 포스트를 수집해 방송 코드**와 만료 시각을 제공한다고 문서에 밝힙니다.

- `:livestream-code:` 식별자가 있는 이벤트만 감지하고, **방송 일정(schedule)은 코드로 오인하지 않습니다**.
- 만료 일시가 이미 지난 이벤트는 추가하지 않습니다.
- 일반 코드와 동일한 게임+문자열이라면 중복으로 나타나지 않고, 수집 출처를 "방송 한정"으로 표시합니다.
- 중국 공식 피드 기반이라 **한국/글로벌 리딤코드 적용 가능 여부는 개별 확인**이 필요합니다.
- 해당 API가 일시적으로 접속 불가하거나 반환 형식이 변경되면 방송 코드를 놓칠 수 있습니다. **현재 제작 환경에서는 실제 API 접속 테스트를 하지 못했습니다.**
- 최초 기본 검사 주기는 **1시간**으로 설정했습니다. 기존 설치에서 6시간으로 설정한 이력은 유지될 수 있으므로 설정에서 변경하세요. Doze 때문에 실제 실행 시각은 지연될 수 있습니다.
