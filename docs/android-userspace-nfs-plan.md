# Android user-space NFS mount - suunnitteludokumentti

Tämä dokumentti kuvaa ensimmäisen toteutussuunnitelman Android-sovellukselle, joka tuo NFS-palvelimen tiedostot käyttäjälle näkyviin ilman kernel-tason `mount`-operaatiota. Lähtökohtainen kehitysympäristö on Gradle 8.7, Kotlin 1.9.22, JVM 17 ja Android-projekti, johon ominaisuus lisätään Kotlinilla.

## Tavoite ja rajaus

Tavoitteena on tarjota Androidissa käyttäjälle selattava ja käytettävä näkymä NFS-palvelimen tiedostoihin. Koska tavallinen Android-sovellus ei voi tehdä oikeaa NFS-kernel-mounttia ilman root-oikeuksia tai järjestelmäallekirjoitusta, ominaisuus toteutetaan user-space-kerroksena.

Ensimmäisessä vaiheessa tiedostot tuodaan näkyviin Androidin Storage Access Frameworkin kautta `DocumentsProvider`-toteutuksella. Tämä tekee NFS-jaosta valittavan sijainnin Androidin tiedostovalitsimissa ja niissä sovelluksissa, jotka käyttävät SAF-rajapintoja. Sovelluksen sisällä voidaan lisäksi tarjota oma tiedostoselain ja kopiointi-/avaustoiminnot.

## Androidin realiteetit

- Oikea mount-polku, esimerkiksi `/mnt/nfs/share`, ei ole normaalille sovellukselle realistinen tavoite ilman rootia, privileged app -asemaa tai laitevalmistajan tukea.
- Käytännöllinen user-space-vaihtoehto on virtuaalinen tiedostojärjestelmä `DocumentsProvider`-rajapinnan kautta.
- `DocumentsProvider` ei tee tiedostoista universaalisti näkyviä POSIX-polkuina, vaan tarjoaa dokumentti-URI:t ja stream-rajapinnat.
- Pitkät NFS-operaatiot on ajettava taustalla esimerkiksi foreground service -mallilla, jotta Androidin prosessinhallinta ei katkaise aktiivista siirtoa.
- Offline- ja verkkovirhetilanteet täytyy mallintaa eksplisiittisesti, koska NFS-palvelin voi kadota kesken hakemistolistauksen tai streamin.

## Ehdotettu arkkitehtuuri

```text
Android UI / SAF clients
        |
        v
DocumentsProvider + oma tiedostoselain
        |
        v
NfsRepository: hakemistot, metadata, streamit, virheet
        |
        v
NfsClient abstraction
        |-----------------------------|
        v                             v
Kotlin NFSv3 client              JNI/libnfs adapter
        |                             |
        v                             v
TCP/UDP RPC, XDR, NFS protocol   native libnfs
```

### Kerrokset

1. **UI-kerros**
   - Jakolistat, yhteysprofiilit, kirjautumattoman NFS-jaon asetukset ja virheilmoitukset.
   - Oma selain on hyödyllinen testaukseen, vaikka lopullinen integraatio tehdään SAF:n kautta.

2. **DocumentsProvider-kerros**
   - Toteuttaa Androidin dokumenttipuun: juuret, hakemistot, tiedostot, MIME-tyypit, metadata ja streamien avaus.
   - Muuntaa NFS-polut vakaiksi document ID -tunnisteiksi.
   - Vastaa siitä, että SAF-kutsut eivät tee verkko-operaatioita pääsäikeessä.

3. **Repository-kerros**
   - Tarjoaa coroutine-pohjaisen API:n hakemistolistaukseen, stat-kyselyihin, lukuun, kirjoitukseen, poistoon ja uudelleennimeämiseen.
   - Sisältää välimuistin, retry-politiikan, aikakatkaisut ja virheiden normalisoinnin.

4. **NFS-client-kerros**
   - Ensimmäinen vaihtoehto: NFSv3-client Kotlinilla, jolloin build ja jakelu pysyvät yksinkertaisina.
   - Toinen vaihtoehto: JNI-silta `libnfs`-kirjastoon, jos tarvitaan nopeammin laajempi protokollatuki tai suorituskyky.

## Protokollavalinta

Suositeltu MVP on NFSv3 TCP:n yli.

Perustelut:

- NFSv3 on tilattomampi ja yksinkertaisempi kuin NFSv4.
- Android-sovelluksen käyttäjäoikeuksilla on helpompi hallita sovellustason TCP-yhteyksiä kuin yrittää integroitua kernelin mount-malliin.
- V3:n RPC/XDR-rakenne on toteutettavissa hallitusti Kotlinissa, jos rajataan ensin vain perusoperaatiot.

MVP:ssä tarvittavat operaatiot:

