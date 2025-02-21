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
  * EChangeWrappers in Subpaket echange, um protected-Methdoen unerreichbar zu machen
* patternmatching
  * PatternMatcher -> rememberInvokedPatternType -> man sollte nicht TggRules speichern, sondern TGGRules+die Stelle, an der der EChangeWrapper invoziert wurde. --> damit reicht eigentlich der Parent-EChangeWrapper, weil der immer nur in einem Template vorkommt.
    --> macht das ganze auch intuitiver: hasBeenInvokedAsChildOf(eChange, eChangeWrapperParent)
    --> Parent/Child-Konstruktion dokumentieren!
* patternconversion
  * Umbenennung: VitruviusChangeTemplateSet sollte eher ChangeSequenceTemplateSet heißen und IbexPatternTemplate sollte ChangeSequenceTemplate heißen
  * VitruviusChangeTemplateSet
    * auskommentierter Code raus
    * detaillierteres JavaDoc
    * getAndInitRelevantIbexPatternTemplatesByEChange
      * matches-Methode nutzen statt selber checken
      * Algorithmus detaillierter erklären (Kommentare)
  * IbexPatternConverter
    * auskommentiertes Zeug weitestgehend raus oder in extra-Methode
    * logging fixen
    * Algorithmus kommentieren
    * Kommentare überprüfen, todos raus etc
    * evtl umbenennen in IbexPatternToChangeSequenceTemplateConverter ?
  * EChangeWrapper
    * ganz viel Javadoc, an Methoden und an Felder.
    * "parent" --> eher was in richtung "Vorlage"
    * Javadocs überarbeiten
    * s
  * EChangeWrapper-Kinder
    * Javadoc-Duplikate raus, nur das reinschreiben, was mehr gemacht wird.
* ibex
  * DefaultRegistrationHelper:
    * Auskommentierter Code zurückdrängen
    * hardgecodete Resource-URLs de-hardcoden ==> Util-Methode NSUriToPlatformUri(..) schreiben.
    * BlackInterpreter (PatternMatcher) im Konstruktor mitgeben
  * SYNCDefault: BlackInterpreter einfach im zusätzlich angebotenen Konstruktor mit. (Default = VitruviusBackwardConversion...) So kann man in der CPS überschreiben, welchen PatternMatcher man will?
  * PfuschURLClassLoader: dokumentieren
* Aufschreiben, was noch alles zu tun ist!
* Alle TODOs überprüfen
* GGF Unit-Tests schreiben für
  * IbexPatternConverter
  * VitruviusChangePatternMatcher
  * dafür das Something2Else-Modell bisschen erweitern und einfach die Patterns ins repo reinhauen. Das benötigte Ibex-Gedöns möglichst kleinhalten, Vitruvius auch.
* Architektur-Review-Ergebnisse einarbeiten
* Ibex-Gedöns schön machen (SyncDefault und so...)
* Hier im Readme die Tabelle |EChange|Werte|EChangeWrapper| anlegen
* Readme strukturieren und schreiben
  * Kapitel: Vitruvius-Anbindung, IbeX-Anbindung (Pattern Retrieval), Pattern to template Conversion, Pattern Template Matching, Pattern Matching Ibex
  * 
