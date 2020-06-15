---
title: Berechtigungen
---
Das Review Plugin bringt folgende Berechtigungen mit:

* Globale Berechtigungen:
    * Alle Pull Requests lesen: Darf alle Pull Requests aus allen Repositories lesen
    * Alle Pull Requests bearbeiten: Darf Pull Requests für alle Repositories erstellen, kommentieren und ablehnen
    * Alle Pull Requests konfigurieren: Darf Pull Requests in allen Repositories konfigurieren
    * Emergency-Merge durchführen: Darf einen Emergency-Merge bei einem Pull Request durchführen und damit die Workflow Regeln ignorieren
    * Workflow Engine konfigurieren: Darf die globale Workflow Engine für alle Repositorys konfigurieren
* Repository-spezifische Berechtigungen:
    * Pull Requests erstellen: Darf neue Pull Requests erstellen
    * Pull Requests lesen: Darf Pull Requests lesen
    * Pull Requests kommentieren: Darf Pull Requests kommentieren
    * Pull Requests bearbeiten: Darf den Titel, Beschreibung und Reviewer von Pull Requests sowie Kommentare von anderen Benutzern bearbeiten
    * Pull Requests mergen/ablehnen: Darf Pull Requests akzeptieren (mergen) oder ablehnen (für den Merge wird zusätzlich die Push-Berechtigung benötigt)
    * Pull Requests konfigurieren: Darf Pull Requests konfigurieren
    * Workflow Engine Konfiguration lesen: Darf die Workflow Engine Konfiguration lesen
    * Workflow Engine Konfiguration ändern: Darf die Workflow Engine Konfiguration bearbeiten
    * Emergency-Merge durchführen: Darf einen Pull Request auch dann mergen, wenn nicht alle in der Workflow-Engine konfigurierten Regeln erfüllt sind

Benutzer mit der Rolle READ können automatisch Pull Requests dieses Repositories lesen. Mit der Rolle WRITE kommen zusätzlich die Berechtigungen zum Erstellen, Kommentieren und Akzeptieren bzw. Ablehnen hinzu.
