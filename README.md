# andersNFSmount

Android/Kotlin-projektin suunnittelupohja user-space NFS -ominaisuudelle.

## Suunnittelun lähtökohta

Tavoitteena on tuoda NFS-palvelimen tiedostot Androidissa käyttäjälle näkyviin ilman root-oikeuksia tai kernel-tason `mount`-operaatiota. Ensisijainen toteutustapa on Androidin Storage Access Frameworkiin perustuva `DocumentsProvider`, joka näyttää NFS-jaon tiedostovalitsimille ja SAF-yhteensopiville sovelluksille.

Katso tarkempi arkkitehtuuri- ja vaiheistussuunnitelma dokumentista [`docs/android-userspace-nfs-plan.md`](docs/android-userspace-nfs-plan.md).

## Kehitysympäristön oletus

- Gradle 9.4.1 tai muu Android-projektin kanssa yhteensopiva paikallinen Gradle-asennus
- JVM 17
- Android SDK asennettuna WSL Debianissa tai Android Studiossa
- Windows 10:n `adb`, jos puhelin näkyy Windowsille mutta ei WSL:lle

## Projektin nykytila

Tämä repository on tällä hetkellä JVM/Kotlin proof-of-concept ja suunnittelupohja. Se kääntää `src/main/kotlin`-hakemiston ja ajaa komentoriviltä `main`-funktion, mutta tästä repositorystä ei nyt tehdä Android APK:ta.

Aiemmin käsin tehty `app/`-moduuliyritys on poistettu. Seuraava järkevä etenemistapa ei ole kirjoittaa Android-projektin rakennetta käsin tähän repositoryyn, vaan ottaa pohjaksi valmis Android-esimerkkiprojekti, jossa Gradle-, Android SDK-, manifesti- ja `DocumentsProvider`-rakenne on jo valmiina.

## Testaus PC-hostissa

Näillä ohjeilla voit testata nykyisen JVM/Kotlin-version Linux-, macOS- tai Windows-hostissa.

### 1. Tarkista työkalut

Asenna JDK 17 ja Gradle. Tarkista versiot projektin juuressa:

```bash
java -version
gradle --version
```

`java -version`-komennon pitäisi näyttää JDK 17. Gradlen pitäisi olla sellainen versio, jolla nykyinen Kotlin-käännös toimii.

### 2. Käännä projekti

Aja projektin juuressa:

```bash
gradle clean build
```

Onnistunut käännös päättyy yleensä tekstiin `BUILD SUCCESSFUL`.

### 3. Aja komentorivitesti

Aja:

```bash
gradle run
```

Nykyisessä proof-of-concept-versiossa odotettu tuloste on:

```text
andersNFSmount Kotlin project is ready.
```

### 4. Tarkista tuotettu JAR

Build tuottaa JAR-tiedoston hakemistoon `build/libs/`. Voit tarkistaa sen esimerkiksi näin:

```bash
find build/libs -maxdepth 1 -type f -name '*.jar' -print
```

Jos haluat ajaa JARin suoraan, lisää Kotlinin standardikirjasto luokkapolkuun. Helpoin suositeltu ajotapa tässä projektissa on kuitenkin `gradle run`, koska Gradle lisää tarvittavan luokkapolun automaattisesti.

## Android-pohja: lataa valmis StorageProvider-esimerkkiprojekti

Tähän NFS-sovellukseen sopivin pohja on Androidin `DocumentsProvider`-esimerkki, ei tyhjä itse käsin rakennettu Gradle-projekti. Androidin Storage Access Frameworkissa juuri `DocumentsProvider` on se komponentti, jolla sovellus voi näyttää omat tiedostonsa Androidin tiedostovalitsimessa ja SAF-yhteensopivissa sovelluksissa.

Sopiva valmis pohja on Androidin virallinen StorageProvider-sample:

- GitHub-repository: <https://github.com/android/storage-samples>
- Esimerkkihakemisto: `StorageProvider/`
- Dokumentaatio sanoo suoraan, että sample näyttää yksinkertaisen `DocumentsProvider`-toteutuksen Storage Access Frameworkilla: <https://android.googlesource.com/platform/developers/samples/android/+/master/content/documentsUi/StorageProvider/README.md>
- Androidin SAF-dokumentaatio selittää, että `DocumentsProvider` on ContentProvider-aliluokka, jolla storage-palvelu näyttää hallitsemansa tiedostot käyttäjälle: <https://developer.android.com/guide/topics/providers/document-provider>

Huomio: `android/storage-samples` on arkistoitu eikä sitä ylläpidetä aktiivisesti, mutta tähän tarkoitukseen se on silti hyvä lähtöpiste, koska se sisältää valmiin pienen `DocumentsProvider`-rakenteen. Se on lähempänä NFS-tavoitetta kuin esimerkiksi Camera2Basic, koska Camera2Basic on kameran API-esimerkki eikä tiedostopalveluntarjoaja.

### 1. Lataa sample WSL Debianissa

Tee tämä esimerkiksi samaan `~/android`-hakemistoon, jossa sinulla on tämä repository:

```bash
cd ~/android
git clone https://github.com/android/storage-samples.git android-storage-samples
cd android-storage-samples/StorageProvider
```

Jos et halua kloonata koko storage-samples-repositoryä pysyvästi, voit poistaa sen myöhemmin ja säilyttää vain `StorageProvider`-hakemiston pohjana.

### 2. Avaa sample Android Studiossa tai tarkista Gradle-komentoriviltä

Helpoin tapa on avata hakemisto Android Studiossa:

