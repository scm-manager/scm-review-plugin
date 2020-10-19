---
title: Arbeiten mit Pull Requests
---
Für ein effizientes Arbeiten mit Pull Requests gibt es unterschiedliche Möglichkeiten Informationen zwischen dem Autor und dessen Reviewern auszutauschen.

### Reviewer hinzufügen
Der Autor kann beim Erstellen bzw. Editieren seines Pull Requests Wunsch-Reviewer eintragen. Diese Reviewer werden per E-Mail über das anstehende Review informiert und sind in den Pull Request Details aufgelistet. 

### Pull Request abonnieren
Oben rechts auf der Pull Request Detailseite kann ein Reviewer mit einem Klick auf den "+" (Plus) bzw. "-" (Minus) Button diesen Pull Request abonnieren / deabonnieren.  Der Abonnent wird über Änderungen an diesem Pull Request und neue Kommentare per E-Mail benachrichtigt.

### Pull Request freigeben
Nach einem Review kann der Reviewer den Pull Request für den Merge freigeben. Die Freigabe gilt vorerst nur als Informationen für den Autor über das durchgeführte Review. Eine tatsächliche Freigabe im Sinne eines Entsperrens findet noch nicht statt. Sobald ein Reviewer auf "Freigeben" klickt, wird hinter seinem Namen in der Reviewer-Liste ein grüner Haken hinterlegt.

### Diff erweitern
Innerhalb des Pull Request werden die Änderungen in verkürzten Diffs pro Datei angezeigt. 
Diese Diffs können auch schrittweise oder komplett nachgeladen werden, um den gesamten Inhalt der Datei zu sehen.

![Pull Request - Diff expand](assets/diffExpand.png)

### Kommentare erstellen
Für einen Pull Request können drei Arten von Kommentaren erstellt werden. 

- **Allgemeiner Kommentar:** Bezieht sich auf den Pull Request als Ganzes. Kann im Kommentar-Tab über den Kommentar-Editor erstellt werden.
- **Datei-Kommentar:** Kann im Diff-Tab in der Kopfzeile einer Datei über "Kommentar hinzufügen" erstellt werden.
- **Zeilen-Kommentar:** Kann über einen Klick auf eine Zeile im Diff erstellt werden.

Alle Kommentare findet man in dem Kommentar-Tab. Die Datei- und Zeilen-Kommentare findet man auch direkt an der Datei in dem Diff-Tab. 
Es ist möglich innerhalb von Kommentaren andere Benutzer des SCM-Managers zu erwähnen. Dafür beginnt man mit einem "@"-Zeichen und tippt danach den Namen, Benutzername oder die E-Mail-Adresse des Benutzers. 
Ab dem dritten Zeichen werden über eine Autovervollständigung passende Benutzer vorgeschlagen. Erwähnte Benutzer werden per E-Mail benachrichtigt, dass sie in dem Kommentar erwähnt wurden. 

![Pull Request - Create comment](assets/createComment.png)

Erstellte Kommentare bieten folgende Interaktions-Möglichkeiten:

|Icon|Bedeutung|
|---|--------------------------------------------|
|![Loeschen](assets/icon-delete.png)|Kommentar löschen|
|![Bearbeiten](assets/icon-edit.png)|Kommentar bearbeiten|
|![Task](assets/icon-make-task.png)|Kommentar in Aufgabe umwandeln|
|![Kommentar](assets/icon-make-comment.png)|Aufgabe in Kommentar umwandeln|
|![Reply](assets/icon-reply.png)|Auf Kommentar bzw. Aufgabe antworten|
|![Done](assets/icon-done.png)|Aufgabe als erledigt markieren|
|![Wieder eröffnen](assets/icon-reopen.png)|Aufgabe als _nicht_ erledigt markieren|

### Veraltete Kommentare
Sobald ein Pull Request neue Commits erhält, werden die Kommentare teilweise als veraltet markiert. Allgemeine Kommentare gelten nach jedem neuen Commit als veraltet und Datei- bzw. Zeilen-Kommentare nach einem Commit, der die entsprechende Datei ändert.

![Pull Request - Outdated comment](assets/outdatedComment.png)

Diese Kommentare werden mit einem "Veraltet"-Tag markiert. Mit einem Klick auf einen blauen Veraltet-Tag bzw. einen blauen Dateinamen-Tag, öffnet sich der ursprüngliche Kontext, zu dem dieser Kommentar erstellt wurde. Damit lässt sich auch nach neuen Änderungen nachvollziehen, warum dieser Kommentar erstellt worden war.

![Pull Request - Outdated context](assets/outdatedContext.png)

### Gleichzeitige Bearbeitung
Da mehrere Benutzer zeitgleich einen Pull Request reviewen oder bearbeiten können, ist es möglich neue Änderungen nicht mitzubekommen. 
Daher werden alle Bearbeiter, die einen Pull Request geöffnet haben, über Änderungen an diesem Pull Request mit einer Meldung informiert.

![Pull Request - Toastmessage](assets/toastmessage.png)

### Aufgaben erstellen
Sollte ein Reviewer Fehler finden oder Änderungen am Pull Request vorschlagen wollen, kann er dies über Aufgaben realisieren. Der Kommentar-Editor kann über einen Radio-Button zu einem Aufgaben-Editor umgeschaltet werden. Aufgaben werden in den Pull Request Details und in der Übersichtstabelle über einen Tag angezeigt, beispielsweise "2 / 5 Aufgaben erledigt". 

Offene Aufgaben können über ein Icon auf "abgeschlossen" gesetzt werden. Aufgaben sind derzeit noch informativ und keine zwingende Voraussetzung zum Mergen des Pull Requests.

![Pull Request - Create task](assets/createTask.png)

### Pull Request ablehnen
Sollte ein Pull Request aus Sicht der Reviewer nicht gemerged werden dürfen, kann dieser Pull Request abgelehnt werden. Dies sollte in Abstimmung mit dem Pull Request Autor passieren. Ein abgelehnter Pull Request kann nicht erneut geöffnet werden. Es muss stattdessen ein neuer Pull Request erstellt werden.

### Pull Request mergen

Sollte ein Pull Request mergebar sein, können die Änderungen über den Button "Pull Request mergen" auf verschiedene Arten angewendet werden:

- **Merge Commit:** Die Branches werden mit einem neuen Commit gemerged.
- **Fast-Forward, wenn möglich:** Falls möglich wird der Target Branch ohne Merge auf den Stand des Source Branches gesetzt. Falls dies nicht möglich ist, wird ein normaler Merge Commit erzeugt.
- **Squash:** Alle Änderungen des Source Branches werden als ein neuer Commit auf dem Target Branch zusammengefasst. Die Commit-Historie des Source Branches wird dabei nicht in den Target Branch übertragen.
- **Rebase:** Alle Änderungen des Source Branches werden einzeln auf den Target Branch angewendet. Es wird kein Commit für den Merge erstellt. 

Für den Merge kann statt der vorgegebenen Muster-Nachricht eine eigene Commit-Nachricht eingetragen werden. Über die Checkbox "Branch löschen" wird der Source-Branch nach dem erfolgreichen Merge aus dem Repository entfernt.

Im Falle eines Merge-Konflikts kann der Pull Request nicht automatisch gemerged werden. Die Konflikte müssen vor dem Merge manuell aufgelöst werden.

In dem Merge-Commit werden die zustimmenden Reviewer als "Reviewed-by" gesetzt.

![Pull Request - Merge-Modal](assets/mergeModal.png)