- `NULL` yhteystestiin.
- `GETATTR` metadatan hakemiseen.
- `LOOKUP` polun komponenttien resolvointiin.
- `READDIRPLUS` hakemistolistaukseen ja metadatan yhdistämiseen.
- `READ` tiedoston lukemiseen.
- `WRITE`, `CREATE`, `REMOVE`, `RENAME` myöhemmässä kirjoitustuen vaiheessa.

## MVP-vaiheet

### Vaihe 1: lukutilainen proof of concept

- Lisää yhteysprofiilin malli: host, export path, optional root path, transport ja timeoutit.
- Toteuta NFS-clientin rajapinta ja fake-client instrumentoituja testejä varten.
- Toteuta hakemistolistaus ja tiedoston luku yhdelle NFSv3 TCP -palvelimelle.
- Lisää yksinkertainen sovelluksen sisäinen tiedostoselain.

### Vaihe 2: SAF-integraatio

- Lisää `DocumentsProvider`, joka näyttää yhden tai useamman NFS-profiilin juurina.
- Toteuta `queryRoots`, `queryChildDocuments`, `queryDocument`, `openDocument` ja MIME-tyypitys.
- Lisää cancellation- ja timeout-käsittely, jotta tiedostovalitsimen UI ei jää odottamaan pysyvästi.

### Vaihe 3: kirjoitustuki

- Lisää tiedoston luonti, päällekirjoitus, poisto ja uudelleennimeäminen.
- Lisää varmistukset tilanteisiin, joissa NFS-palvelimen oikeudet tai UID/GID-mäppäys estävät operaation.
- Lisää konfliktien käsittely ja käyttäjälle näkyvät virheet.

### Vaihe 4: tuotantokovennus

- Välimuistin invalidointi ja hakemistojen refresh.
- Foreground service pitkille siirroille.
- Suurempien tiedostojen streaming ilman koko tiedoston muistissa pitämistä.
- Observability: lokitus, metriikat ja käyttäjän diagnostiikkaraportti.
- Verkkovaihdosten käsittely `ConnectivityManager`-rajapinnalla.

## Kotlin-rajapintaluonnos

```kotlin
data class NfsProfile(
    val id: String,
    val host: String,
    val exportPath: String,
    val rootPath: String = "/",
    val port: Int = 2049,
    val readOnly: Boolean = true,
    val timeoutMillis: Long = 15_000,
)

data class NfsEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long?,
    val modifiedAtMillis: Long?,
    val mimeType: String?,
)

interface NfsClient {
    suspend fun connect(profile: NfsProfile): NfsSession
}

interface NfsSession : AutoCloseable {
    suspend fun list(path: String): List<NfsEntry>
    suspend fun stat(path: String): NfsEntry
    suspend fun openRead(path: String, offset: Long = 0): NfsReadStream
}

interface NfsReadStream : AutoCloseable {
    suspend fun read(buffer: ByteArray, offset: Int, length: Int): Int
}
```

## Testausstrategia

- Unit-testit XDR-enkoodaukselle ja -dekoodaukselle tunnetuilla testivektoreilla.
- Repository-testit fake-clientillä ilman verkkoa.
- Instrumentoidut testit `DocumentsProvider`-rajapinnalle.
- Integraatiotestit Dockerissa ajettavaa NFS-palvelinta vasten CI-ympäristössä, jos runner tukee privileged-kontteja tai NFS-palvelua.
- Manuaalitestit Android-laitteella: tiedostovalitsin, kuvien avaaminen, suurten tiedostojen lukeminen, verkon katkaisu kesken siirron.

## Riskit ja avoimet kysymykset

- **POSIX-polkuodotus:** jos jokin kohdesovellus vaatii oikean tiedostopolun eikä hyväksy `content://`-URI:a, SAF ei riitä.
- **NFS-autentikointi:** MVP kannattaa rajata AUTH_SYS/NFSv3-ympäristöön; Kerberos/NFSv4 lisää merkittävästi kompleksisuutta.
- **Suorituskyky:** Kotlin-toteutuksen XDR/RPC voi olla riittävä MVP:hen, mutta suuret siirrot voivat hyötyä JNI/libnfs-polusta.
- **Android-taustarajoitukset:** pitkäkestoiset siirrot tarvitsevat foreground service -mallin ja selkeän ilmoituksen käyttäjälle.
- **Tietoturva:** profiilien tallennus, verkon salaamattomuus ja käyttäjän luottamus NFS-palvelimeen täytyy dokumentoida.

## Seuraava konkreettinen tehtävä

Seuraavaksi kannattaa lisätä Android-projektiin rajapinnat `NfsClient`, `NfsSession` ja `NfsRepository` sekä fake-toteutus, jolla voidaan rakentaa tiedostoselain ja `DocumentsProvider` ilman vielä valmista NFS-protokollapinoa.
