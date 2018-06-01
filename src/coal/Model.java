package coal;

import io.improbable.keanu.algorithms.NetworkSamples;
import io.improbable.keanu.algorithms.mcmc.MetropolisHastings;
import io.improbable.keanu.network.BayesianNetwork;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.GreaterThanVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.CastDoubleVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.ConstantDoubleVertex;
import io.improbable.keanu.vertices.dbl.probabilistic.ExponentialVertex;
import io.improbable.keanu.vertices.generic.nonprobabilistic.ConstantVertex;
import io.improbable.keanu.vertices.generic.nonprobabilistic.IfVertex;
import io.improbable.keanu.vertices.intgr.probabilistic.PoissonVertex;
import io.improbable.keanu.vertices.intgr.probabilistic.UniformIntVertex;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Model {

    public static void main(String[] args) {

        System.out.println("Java");
        //Data coalMiningDisasterData = Data.load("coal-mining-disaster-data.csv");
        Data coalMiningDisasterData = Data.load("dummy2.csv");

        //for (int i = 0; i < coalMiningDisasterData.yearToDisasterCounts.size(); i++) {
        //    System.out.println("key : " + i + "\tvalue: " + coalMiningDisasterData.yearToDisasterCounts.get(i));

        //}

        System.out.println("Creating model using loaded data");
        Model coalMiningDisastersModel = new Model(coalMiningDisasterData);

        //System.out.println(coalMiningDisastersModel.startYearVertex.getDerivedValue());
        //System.out.println(coalMiningDisastersModel.endYearVertex.getDerivedValue());

        System.out.println("Running model...");
        coalMiningDisastersModel.run();
        System.out.println("Run complete");

        int switchYear = coalMiningDisastersModel.results.get(coalMiningDisastersModel.switchpoint).getMode();
        double late = coalMiningDisastersModel.results.get(coalMiningDisastersModel.lateRate).getMode();
        double early = coalMiningDisastersModel.results.get(coalMiningDisastersModel.earlyRate).getMode();

        System.out.println("Switch year found: " + switchYear);
        System.out.println("early rate: " + early);
        System.out.println("late rate: " + late);
        System.out.println(coalMiningDisastersModel.results.get(coalMiningDisastersModel.lateRate).asList());
    }

    private final Random r;

    final ConstantVertex<Integer> startYearVertex;
    final ConstantVertex<Integer> endYearVertex;
    final ExponentialVertex earlyRate;
    final ExponentialVertex lateRate;
    final List<PoissonVertex> disasters;
    final UniformIntVertex switchpoint;

    final Data data;
    NetworkSamples results;

    public Model(Data data) {
        this.data = data;
        r = new Random(1);

        startYearVertex = new ConstantVertex<>(data.startYear);
        endYearVertex = new ConstantVertex<>(data.endYear + 1);
        switchpoint = new UniformIntVertex(startYearVertex, endYearVertex);
        earlyRate = new ExponentialVertex(new ConstantDoubleVertex(1.0), new ConstantDoubleVertex(1.0));
        lateRate = new ExponentialVertex(new ConstantDoubleVertex(1.0), new ConstantDoubleVertex(1.0));


        Stream<IfVertex<Double>> rates = IntStream.range(data.startYear, data.endYear).boxed()
                .map(ConstantVertex::new)
                .map(year -> {
                    GreaterThanVertex<Integer, Integer> switchpointGreaterThanYear = new GreaterThanVertex<>(
                            switchpoint,
                            year
                    );
                    //System.out.println(switchpointGreaterThanYear + " " + earlyRate + " " + lateRate);
                    return new IfVertex<>(switchpointGreaterThanYear, earlyRate, lateRate);
                });




        disasters = rates
                .map(CastDoubleVertex::new)
                .map(rate -> new PoissonVertex(rate))
                .collect(Collectors.toList());

        //disasters.forEach(
        //element -> System.out.println(element.getValue()));

        IntStream.range(0, disasters.size()).forEach(i -> {
            Integer year = data.startYear + i;
            Integer observedValue = data.yearToDisasterCounts.get(year);
            disasters.get(i).observe(observedValue);
        });

        //disasters.forEach(
        //        element -> System.out.println(element.getValue()));
        //System.exit(0);
    }



    /**
     * Runs the MetropolisHastings algorithm and saves the resulting samples to results
     */
    public void run() {
        BayesianNetwork net = new BayesianNetwork(switchpoint.getConnectedGraph());
        Integer numSamples = 50000;
        NetworkSamples posteriorDistSamples = MetropolisHastings.getPosteriorSamples(net, net.getLatentVertices(), numSamples);

        Integer dropCount = 1000;
        results = posteriorDistSamples.drop(dropCount).downSample(5);
    }

}
