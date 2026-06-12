# andersNFSmount

Android/Kotlin-projektin suunnittelupohja user-space NFS -ominaisuudelle.

## Suunnittelun lähtökohta

Tavoitteena on tuoda NFS-palvelimen tiedostot Androidissa käyttäjälle näkyviin ilman root-oikeuksia tai kernel-tason `mount`-operaatiota. Ensisijainen toteutustapa on Androidin Storage Access Frameworkiin perustuva `DocumentsProvider`, joka näyttää NFS-jaon tiedostovalitsimille ja SAF-yhteensopiville sovelluksille.

Katso tarkempi arkkitehtuuri- ja vaiheistussuunnitelma dokumentista [`docs/android-userspace-nfs-plan.md`](docs/android-userspace-nfs-plan.md).

## Kehitysympäristön oletus

- Gradle 8.7
- Kotlin Gradlen mukana tulevalla compilerillä
- JVM 17
- Android-projekti, johon NFS-ominaisuus lisätään Kotlinilla

## Projektin nykytila

Tämä hakemisto sisältää tarkoituksella mahdollisimman pienen Gradle/Kotlin-pohjan. Se ei vielä ole valmis Android-sovellus eikä siitä synny APK-pakettia. Nykyinen testi kääntää Kotlin-koodin JVM:lle ja ajaa komentoriviltä `main`-funktion.

`MainActivity` ja Android-yhteensopivuusluokat ovat tässä vaiheessa kokeilupohjia, jotta NFS-kerrosta voidaan hahmotella ennen varsinaista Android-projektiksi muuttamista.

## Testaus PC-hostissa

Näillä ohjeilla voit testata nykyisen version Linux-, macOS- tai Windows-hostissa.

### 1. Tarkista työkalut

Asenna JDK 17 ja Gradle. Tarkista versiot projektin juuressa:

```bash
java -version
gradle --version
```

`java -version`-komennon pitäisi näyttää JDK 17. Gradlen pitäisi olla 8.x-sarjaa; projektin oletus on Gradle 8.7.

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

## Testaus kännykässä

Nykyinen repository ei vielä rakenna asennettavaa Android APK:ta. Kännykässä testaus onnistuu tässä vaiheessa helpoiten Termuxissa ajettavana JVM/Kotlin-komentorivitestinä. Tämä testaa saman käännöksen ja `main`-funktion kuin PC-hostissa.

### Vaihtoehto A: testaa Android-puhelimessa Termuxilla

1. Asenna Termux esimerkiksi F-Droidista.
2. Avaa Termux ja asenna perustyökalut:

   ```bash
   pkg update
   pkg install git openjdk-17 gradle
   ```

3. Kloonaa projekti puhelimeen:

   ```bash
   git clone <repository-url>
   cd andersNFSmount
   ```

   Jos repository on jo kopioitu puhelimeen muulla tavalla, siirry suoraan projektihakemistoon.

4. Käännä ja aja testi:

   ```bash
   gradle clean build
   gradle run
   ```

5. Onnistunut ajo tulostaa:

   ```text
   andersNFSmount Kotlin project is ready.
   ```

Huomio: tämä ei vielä näytä NFS-jakoa Androidin tiedostosovelluksessa, koska `DocumentsProvider`- ja APK-toteutus puuttuvat vielä.

### Vaihtoehto B: myöhempi APK-testaus Android-laitteella

Kun projekti on muutettu varsinaiseksi Android-sovellukseksi, testaus tehdään näin:

1. Kytke puhelimesta **Developer options** ja **USB debugging** päälle.
2. Liitä puhelin PC:hen USB-kaapelilla.
3. Tarkista yhteys:

   ```bash
   adb devices
   ```

4. Rakenna ja asenna debug-versio Android Gradle Plugin -projektissa:

   ```bash
   ./gradlew installDebug
   ```

5. Avaa sovellus puhelimessa ja testaa NFS-yhteys samassa Wi-Fi- tai VPN-verkossa olevaan NFS-palvelimeen.

Nämä APK-vaiheet ovat tulevaa Android-projektimuotoa varten. Nykyisessä minimipohjassa oikea testikomento on edelleen `gradle run`.

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

Ensimmäinen varsinainen koodausvaihe on lisätä NFS-clientin Kotlin-rajapinnat, fake-toteutus ja lukutilainen tiedostoselain. Tämän jälkeen sama repository-kerros voidaan kytkeä `DocumentsProvider`-toteutukseen.
