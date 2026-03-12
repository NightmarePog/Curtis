# Projektová dokumentace: Aplikace na kvízy – Specifikace MVP

**Cíl projektu:** Vytvořit efektivní alternativu k řešení MS Teams Kvízy s podporou plně automatického vyhodnocování.

## 1. Technický stack

* **Autentizace a autorizace:** Školní Microsoft účty (Microsoft Entra ID / SSO). Aplikace neukládá ani nespravuje uživatelská hesla.
* **Backend:** Java Spring, relační databáze (PostgreSQL nebo MariaDB).
* **Frontend:** React (detaily knihoven se určí později).

## 2. Uživatelské role

* **Žák:** Disponuje oprávněním vyplňovat přiřazené kvízy a prohlížet si vlastní výsledky.
* **Vyučující (Administrátor):** Disponuje oprávněním vytvářet a upravovat kvízy, spravovat otázky a nahlížet do výsledků žáků.

## 3. Funkční požadavky pro MVP

**Modul Vyučující:**
* Vytvoření nového kvízu s definicí názvu a časového okna platnosti (od-do).
* Přidávání uzavřených otázek do kvízu (výběr z možností, podpora jedné nebo více správných odpovědí).

**Modul Žák:**
* Zobrazení seznamu aktuálně dostupných kvízů k vyplnění.
* Zadání a odeslání odpovědí.

**Systém:**
* Automatické vyhodnocení úspěšnosti bezprostředně po odeslání odpovědí žákem.

## 4. Návrh datového modelu (Základní entity)

Výchozí struktura pro implementaci REST API a relační databáze:

* `User`: Ukládá se pouze unikátní identifikátor z Entra ID, zobrazované jméno a přiřazená role.
* `Quiz`: Atributy: Název, Popis, Datum_Od, Datum_Do.
* `Question`: Atributy: Text otázky, Vazba na entitu Quiz.
* `Option`: Atributy: Text možnosti, Vazba na entitu Question, Je_Spravna (Boolean).
* `Student_Attempt`: Atributy: Vazba na User, Vazba na Quiz, Celkové skóre.

## 5. Otevřené body a blokery

* **Správa skupin/tříd:** Čeká se na technické vyjádření IT oddělení ohledně struktury a synchronizace skupin v rámci tenantu Entra ID. Na základě tohoto zjištění se rozhodne, zda bude aplikace využívat MS Graph API pro načítání existujících tříd, nebo zda bude implementován vlastní modul pro manuální tvorbu skupin. Do finálního rozhodnutí je vývoj API pro správu skupin pozastaven.
