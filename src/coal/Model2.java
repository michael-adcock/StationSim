package coal;

import StationSim.ElifVertex;
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

public class Model2 {

    public static void main(String[] args) {

        System.out.println("Java");
        Data coalMiningDisasterData = Data.load("dummy.csv");

        //for (int i = 0; i < coalMiningDisasterData.yearToDisasterCounts.size(); i++) {
        //    System.out.println("key : " + i + "\tvalue: " + coalMiningDisasterData.yearToDisasterCounts.get(i));

        //}



        System.out.println("Creating model using loaded data");
        Model2 coalMiningDisastersModel = new Model2(coalMiningDisasterData);

        //System.out.println(coalMiningDisastersModel.startYearVertex.getDerivedValue());
        //System.out.println(coalMiningDisastersModel.endYearVertex.getDerivedValue());



        System.out.println("Running model...");
        coalMiningDisastersModel.run();
        System.out.println("Run complete");

        int switch1 = coalMiningDisastersModel.results.get(coalMiningDisastersModel.switchpoint1).getMode();
        int switch2 = coalMiningDisastersModel.results.get(coalMiningDisastersModel.switchpoint2).getMode();
        double late = coalMiningDisastersModel.results.get(coalMiningDisastersModel.lateRate).getMode();
        double mid = coalMiningDisastersModel.results.get(coalMiningDisastersModel.midRate).getMode();
        double early = coalMiningDisastersModel.results.get(coalMiningDisastersModel.earlyRate).getMode();
        System.out.println("Switchpoint 1 found: " + switch1);
        System.out.println("Switchpoint 2 found: " + switch2);
        System.out.println("early rate: " + early);
        System.out.println("mid rate: " + mid);
        System.out.println("late rate: " + late);
        System.out.println(coalMiningDisastersModel.results.get(coalMiningDisastersModel.lateRate).asList());
    }

    private final Random r;

    final ConstantVertex<Integer> startYearVertex;
    final ConstantVertex<Integer> endYearVertex;
    final ExponentialVertex earlyRate;
    final ExponentialVertex lateRate;
    final ExponentialVertex midRate;
    final List<PoissonVertex> disasters;
    final UniformIntVertex switchpoint1;
    final UniformIntVertex switchpoint2;

    final Data data;
    NetworkSamples results;

    public Model2(Data data) {
        this.data = data;
        r = new Random(1);

        startYearVertex = new ConstantVertex<>(data.startYear);
        endYearVertex = new ConstantVertex<>(data.endYear + 1);
        switchpoint1 = new UniformIntVertex(startYearVertex, endYearVertex);
        switchpoint2 = new UniformIntVertex(startYearVertex, endYearVertex);
        earlyRate = new ExponentialVertex(new ConstantDoubleVertex(1.0), new ConstantDoubleVertex(1.0));
        lateRate = new ExponentialVertex(new ConstantDoubleVertex(1.0), new ConstantDoubleVertex(1.0));
        midRate = new ExponentialVertex(new ConstantDoubleVertex(1.0), new ConstantDoubleVertex(1.0));


        Stream<ElifVertex<Double>> rates = IntStream.range(data.startYear, data.endYear).boxed()
                .map(ConstantVertex::new)
                .map(year -> {
                    GreaterThanVertex<Integer, Integer> switchpoint1GreaterThanYear = new GreaterThanVertex<>(
                            switchpoint1,
                            year
                    );
                    GreaterThanVertex<Integer, Integer> switchpoint2GreaterThanYear = new GreaterThanVertex<>(
                            switchpoint2,
                            year
                    );
                    //System.out.println(switchpointGreaterThanYear + " " + earlyRate + " " + lateRate);
                    return new ElifVertex<>(switchpoint1GreaterThanYear, switchpoint2GreaterThanYear,
                            earlyRate, midRate, lateRate);
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
        BayesianNetwork net = new BayesianNetwork(switchpoint1.getConnectedGraph());
        Integer numSamples = 50000;
        NetworkSamples posteriorDistSamples = MetropolisHastings.getPosteriorSamples(net, net.getLatentVertices(), numSamples);

        Integer dropCount = 1000;
        results = posteriorDistSamples.drop(dropCount).downSample(5);
    }

}
