package tools.vitruv.dsls.tgg.emoflonintegration.patternmatching;

import edu.kit.ipd.sdq.commons.util.java.Pair;
import org.eclipse.emf.ecore.EObject;
import tools.vitruv.change.atomic.EChange;
import tools.vitruv.change.composite.description.VitruviusChange;
import tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange.ChangeSequenceTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enables flattening the coverage of a {@link VitruviusChange} with {@link ChangeSequenceTemplate}s (in the following: pattern applications) to 1 per change each.<br/>
 * We need to make a choice between different pattern applications, and this class does it in the following way:
 * <ol>
 *     <li/> Leaning on Khelladi's containment level heuristic, we detect containment relations between pattern applications and remove all but the "top container".
 *     <li/> If there are still conflicts, we use a mix of our own maxCoverage heuristic and Khelladi's distance heuristic:
 *     <ol>
 *         <li/> First, for each conflict, we find the coverage-maximal (most EChanges covered) pattern applications.
 *         <li/> Then, from these pattern applications, we find the distance-minimal candidates.
 *         <li/> From the remaining pattern applications, we arbitrarily choose one of the candidates.
 *     </ol>
 *
 * </ol>
 */
public class PatternCoverageFlattener {
    /**
     * This remains unmodified, only provides the sorting!
     */
    private final LinkedList<VitruviusBackwardConversionMatch> sortedPatternApplications;
    /**
     * This is the set we modify and return
     */
    private final Set<VitruviusBackwardConversionMatch> patternApplications;
    private final List<EChange<EObject>> changeSequence;
    private final Map<EChange<EObject>, Integer> changeSequenceIndexMap;

    private static final Map<EChange<EObject>, Set<VitruviusBackwardConversionMatch>> staticEChangeToPatternApplicationMap = new HashMap<>();
    private static final Set<VitruviusBackwardConversionMatch> staticPatternApplicationsSet = new HashSet<>();


