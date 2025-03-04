# Code Review Guidance

Short Description what I think are important spots relevant for a code review.

## Vitruvius Connection
The [TGGChangePropagationSpecification](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/TGGChangePropagationSpecification.java) 
is a key spot, since it is here where the changes are retrieved from Vitruvius and handed to the project.

## Ibex wrapping/ connection
Nothing too interesting, here, in my opinion. Most of the structure of these classes is predetermined by the class structure of eMoflon::IBeX.
The Implementation of the IBeX Pattern Matcher interface (IBlackInterpreter) is 
[VitruviusBackwardConversionTGGEngine](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/ibex/VitruviusBackwardConversionTGGEngine.java),
but that is not of much interest, since it is not developed that far. 
It shows where and the "interesting" spots like pattern conversion and pattern matching are triggered, though.

## Pattern Conversion
In the package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion, the EChangeWrapper structure could be interesting for code review:
* [EChangeWrapper](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/echange/EChangeWrapper.java):
The abstract superclass for EChange wrappers.
* An exemplary subclass like 
[EReferenceValueIndexEChangeWrapper](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/echange/EReferenceValueIndexEChangeWrapper.java)
* [ChangeSequenceTemplate](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/echange/ChangeSequenceTemplate.java) which represents a converted pattern.
to show/ discuss the dynamics of copying, matching and initialization of single EChangeWrappers against EChanges.

Further, the following code spots are interesting for the Pattern Conversion Process
* [IbexPatternToChangeSequenceTemplateConverter](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/IbexPatternToChangeSequenceTemplateConverter.java), 
this is where the conversion algorithm is located
* [ChangeSequenceTemplateSet](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternconversion/ChangeSequenceTemplateSet.java)::getAndInitRelevantIbexPatternTemplatesByEChange

## Pattern Matching
Relevant for pattern matching and interesting for the code review are the pattern conversion and the actual algorithm in [VitruviusChangePatternMatcher](emoflon-integration/src/main/java/tools/vitruv/dsls/tgg/emoflonintegration/patternmatching/VitruviusChangePatternMatcher.java)