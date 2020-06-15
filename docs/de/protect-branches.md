---
title: Branches schützen
---
### Lokal
In den Repository-Einstellungen gibt es den Tab "Pull Requests". Dort kann ein Repository-bezogener Schutz für Branches aktiviert werden. Für den Schutzmechanismus können Branches mit Namen oder auch als Muster (z. B. "feature/*") eingetragen werden, für die der Schreibschutz gelten soll. Ist ein Branch geschützt, können Änderungen ausschließlich über Pull Requests auf diesen Branch committet werden. Mithilfe dieser Einschränkung kann ein Review-Workflow über Pull Requests erzwungen werden.

### Global
Es gibt zusätzlich die Möglichkeit einen Repository-übergreifenden Schreibschutz über die Administrations-Oberfläche zu definieren. Damit kann man beispielsweise festlegen, dass per Default in keinem Repository direkt auf den "master"-Branch gepusht werden darf, wenn keine spezifische Konfiguration für dieses Repository vorhanden ist.

Will man verhindern das Repository-Owner ihren eigenen Branch-Schutz definieren, kann über eine Checkbox der lokale Branch-Schutz für die SCM-Manager-Instanz deaktiviert werden.
