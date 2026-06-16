/*
 * Main.java - Einstiegspunkt des Spiels
 * 
 * Diese Klasse ist der Startpunkt der Anwendung und muss zuerst ausgeführt werden.
 * Sie initialisiert die GameUI (das Hauptmenü mit Schwierigkeitsauswahl) auf dem Event Dispatch Thread.
 * 
 * Ablauf:
 * 1. Main.java wird ausgeführt und startet die GameUI
 * 2. Der Spieler sieht das Hauptmenü und wählt einen Schwierigkeitsgrad
 * 3. Nachdem der Spieler auf "Start Game" klickt, wird MainGame.java gestartet
 * 4. MainGame.java lädt das eigentliche Spiel mit dem Generator
 */

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameUI::new);
    }
}

//