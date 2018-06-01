package StationSim;

import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.dbltensor.KeanuRandom;
import io.improbable.keanu.vertices.generic.nonprobabilistic.NonProbabilistic;

public class ElifVertex<T> extends NonProbabilistic<T> {

    private final Vertex<Boolean> predicate1;
    private final Vertex<Boolean> predicate2;
    private final Vertex<T> thn;
    private final Vertex<T> elif;
    private final Vertex<T> els;

    public ElifVertex(Vertex<Boolean> predicate1, Vertex<Boolean> predicate2, Vertex<T> thn, Vertex<T> elif, Vertex<T> els) {
        this.predicate1 = predicate1;
        this.predicate2 = predicate2;
        this.thn = thn;
        this.elif = elif;
        this.els = els;
        setParents(predicate1, thn, elif, els);
    }

    @Override
    public T sample(KeanuRandom random) {
        return op(predicate1.sample(random), predicate2.sample(random),
                thn.sample(random), elif.sample(random), els.sample(random));
    }

    @Override
    public T getDerivedValue() {
        return op(predicate1.getValue(), predicate2.getValue(),
                elif.getValue(), thn.getValue(), els.getValue());
    }

    private T op(boolean predicate1, boolean predicate2, T thn, T elif, T els) {
        if (predicate1) {
            return thn;
        } else if (predicate2) {
            return elif;
        } else {
            return els;
        }
    }

}