package coal;



import io.improbable.keanu.algorithms.NetworkSamples;
import io.improbable.keanu.algorithms.VertexSamples;
import io.improbable.keanu.algorithms.mcmc.MetropolisHastings;
import io.improbable.keanu.network.BayesianNetwork;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.GreaterThanVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.CastDoubleVertex;
import io.improbable.keanu.vertices.dbl.probabilistic.ExponentialVertex;
import io.improbable.keanu.vertices.dbltensor.KeanuRandom;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CoalModel {

    public static void main(String[] args) {

        System.out.println("Loading data from a csv file");
        //Data coalMiningDisasterData = Data.load("coal-mining-disaster-data.csv");
        Data coalMiningDisasterData = Data.load("dummy2.csv");

        System.out.println("Creating model using loaded data");
        CoalModel coalMiningDisastersModel = new CoalModel(coalMiningDisasterData);

        System.out.println("Running model...");
        coalMiningDisastersModel.run();
        System.out.println("Run complete");

        // More print statements added
        VertexSamples switchpoint = coalMiningDisastersModel.results.get(coalMiningDisastersModel.switchpoint);
        VertexSamples earlyRate = coalMiningDisastersModel.results.get(coalMiningDisastersModel.earlyRate);
        VertexSamples lateRate = coalMiningDisastersModel.results.get(coalMiningDisastersModel.lateRate);

        System.out.println("Switch year found: " + switchpoint.getMode());
        System.out.println("Early Rate: " + earlyRate.getMode());
        System.out.println("Late Rate: " + lateRate.getMode());

        //Charts
        List earlyList = earlyRate.asList();
        double[] earlyArray = new double[earlyList.size()];
        for (int i = 0; i < earlyList.size(); i++) {
            earlyArray[i] = (double) earlyList.get(i);
        }

        List lateList = lateRate.asList();
        double[] lateArray = new double[lateList.size()];
        for (int i = 0; i < lateList.size(); i++) {
            lateArray[i] = (double) lateList.get(i);
        }

        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries("early", earlyArray,50); // Number of bins is 50
        dataset.addSeries("late", lateArray,50); // Number of bins is 50
        JFreeChart chart = ChartFactory.createHistogram("Rates before and after switchpoint", "Lambda value", "Probability",
                dataset, PlotOrientation.VERTICAL, true, false, false);
        ChartFrame frame = new ChartFrame("", chart);
        frame.setVisible(true);
        frame.pack();

        List switchpointList = switchpoint.asList();
        double[] switchpointArray = new double[switchpointList.size()];
        for (int i = 0; i < switchpointList.size(); i++) {
            switchpointArray[i] = (double)(int) switchpointList.get(i);
        }

        dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries("swithpoint", switchpointArray,50); // Number of bins is 50
        chart = ChartFactory.createHistogram("Switchpoint", "Step", "Probability",
                dataset, PlotOrientation.VERTICAL, true, false, false);
        frame = new ChartFrame("", chart);
        frame.setVisible(true);
        frame.pack();

    }

    private final KeanuRandom random;

    final ConstantVertex<Integer> startYearVertex;
    final ConstantVertex<Integer> endYearVertex;
    final ExponentialVertex earlyRate;
    final ExponentialVertex lateRate;
    final List<PoissonVertex> disasters;
    final UniformIntVertex switchpoint;

    final Data data;
    NetworkSamples results;

    public CoalModel(Data data) {
        this.data = data;
        random = new KeanuRandom(1);

        startYearVertex = new ConstantVertex<>(data.startYear);
        endYearVertex = new ConstantVertex<>(data.endYear + 1);
        switchpoint = new UniformIntVertex(startYearVertex, endYearVertex);
        earlyRate = new ExponentialVertex(1.0, 1.0);
        lateRate = new ExponentialVertex(1.0, 1.0);

        Stream<IfVertex<Double>> rates = IntStream.range(data.startYear, data.endYear).boxed()
                .map(ConstantVertex::new)
                .map(year -> {
                    GreaterThanVertex<Integer, Integer> switchpointGreaterThanYear = new GreaterThanVertex<>(
                            switchpoint,
                            year
                    );
                    return new IfVertex<>(switchpointGreaterThanYear, earlyRate, lateRate);
                });

        disasters = rates
                .map(CastDoubleVertex::new)
                .map(PoissonVertex::new)
                .collect(Collectors.toList());

        IntStream.range(0, disasters.size()).forEach(i -> {
            Integer year = data.startYear + i;
            Integer observedValue = data.yearToDisasterCounts.get(year);
            disasters.get(i).observe(observedValue);
        });
    }

    /**
     * Runs the MetropolisHastings algorithm and saves the resulting samples to results
     */
    public void run() {
        BayesianNetwork net = new BayesianNetwork(switchpoint.getConnectedGraph());
        Integer numSamples = 50000;
        NetworkSamples posteriorDistSamples = MetropolisHastings.getPosteriorSamples(net, net.getLatentVertices(), numSamples, random);

        Integer dropCount = 1000;
        results = posteriorDistSamples.drop(dropCount).downSample(5);
    }

}