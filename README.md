# Vitruv-TGG
TGG Support for Vitruvius using eMoflon::IbeX

Since this depends on eMoflon, we need at least a Java-21-SDK

## Documentation
This project enables the usage of Tripe Graph Grammar (TGG) rules to define consistency preservation rules (CPRs) in the Vitruvius framework.
CPRs define how consistency is preserved between two models.
For the definition and to support the propagation of those rules (patterns in TGGs), the eMoflon::IbeX framework is used.
In the following, the process of getting sequences of changes to a source model, given by Vitruvius,
and using IbeX and a pattern matching process, which this project mainly consists of, to propagate those changes to a target model, is shown.

### Rule definition
The rules are defined by the methodologist in an Eclipse eMoflon::IbeX project.
Further information about that can be found here (TODO).

### Vitruvius connection
To retrieve change sequences from Vitruvius, the TGGChangePropagationSpecification is used.
A methodologist extends the class and provides information specific to the consistency relation between two (meta-) models.
This extension is registered in Vitruvius and the respective methods called whenever Vitruvius registers a change to the source model.
(Probably two such classes are necessary, one for each direction --> todo..)

The ibex.VitruviusTGGChangePropagationIbexEntrypoint is called by the TGGChangePropagationSpecification, with information about 
  * source and target metamodels and models
  * The Eclipse Project where the methodologist has defined the TGG rule

* Kapitel: Vitruvius-Anbindung, IbeX-Anbindung (Pattern Retrieval), Pattern to template Conversion, Pattern Template Matching, Pattern Matching Ibex

## todo 4 code review
* Urls in der Documentation ergänzen!
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
