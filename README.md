# Vitruv-TGG
TGG Support for Vitruvius using eMoflon::IbeX

Since this depends on eMoflon, we need at least a Java-21-SDK

## Documentation
This project enables the usage of Tripe Graph Grammar (TGG) rules to define consistency preservation rules (CPRs) in the [Vitruvius](https://vitruv.tools/) framework.
CPRs define how consistency is preserved between two models.
For the definition and to support the propagation of those rules (patterns in TGGs), the [eMoflon::IBeX](https://github.com/eMoflon/emoflon-ibex/) framework is used.
In the [proposal document](doc/Proposal.pdf), the concept behind this project and the concepts this project touches are described in more detail.
In the following, the process of getting sequences of changes to a source model, given by Vitruvius,
and using IbeX and a pattern matching process, which this project mainly consists of, to propagate those changes to a target model, is shown.

### Rule definition
The rules are defined by the methodologist in an Eclipse eMoflon::IBeX project.
Further information about the creation of a project and the TGG rule specification with eMoflon::IBeX  can be found 
[here](https://github.com/eMoflon/emoflon-ibex-tutorial/releases/latest).
After specifying the rules, the methodologist has to clean, build and "incrementally build" the project.
Only after doing so, the project structure is in a state such that it is useable for Change Propagation with this Vitruvius integration.
The methodologist extracts certain information from the project (eclipse-specific namespace URIs for the models, File paths) 
and uses them in the definition of the 
[TGGChangePropagationSpecification](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/TGGChangePropagationSpecification.java),
as described in the following subsection.

### Vitruvius connection
To retrieve change sequences from Vitruvius, the 
[TGGChangePropagationSpecification](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/TGGChangePropagationSpecification.java) is used.
A methodologist extends the class and provides information specific to the consistency relation between two (meta-) models.
This extension is registered in Vitruvius and the respective methods called whenever Vitruvius registers a change to the source model.
(Probably two such classes are necessary, one for each direction --> todo..)

[VitruviusTGGChangePropagationIbexEntrypoint](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/ibex/VitruviusTGGChangePropagationIbexEntrypoint.java)::propagateChanges() 
is called by the TGGChangePropagationSpecification, with information about 
  * source and target metamodels and models
  * The Eclipse Project where the methodologist has defined the TGG ruleset
  * the Pattern Matcher to be used (we want to use the
[VitruviusBackwardConversionTGGEngine](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/ibex/VitruviusBackwardConversionTGGEngine.java))


### Ibex connection and pattern matching
The ibex package consists of several classes extending and/or implementing ibex classes/ interfaces.
As described before, 
[VitruviusTGGChangePropagationIbexEntrypoint](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/ibex/VitruviusTGGChangePropagationIbexEntrypoint.java) is the starting point of the change propagation process, which is executed alongside the ibex pattern matching synchronization algorithm.
Shortly described, this algorithm consists of a Pattern Matcher and a SYNC class.
The pattern matcher has a dirty state of the triple graph, where dirty is meant in a sense that the source graph has been changed but the changes haven't been propagated to the target model.
That state includes the triple graph and a protocol (stored in the eclipse ibex project), where the already existing rule applications are stored. 
That way, the pattern matcher can know what part of the graph is not coverd by patterns applications, allowing it to work incrementally.
Knowing that, in the synchronization algorithm, the pattern matcher calculates new pattern matches possible (and patterns that are broken, e.g. if something has been deleted in the source model/graph) 
from the current state and hands it to the SYNC class.
The SYNC class decides which patterns to apply, applies them and then asks the pattern matcher to recalculate, as the triple graph has been updated now and new matches have become possible (possibly).
That ping-pong game is iterated until there are no more new/ broken matches.
While using that same "algorithm template", the 
[VitruviusBackwardConversionTGGEngine](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/ibex/VitruviusBackwardConversionTGGEngine.java) 
slightly deviates from that path, as is explained in the following.

### Backward Conversion Pattern Matching
In the scope of this project, we implement a Pattern Matcher that has additional information that Pattern Matchers in the context of ibex normally don't have: The change sequence.
We hope that by using that additional information, we can find better matches and speed the pattern matching up.
As mentioned before, the integration of the change sequence information in pattern matching changes the Pattern Matching process a bit:
Currently, we create a whole coverage of the change sequence with (converted) TGG patterns, and thus don't need to play the ping pong game more than one time. The change sequence information allows that.
This coverage process works in the following way:

#### Pattern Conversion (Package: patternconversion)
First, all TGG rules that are found in the given eclipse ibex project are converted into so called 
[ChangeSequenceTemplates](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/echange/ChangeSequenceTemplate.java) 
by the 
[IbexPatternToChangeSequenceTemplatedConverter](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/IbexPatternToChangeSequenceTemplateConverter.java).
This is done by doing DFS through the rules (which consist of typed nodes and edges) and creating 
[EChangeWrappers](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/echange/EChangeWrapper.java)
along the way.
Those each represent one _atomic_ EChange as defined in the Vitruvius change model.
Since these changes are interrelated (meaning that two changes oftentimes affect (partly) the same entities), 
we fill those [EChangeWrappers](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/echange/EChangeWrapper.java) 
with [EObjectPlaceholders](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/EObjectPlaceholder.java) that occur in multiple wrappers.
That ensures that the interrelation information is not lost in translation. The placeholders are filled with EObjects in the pattern matching process.

The EChangeWrapper class structure is similar to the Vitruvius change model, but a bit simplified: All concrete EChange subclasses that share the same parameters 
are represented by one concrete subclass of [EChangeWrapper](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/echange/EChangeWrapper.java) .
That way, we don't have massive code duplication or an overly inflated class hierarchy (for our purpose) and only five subclasses instead of eleven...
What [EChangeWrapper](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/echange/EChangeWrapper.java) 
represents which EChange types is documented in the respective subclasses' javadocs.

#### Pattern Matching (Package: patternmatching)
The [VitruviusChangePatternMatcher](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternmatching/VitruviusChangePatternMatcher.java)
takes the converted Patterns, packed together as a _ChangeSequenceTemplateSet_ and matches them over the given change sequence.
The general idea is for the _ChangeSequenceTemplateSet_ to act as a factory for pattern template invocations that can be used by the 
[VitruviusChangePatternMatcher](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternmatching/VitruviusChangePatternMatcher.java)'s algorithm.
Since we want to be able to invoke pattern templates (meaning that they are filled with real EChanges) more than once,
the [ChangeSequenceTemplateSet](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/ChangeSequenceTemplateSet.java) 
provides functionality to return copies of the pattern templates it holdes, given an EChange.
The detailled function of the pattern matching is described in the 
[VitruviusChangePatternMatcher](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternmatching/VitruviusChangePatternMatcher.java) class.
After a pattern coverage of the change sequence is found, they are handed back to the Pattern Matcher ibex interface.

### Handling Broken Matches and handing the matches to SYNC
**The following is NOT yet implemented!**

The [VitruviusBackwardConversionTGGEngine](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/ibex/VitruviusBackwardConversionTGGEngine.java), 
which implements the ibex Pattern Matcher interface, retrieves the pattern coverage and converts it to ibex-readable matches.
It also calculates which matches that already existed (from previous change propagations) are broken by the changes in the change sequences.
Both is handed to SYNC. SYNC applies the matches and (hopefully) keeps the protocol up-to-date.

### Target change generation
**The following is NOT yet implemented!**

Since the ultimate goal is to transform source model changes to target model changes, after the pattern matching process is through, 
we translate the pattern coverage plus the pattern matches introduced by SYNC (as a result of handling broken matches) into target model changes.

## todo 4 code review
* Code-Review anleitung (im Email-Text, nicht hier...)
* ((((Bild malen und hier reinhauen))))
* Git Tag machen
* Mail schreiben

## TODO next
* IBlackinterpreter-Sachen erfüllen
  * IMatch-Zeug (Übersetzung)
  * Broken match handling (Protokoll lesen und VitruvCHange durchgehen)
* targetchange generation
* GGF Unit-Tests schreiben für
  * IbexPatternConverter
  * VitruviusChangePatternMatcher
  * dafür das Something2Else-Modell bisschen erweitern und einfach die Patterns ins repo reinhauen. Das benötigte Ibex-Gedöns möglichst kleinhalten, Vitruvius auch.
* Architektur-Review-Ergebnisse einarbeiten

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
## Install on Linux

* Not tested, but should work better than on windows...
* Change your shell's JAVA_HOME to a Java-23 JDK path:
    ```
    export JAVA_HOME="/PATH/TO/openjdk-23.0.1"
    ```
* Install
    ```
    mvnw clean install
    ```
  
## Test/ Usage
* Testing/ exemplary usage is currently implemented in a private project, only. You need 
  * a methodologist-template-derived test project
  * to select two models there
  * to create a class inheriting TGGChangePropagationSpecification where the models and metamodels are specified.
  * an eMoflon project with defined rules, that is referenced by your test project.