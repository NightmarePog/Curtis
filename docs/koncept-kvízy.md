# Koncept kvízy

## architektura
/quiz GET:
- Kvíz
  - Otázky
    - Možnosti jedna či vícero
  - omezený čas (časový údaj)
  - hash který bude ověřovat integritu odpovědi (aby se zamezilo, že si uživatel navyšší čas např.) 
- stav jestli je kvíz hotový (bool)
- ID kvízu

/quiz POST: 
  - název kvízu
  - kvízové otázky
    - Otázka
    - odpovědi
      - typ (volná, vícero, jedna)
      - odpověď (bude záležet na typu odpovědi)
    
    
/quiz/results GET
  - Otázka
    - Text
    - Odpovědi
    - ID správné odpovědi
