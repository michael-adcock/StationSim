package coal;

import StationSim.ElifVertex;
import io.improbable.keanu.algorithms.NetworkSamples;
import io.improbable.keanu.algorithms.VertexSamples;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

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

        VertexSamples switch1 = coalMiningDisastersModel.results.get(coalMiningDisastersModel.switchpoint1);
        VertexSamples switch2 = coalMiningDisastersModel.results.get(coalMiningDisastersModel.switchpoint2);
        VertexSamples early = coalMiningDisastersModel.results.get(coalMiningDisastersModel.earlyRate);
        VertexSamples mid = coalMiningDisastersModel.results.get(coalMiningDisastersModel.midRate);
        VertexSamples late = coalMiningDisastersModel.results.get(coalMiningDisastersModel.lateRate);

        System.out.println("Switchpoint 1 found: " + switch1.getMode());
        System.out.println("Switchpoint 2 found: " + switch2.getMode());
        System.out.println("early rate: " + early.getMode());
        System.out.println("mid rate: " + mid.getMode());
        System.out.println("late rate: " + late.getMode());

        //Charts
        List earlyList = early.asList();
        double[] earlyArray = new double[earlyList.size()];
        for (int i = 0; i < earlyList.size(); i++) {
            earlyArray[i] = (double) earlyList.get(i);
        }

        List midList = mid.asList();
        double[] midArray = new double[midList.size()];
        for (int i = 0; i < midList.size(); i++) {
            midArray[i] = (double) midList.get(i);
        }

        List lateList = late.asList();
        double[] lateArray = new double[lateList.size()];
        for (int i = 0; i < lateList.size(); i++) {
            lateArray[i] = (double) lateList.get(i);
        }

        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries("early", earlyArray,50); // Number of bins is 50
        dataset.addSeries("mid", midArray,50); // Number of bins is 50
        dataset.addSeries("late", lateArray,50); // Number of bins is 50
        JFreeChart chart = ChartFactory.createHistogram("Rates before and after switchpoint", "Lambda value", "Probability",
                dataset, PlotOrientation.VERTICAL, true, false, false);
        ChartFrame frame = new ChartFrame("", chart);
        frame.setVisible(true);
        frame.pack();

        List switch1List = switch1.asList();
        double[] switch1Array = new double[switch1List.size()];
        for (int i = 0; i < switch1List.size(); i++) {
            switch1Array[i] = (double)(int) switch1List.get(i);
        }

        List switch2List = switch2.asList();
        double[] switch2Array = new double[switch2List.size()];
        for (int i = 0; i < switch2List.size(); i++) {
            switch2Array[i] = (double)(int) switch2List.get(i);
        }

        dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries("swithpoint 1", switch1Array,50); // Number of bins is 50
        dataset.addSeries("swithpoint 2", switch2Array,50); // Number of bins is 50
        chart = ChartFactory.createHistogram("Switchpoint", "Step", "Probability",
                dataset, PlotOrientation.VERTICAL, true, false, false);
        frame = new ChartFrame("", chart);
        frame.setVisible(true);
        frame.pack();

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
        switchpoint1 = new UniformIntVertex(startYearVertex, new ConstantVertex<>(100)); // Magic number here !!!!!!!!!
        switchpoint2 = new UniformIntVertex(switchpoint1, endYearVertex);
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
