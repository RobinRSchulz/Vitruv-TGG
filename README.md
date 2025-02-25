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
* patternmatching
  * PatternMatcher -> rememberInvokedPatternType -> man sollte nicht TggRules speichern, sondern TGGRules+die Stelle, an der der EChangeWrapper invoziert wurde. 
    --> damit reicht eigentlich der Parent-EChangeWrapper, weil der immer nur in einem Template vorkommt.
    --> macht das ganze auch intuitiver: hasBeenInvokedAsChildOf(eChange, eChangeWrapperParent)
    --> Parent/Child-Konstruktion dokumentieren!
* patternconversion
  * EChangeWrapper-Kinder
    * Javadoc-Duplikate raus, nur das reinschreiben, was mehr gemacht wird.
* ibex
  * DefaultRegistrationHelper:
    * hardgecodete Resource-URLs de-hardcoden ==> Util-Methode NSUriToPlatformUri(..) schreiben.
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
