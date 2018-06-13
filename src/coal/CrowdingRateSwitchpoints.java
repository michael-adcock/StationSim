package coal;


import io.improbable.keanu.algorithms.NetworkSamples;
import io.improbable.keanu.algorithms.mcmc.MetropolisHastings;
import io.improbable.keanu.network.BayesianNetwork;
import io.improbable.keanu.tensor.bool.BooleanTensor;
import io.improbable.keanu.vertices.ConstantVertex;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.GreaterThanVertex;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.KeanuRandom;
import io.improbable.keanu.vertices.dbl.probabilistic.SmoothUniformVertex;
import io.improbable.keanu.vertices.generic.nonprobabilistic.If;
import io.improbable.keanu.vertices.intgr.nonprobabilistic.ConstantIntegerVertex;
import io.improbable.keanu.vertices.intgr.probabilistic.PoissonVertex;
import io.improbable.keanu.vertices.intgr.probabilistic.UniformIntVertex;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import java.util.List;

public class CrowdingRateSwitchpoints {

    DoubleVertex beforeRate;
    DoubleVertex duringRate;
    DoubleVertex afterRate;
    UniformIntVertex switchPoint1;
    UniformIntVertex switchPoint2;

    Data data;
    NetworkSamples results;

    public CrowdingRateSwitchpoints(Data data) {
        this.data = data;
        KeanuRandom.setDefaultRandomSeed(1);
    }

    public void run() {
        BayesianNetwork net = buildBayesianNetwork();
        Integer numSamples = 11000;

        NetworkSamples posteriorDistSamples = MetropolisHastings.getPosteriorSamples(
                net,
                net.getLatentVertices(),
                numSamples,
                KeanuRandom.getDefaultRandom()
        );

        results = posteriorDistSamples.drop(1000).downSample(3);
    }

    private BayesianNetwork buildBayesianNetwork() {

        switchPoint1 = new UniformIntVertex(data.startYear, data.endYear + 1);
        switchPoint2 = new UniformIntVertex(data.startYear, data.endYear + 1);
        beforeRate = new SmoothUniformVertex(0.0, 500.0);
        duringRate = new SmoothUniformVertex(0.0, 500.0);
        afterRate = new SmoothUniformVertex(0.0, 500.0);

        ConstantIntegerVertex years = ConstantVertex.of(data.years);

        Vertex<BooleanTensor> greaterThanSwitchpoint1 = new GreaterThanVertex<>(switchPoint1, years);
        Vertex<BooleanTensor> greaterThanSwitchpoint2 = new GreaterThanVertex<>(switchPoint2, years);

        DoubleVertex rateForYear = If.isTrue(greaterThanSwitchpoint1)
                .then(beforeRate)
                .orElse(duringRate);

        rateForYear = If.isTrue(greaterThanSwitchpoint2)
                .then(rateForYear)
                .orElse(afterRate);


        PoissonVertex disastersForYear = new PoissonVertex(rateForYear);

        disastersForYear.observe(data.disasters);

        return new BayesianNetwork(switchPoint1.getConnectedGraph());
    }

    public static void main(String[] args) {

        Data data = Data.load("dummy.csv");

        CrowdingRateSwitchpoints model = new CrowdingRateSwitchpoints(data);
        model.run();

        double beforeRate = model.results.getDoubleTensorSamples(model.beforeRate).getMode().scalar();
        double duringRate = model.results.getDoubleTensorSamples(model.duringRate).getMode().scalar();
        double afterRate = model.results.getDoubleTensorSamples(model.afterRate).getMode().scalar();
        int switchPoint1 = model.results.getIntegerTensorSamples(model.switchPoint1).getScalarMode();
        int switchPoint2 = model.results.getIntegerTensorSamples(model.switchPoint2).getScalarMode();

        System.out.println("Rate before: " + beforeRate);
        System.out.println("Rate during: " + duringRate);
        System.out.println("Rate after: " + afterRate);
        System.out.println("Switchpoint 1: " + switchPoint1);
        System.out.println("Switchpoint 2: " + switchPoint2);

        //Charts
        List earlyList = model.results.getDoubleTensorSamples(model.beforeRate).asList();
        double[] earlyArray = new double[earlyList.size()];
        for (int i = 0; i < earlyList.size(); i++) {
            earlyArray[i] = (double) earlyList.get(i);
        }

        List midList = model.results.getDoubleTensorSamples(model.duringRate).asList();
        double[] midArray = new double[midList.size()];
        for (int i = 0; i < midList.size(); i++) {
            midArray[i] = (double) midList.get(i);
        }

        List lateList = model.results.getDoubleTensorSamples(model.afterRate).asList();
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

        List switch1List = model.results.getIntegerTensorSamples(model.switchPoint1).asList();
        double[] switch1Array = new double[switch1List.size()];
        for (int i = 0; i < switch1List.size(); i++) {
            switch1Array[i] = (double)(int) switch1List.get(i);
        }

        List switch2List = model.results.getIntegerTensorSamples(model.switchPoint2).asList();
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

}