    public PatternCoverageFlattener(Set<VitruviusBackwardConversionMatch> patternApplications, VitruviusChange<EObject> vitruviusChange) {
        this.patternApplications = new HashSet<>(patternApplications);
        this.changeSequence = vitruviusChange.getEChanges();

        // performance optimizations
        this.changeSequenceIndexMap = new HashMap<>(changeSequence.size());
        initializeIndexesForChangeSequence();
        initializeStaticEChangeToPatternApplicationMap(patternApplications);

        this.sortedPatternApplications = patternApplications.stream()
                .sorted(Comparator.comparingInt(a -> a.getMatchedChangeSequenceTemplate().getEChangeWrappers().size()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private void initializeStaticEChangeToPatternApplicationMap(Set<VitruviusBackwardConversionMatch> patternApplications) {
        staticPatternApplicationsSet.addAll(patternApplications);
        for (EChange<EObject> eChange : changeSequence) {
            Set<VitruviusBackwardConversionMatch> staticPatternApplications = staticEChangeToPatternApplicationMap
                    .computeIfAbsent(eChange, eChange1 -> new HashSet<>());

            for (VitruviusBackwardConversionMatch vitruviusBackwardConversionMatch : patternApplications) {
                if (!staticPatternApplicationsSet.contains(vitruviusBackwardConversionMatch)) {
                    if (vitruviusBackwardConversionMatch.getMatchedChangeSequenceTemplate().getEChanges().contains(eChange)) {
                        staticPatternApplications.add(vitruviusBackwardConversionMatch);
                    }
                }
            }
        }
    }

    private void initializeIndexesForChangeSequence() {
        int i = 0;
        for (EChange<EObject> eChange : this.changeSequence) {
            this.changeSequenceIndexMap.put(eChange, i);
        }
    }

    /**
     * Apply flattening and return the result.
     * @return the {@link VitruviusBackwardConversionMatch}es that remain after flattening.
     */
    public Set<VitruviusBackwardConversionMatch> getFlattenedPatternApplications() {
        if (isFlat()) {
            // early return
            return patternApplications;
        }
        applyContainmentLevelHeuristic();
        if (isFlat()) {
            // early return
            return patternApplications;
        }
        applyMaxCoverageAndDistanceHeuristic();
        assertFlatness();
        return patternApplications;
    }

    private void applyContainmentLevelHeuristic() {
        ListIterator<VitruviusBackwardConversionMatch> listIterator = sortedPatternApplications.listIterator();
        while (listIterator.hasNext()) {
            VitruviusBackwardConversionMatch patternApplication = listIterator.next();
            if (listIterator.hasNext()) {
                for (VitruviusBackwardConversionMatch containerCandidate : sortedPatternApplications.subList(listIterator.nextIndex(), sortedPatternApplications.size())) {
                    if (isContainedIn(patternApplication.getMatchedChangeSequenceTemplate(), containerCandidate.getMatchedChangeSequenceTemplate())) {
                        patternApplications.remove(patternApplication);
                        // we don't need to run through ALL containers, since the next will be covered anyway...
                        break;
                    }
                }
                if (!patternApplications.contains(patternApplication)) {
                    listIterator.remove();
                }
            }
        }
    }

    private boolean isContainedIn(ChangeSequenceTemplate potentialContainee, ChangeSequenceTemplate potentialContainer) {
        Set<EChange<EObject>> containerEChanges = potentialContainer.getEChanges();
        Set<EChange<EObject>> containeeEChanges = potentialContainee.getEChanges();
        return containerEChanges.containsAll(containeeEChanges);
    }

    private void applyMaxCoverageAndDistanceHeuristic() {
        for (EChange<EObject> eChange : changeSequence) {
            Set<VitruviusBackwardConversionMatch> relevantPatternApplications = getRelevantPatternApplications(eChange);
            if (relevantPatternApplications.isEmpty()) continue;
            int maxCoverage = getMaxCoverage(relevantPatternApplications);
            // first ensure only maximum-coverage pattern applications are chosen. Then choose the one with the highest density
            VitruviusBackwardConversionMatch survivingPatternApplication = relevantPatternApplications.stream()
                    .filter(patternApplication -> patternApplication.getMatchedChangeSequenceTemplate().getEChanges().size() == maxCoverage)
                    .map(patternApplication -> new Pair<>(patternApplication, densityHeuristic(patternApplication)))
                    .max(Comparator.comparing(Pair::getSecond))
                    .orElseThrow(() -> new IllegalStateException("No ChangeSequenceTemplates"))
                    .getFirst();
            // remove all but the survivor
            patternApplications.removeAll(relevantPatternApplications.stream()
                    .filter(patternApplication -> !patternApplication.equals(survivingPatternApplication))
                    .collect(Collectors.toSet()));
        }
    }

    /**
     *
     * @return Khelladi's distance/ density heuristic: is between 0 and 1. the higher, the denser the changes are grouped together
     */
    private double densityHeuristic(VitruviusBackwardConversionMatch changeSequenceTemplate) {
        List<Integer> indexes = changeSequenceTemplate.getMatchedChangeSequenceTemplate().getEChanges().stream().map(changeSequenceIndexMap::get).toList();
        int firstIndex = indexes.stream().mapToInt(i -> i).min().orElseThrow(() -> new IllegalStateException("Empty ChangeSequenceTemplate"));
        int lastIndex = indexes.stream().mapToInt(i -> i).max().orElseThrow(() -> new IllegalStateException("Empty ChangeSequenceTemplate"));
        int size = changeSequenceTemplate.getMatchedChangeSequenceTemplate().getEChangeWrappers().size();

        return ((double)(size-1)) / ((double)(lastIndex - firstIndex));
    }

    private Set<VitruviusBackwardConversionMatch> getRelevantPatternApplications(EChange<EObject> eChange) {
        // for performance reasons, the map is pre-calculated. Since patternApplications contains the current coverage, these are significant.
        return staticEChangeToPatternApplicationMap.get(eChange).stream()
                .filter(patternApplications::contains)
                .collect(Collectors.toSet());
    }

    private int getMaxCoverage(Set<VitruviusBackwardConversionMatch> relevantPatternApplications) {
        return relevantPatternApplications.stream()
                .map(patternApplication -> patternApplication.getMatchedChangeSequenceTemplate().getEChanges().size())
                .mapToInt(size -> size)
                .max()
                .orElseThrow(() -> new IllegalStateException("Empty ChangeSequenceTemplate"));
    }


    private boolean isFlat() {
        return changeSequence.stream()
                .noneMatch(eChange -> getRelevantPatternApplications(eChange).size() > 1);
    }
    /**
     * If conflicts exist after flattening, the algorithm is faulty.
     *
     */
    private void assertFlatness() {
        assert isFlat();
    }
}
