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

## Kääntäminen

Tämä hakemisto sisältää tarkoituksella mahdollisimman pienen Gradle/Kotlin-pohjan. Se ei vielä ole Android-sovellus, vaan JVM:llä kääntyvä Kotlin-projekti, johon Android- ja SAF-kerrokset voidaan lisätä myöhemmin.

Käytä JDK 17:ää ja aja:

```bash
gradle build
gradle run
```

## Ensimmäinen toteutustavoite

Ensimmäinen varsinainen koodausvaihe on lisätä NFS-clientin Kotlin-rajapinnat, fake-toteutus ja lukutilainen tiedostoselain. Tämän jälkeen sama repository-kerros voidaan kytkeä `DocumentsProvider`-toteutukseen.
