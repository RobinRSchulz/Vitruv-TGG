# Vitruv-TGG
TGG Support for Vitruvius using eMoflon::IbeX

Since this depends on eMoflon, we need at least a Java-21-SDK

## Install on windows

* Not use powershell. In IntelliJ: "Command Prompt"
  OBACHT!In IntelliJ, that has to be re-opened each time the IDE is restarted (otherwise, it changes to powershell...)
* Change your shell's JAVA_HOME to a Java-23 JDK path:
    ```
    set JAVA_HOME="C:\Users\XPS-15\.jdks\openjdk-23.0.1"
    ```
* Install
    ```
    mvnw clean install
    ```
## todo 4 code review
* TGGChangePropagationSpecification
  * getAffectedElement --> Util.getAffectedElement
  * findModel aufräumen
  * propagateNonAtomicChange aufräumen
  * logging reduzieren
* VitruviusTGGResourceHandler
  * Kommentare reduzieren
* Paket-Restrukturierung
  * ibex enthält nur die Klassen, die von Ibex-Klassen erben, die anderen Pakete werden hochgezogen.
  * Util kommt ins root-Paket und wird zu einer Klasse.
* patternconversion
  * Umbenennung: VitruviusChangeTemplateSet sollte eher ChangeSequenceTemplateSet heißen und IbexPatternTemplate sollte ChangeSequenceTemplate heißen
  * VitruviusChangeTemplateSet
    * auskommentierter Code raus
    * detaillierteres JavaDoc
    * getAndInitRelevantIbexPatternTemplatesByEChange
      * matches-Methode nutzen statt selber checken
      * Algorithmus detaillierter erklären (Kommentare)
* Hier im Readme die Tabelle |EChange|Werte|EChangeWrapper| anlegen
* Readme strukturieren und schreiben
  * Kapitel: Vitruvius-Anbindung, IbeX-Anbindung (Pattern Retrieval), Pattern to template Conversion, Pattern Template Matching, Pattern Matching Ibex
  * 
