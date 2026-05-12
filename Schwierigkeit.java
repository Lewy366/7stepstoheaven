public enum Schwierigkeit {

    LEICHT("Leicht",  1, "Entspannt spielen",       new int[]{3, 1, 5}),
    MITTEL("Mittel",  2, "Normale Herausforderung",  new int[]{5, 2, 3}),
    SCHWER("Schwer",  3, "Nur für Profis",           new int[]{8, 3, 1});

    // Felder
    private final String anzeigeName;    // Name für die Oberfläche
    private final int    stufe;          // 1 = leicht, 2 = mittel, 3 = schwer
    private final String beschreibung;  // Kurzbeschreibung
    private final int    leben;         // Anzahl der Startleben
    private final int    geschwindigkeit; // Spielgeschwindigkeit (z. B. für Gegner-KI)
    private final int    zeitBonus;     // Bonus-Sekunden bei Levelabschluss

    // Konstruktor
    Schwierigkeit(String anzeigeName, int stufe, String beschreibung, int[] werte) {
        this.anzeigeName     = anzeigeName;
        this.stufe           = stufe;
        this.beschreibung    = beschreibung;
        this.leben           = werte[0];
        this.geschwindigkeit = werte[1];
        this.zeitBonus       = werte[2];
    }

    // Getter
    public String getAnzeigeName()      { return anzeigeName;     }
    public int    getStufe()            { return stufe;           }
    public String getBeschreibung()     { return beschreibung;    }
    public int    getLeben()            { return leben;           }
    public int    getGeschwindigkeit()  { return geschwindigkeit; }
    public int    getZeitBonus()        { return zeitBonus;       }

    /** Sterne-String passend zur Stufe, z. B. "★★★" für MITTEL */
    public String getSterne() {
        return "★".repeat(stufe) + "☆".repeat(3 - stufe);
    }

    @Override
    public String toString() {
        return String.format("[%s] Stufe %d | Leben: %d | Geschwindigkeit: %d | Zeitbonus: %ds | %s",
                anzeigeName, stufe, leben, geschwindigkeit, zeitBonus, beschreibung);
    }
}