```text
~/android/android-storage-samples/StorageProvider
```

Android Studio osaa yleensä päivittää vanhan sample-projektin Gradle/AGP-asetukset nykyiseen Android SDK -ympäristöösi. Tämä on parempi kuin kirjoittaa `settings.gradle`, `build.gradle`, wrapperit ja manifestit käsin.

Komentoriviltä voit ensin katsoa, mitä tehtäviä sample tarjoaa:

```bash
gradle tasks --all
```

Jos sample sisältää oman wrapperin ja haluat käyttää sitä paikallisesti, voit käyttää myös `./gradlew`-komentoa. Wrapperin `gradle-wrapper.jar` on binääritiedosto, joten sitä ei pidä yrittää lisätä tähän PR:ään, jos PR-järjestelmä sanoo `Binääritiedostoja ei tueta`.

### 3. Rakenna sample-APK

Kun Android Studio tai Gradle on päivittänyt projektin toimivaan tilaan, rakenna debug-APK:

```bash
gradle assembleDebug
```

Jos sample on monimoduulinen ja tehtävä näkyy moduulin alla, käytä tehtävälistassa näkyvää nimeä, esimerkiksi:

```bash
gradle :Application:assembleDebug
```

Etsi syntynyt APK:

```bash
find . -path '*/build/outputs/apk/debug/*.apk' -print
```

### 4. Kopioi APK Windowsin puolelle

Korvaa `<windows-kayttaja>` omalla Windows-käyttäjänimelläsi ja `<apk-polku>` edellisen `find`-komennon tuloksella:

```bash
cp <apk-polku> /mnt/c/Users/<windows-kayttaja>/Downloads/StorageProvider-debug.apk
```

### 5. Asenna APK puhelimeen Windows 10 CMD:stä

Kytke puhelimesta **Developer options** ja **USB debugging** päälle. Liitä puhelin USB-kaapelilla Windowsiin. Avaa Windowsissa **Command Prompt** eli `cmd.exe` ja tarkista yhteys:

```cmd
adb devices
```

Hyväksy tarvittaessa puhelimessa USB debugging -sormenjälki. Yhteys on valmis, kun `adb devices` näyttää laitteen tilassa `device`.

Asenna APK:

```cmd
adb install -r %USERPROFILE%\Downloads\StorageProvider-debug.apk
```

Jos asennus epäonnistuu allekirjoituksen vaihtumisen takia, poista vanha debug-versio ensin. Paketin nimen näkee samplen `AndroidManifest.xml`- tai `build.gradle`-tiedostosta.

### 6. Miten tästä tehdään andersNFSmount-pohja?

Kun StorageProvider-sample buildaa ja asentuu puhelimeen, seuraava työvaihe on muokata sitä eikä tätä JVM-proof-of-conceptia:

1. Vaihda samplen sovelluksen nimi ja package/applicationId muotoon `dev.andersnfs` tai muu valittu tunniste.
2. Etsi samplesta `DocumentsProvider`-luokka.
3. Korvaa samplen paikallinen fake-/testitiedostolista NFS-repository-rajapinnalla.
4. Siirrä tästä repositorystä hyödylliset NFS-luonnokset uuteen Android-projektiin pala kerrallaan.
5. Pidä ensimmäinen Android-versio lukutilaisena: listaa hakemistot ja avaa tiedostoja, mutta älä vielä tee kirjoitus-, rename- tai delete-operaatioita.

Tämän jälkeen APK:n puhelintestaus tehdään samasta sample-pohjasta komennolla `gradle assembleDebug`, APK kopioidaan Windowsiin ja asennetaan `adb install -r` -komennolla yllä olevan ohjeen mukaan.

## NFS-palvelimen valmistelu manuaalitestejä varten

Kun NFS-clientin varsinainen toteutus lisätään, puhelimen ja PC:n pitää päästä samaan NFS-palvelimeen verkon yli.

- Varmista, että puhelin ja NFS-palvelin ovat samassa Wi-Fi-verkossa tai VPN:ssä.
- Salli palomuurista NFS:n tarvitsemat portit. NFSv3 käyttää tyypillisesti porttia `2049` sekä mount/rpcbind-palveluita, ellei palvelinta ole lukittu kiinteisiin portteihin.
- Käytä testijaossa aluksi vain lukutilaisia oikeuksia ja testitiedostoja.
- Tarkista ensin PC:ltä, että NFS-jako toimii, ja siirry vasta sitten puhelintestiin.

Esimerkki PC:llä tehtävästä NFS-jaon tarkistuksesta:

```bash
showmount -e <nfs-palvelimen-ip>
```

Jos käytössä on Linux-host ja haluat varmistaa jaon sisällön kernel-mountilla, voit testata erillisessä testihakemistossa:

```bash
sudo mkdir -p /mnt/andersnfs-test
sudo mount -t nfs <nfs-palvelimen-ip>:/export/path /mnt/andersnfs-test
find /mnt/andersnfs-test -maxdepth 2 -type f -print
sudo umount /mnt/andersnfs-test
```

Android-sovelluksen lopullinen tavoite ei kuitenkaan ole tehdä tällaista kernel-mounttia puhelimessa, vaan näyttää NFS-sisältö user-space-toteutuksella Androidin tiedostorajapintojen kautta.

## Ensimmäinen toteutustavoite

Ensimmäinen varsinainen Android-toteutusvaihe tehdään StorageProvider-sample-pohjaan: lisää NFS-clientin rajapinnat, fake-toteutus ja lukutilainen tiedostoselain sample-projektin `DocumentsProvider`-toteutuksen taakse.
