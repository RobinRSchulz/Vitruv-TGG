package tools.vitruv.dsls.tgg.emoflonintegration.ibex.smartEmfFix;

import org.eclipse.emf.common.notify.Adapter;
import org.emoflon.smartemf.persistence.AdapterList;
import org.emoflon.smartemf.persistence.SmartEMFResource;

/**
 * An AdapterList implementation that does nothing but also doesn't throw {@link UnsupportedOperationException} like {@link AdapterList} does which breaks some stuff.
 */
public class AdapterListFixed extends AdapterList {
    public AdapterListFixed(SmartEMFResource resource) {
        super(resource);
    }

    @Override
    public int indexOf(Object o) {
//        this.adapters.toArray()
//        return super.indexOf(o);
        //todo check if more to do ...
        return 0;
    }

    @Override
    public void add(int index, Adapter element) {
        //noop. Todo check if not so goodie
//        super.add(index, element);
    }
}
