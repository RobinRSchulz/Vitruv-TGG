package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXPatternSet;
import tools.vitruv.change.atomic.EChange;

import java.util.List;

public class IbexPatternConverter {

    private IBeXPatternSet iBeXPatterns;

    public IbexPatternConverter(IBeXPatternSet ibexPatterns) {
        this.iBeXPatterns = ibexPatterns;
    }

    public List<EChange> convert() {
        // TODO implement
        // TODO don't return a list but a smaaaart datastructure
        // todo ibexPatterns mit nem Debugger die Struktur rausfinden!
        this.iBeXPatterns.getContextPatterns().forEach(contextPattern -> {
//            contextPattern;
        });
    }
}
