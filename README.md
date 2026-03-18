# Kodera

Kodera on Spring Boot REST -sovellus, jossa:
- opettaja luo tehtävän
- opiskelija palauttaa zip-tiedoston
- sovellus tekee arvioinnin ja tallentaa pisteet

Projektissa on mukana:
- Spring Boot + tietokanta
- profiilit dev/test/prod
- automaattiset testit
- GitHub Actions CI/CD
- Docker + Docker Compose

# Nopea kaynnistys paikallisesti

1. Liitä .env tiedosto projektin juureen!
2. Avaa Docker Desktop.
3. Käynnistä:

docker compose up --build -d


4. Avaa selain:

- sovellus: http://localhost:8080/

- tietokannan selainta mahdollista tutkia: http://localhost:8081/

- Health check: http://localhost:8080/api/assignments/health

## Endpointit

- `POST /api/assignments` luo tehtävän
- `POST /api/submissions` lähettää zip-palautuksen
- `POST /api/grades/{submissionId}` käynnistää arvioinnin
- `GET /api/grades/{submissionId}` hakee arvion

Lisäksi käytossä:
- `GET /api/assignments/health`
- `GET /api/assignments`
- `GET /api/submissions`

## Profiilit yksinkertaisesti

Profiili tarkoittaa eri asetuksia eri tilanteisiin.

- dev = paikallinen kehitys Dockerilla
- test = testien ajo (CI:ssa aina tama)
- prod = tuotanto / palvelinymparisto

Profiilin voi valita ymparistomuuttujalla:

SPRING_PROFILES_ACTIVE=dev
SPRING_PROFILES_ACTIVE=test
SPRING_PROFILES_ACTIVE=prod


Tässä projektissa:
- Docker local käyttaa normaalisti dev
- GitHub Actions testit ajaa test-profiililla
- tuotantoon tarkoitettu compose kayttaa prod

## Testit ja testiprofiili

Testit ajetaan GitHub Actionsissa automaattisesti.

Workflowt:
- .github/workflows/test.yml
- .github/workflows/ci-cd.yml

Molemmissa on `SPRING_PROFILES_ACTIVE: test`, eli testit eivat aja vahingossa dev/prod-asetuksilla.


## Datavirta alusta loppuun:

1. Opettaja luo tehtävän käyttöliittymässä
Selain lähettää pyynnön tehtävä-APIin.
Controller tallentaa tehtävän tietokantaan assignments-tauluun.

2. Opiskelija valitsee tehtävän ja lähettää zipin
Selain lähettää tiedoston ja tehtävän id:n submissions-APIin.
Controller ja service tallentavat zipin submissions-tauluun.

3. Arviointi käynnistetään submission id:llä
Selain kutsuu grades-APIa.
GradeService hakee palautuksen + tehtävänannon tietokannasta.

4. GradeService purkaa zipin ja muodostaa arviointipyynnön
Koodi luetaan zipistä, vaatimukset otetaan tehtävänannosta, ja pyyntö lähetetään GeminiClientin kautta.

5. Gemini palauttaa analyysin
GradeService tekee pisteytyksen ja normalisoi palautteen.

6. Tulos tallennetaan grades-tauluun
Pisteet + palaute + aikaleima tallennetaan, ja vastaus palautetaan käyttöliittymälle.

7. Käyttöliittymä näyttää lopputuloksen
Opiskelija/opettaja näkee arvosanan ja palautteen.

## Sammutus

docker compose down



