package tools.vitruv.dsls.tgg.emoflonintegration.ibex.patternconversion;

import org.apache.log4j.Logger;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContext;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXContextPattern;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXModel;
import org.emoflon.ibex.patternmodel.IBeXPatternModel.IBeXPatternSet;
import tools.vitruv.change.atomic.EChange;

import java.util.List;

public class IbexPatternConverter {

    protected static final Logger logger = Logger.getRootLogger();

    private IBeXModel iBeXModel;

    public IbexPatternConverter(IBeXModel iBeXModel) {
        this.iBeXModel = iBeXModel;
    }

    public VitruviusChangeTemplateSet convert() {
        // TODO implement
        // TODO don't return a list but a smaaaart datastructure
        // todo ibexPatterns mit nem Debugger die Struktur rausfinden!
        this.iBeXModel.getPatternSet().getContextPatterns().forEach(contextPattern -> {
            logger.info("ContextPattern: " + contextPattern.getName());
        });
        throw new RuntimeException("Make here weiter!");
//        return null;
    }

//    private int parseIBeXContext(IBeXContext iBeXContext) {
//        switch (iBeXContext) {
//            case IBeXContextPattern p -> logger.info("IbexContextPattern");
//            default -> logger.info("default");
//        }
//    }
}
