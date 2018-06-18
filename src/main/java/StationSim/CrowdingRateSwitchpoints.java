import io.improbable.keanu.algorithms.NetworkSamples;
import io.improbable.keanu.algorithms.mcmc.MetropolisHastings;
import io.improbable.keanu.network.BayesianNetwork;
import io.improbable.keanu.tensor.bool.BooleanTensor;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.tensor.intgr.IntegerTensor;
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
        List<DoubleTensor> earlyList = model.results.getDoubleTensorSamples(model.beforeRate).asList();
        double[] earlyArray = new double[earlyList.size()];
        for (int i = 0; i < earlyList.size(); i++) {
            earlyArray[i] = earlyList.get(i).scalar();
        }

        List<DoubleTensor> midList = model.results.getDoubleTensorSamples(model.duringRate).asList();
        double[] midArray = new double[midList.size()];
        for (int i = 0; i < midList.size(); i++) {
            midArray[i] = midList.get(i).scalar();
        }

        List<DoubleTensor> lateList = model.results.getDoubleTensorSamples(model.afterRate).asList();
        double[] lateArray = new double[lateList.size()];
        for (int i = 0; i < lateList.size(); i++) {
            lateArray[i] = lateList.get(i).scalar();
        }

        HistogramDataset dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries("early", earlyArray,10); // Number of bins is 50
        JFreeChart chart = ChartFactory.createHistogram("Early Rate", "Lambda value", "Probability",
                dataset, PlotOrientation.VERTICAL, true, false, false);
        ChartFrame frame = new ChartFrame("", chart);
        frame.setVisible(true);
        frame.pack();

        dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries("mid", midArray,10); // Number of bins is 50
        chart = ChartFactory.createHistogram("Mid Rate", "Lambda value", "Probability",
                dataset, PlotOrientation.VERTICAL, true, false, false);
        frame = new ChartFrame("", chart);
        frame.setVisible(true);
        frame.pack();

        dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries("late", lateArray,10); // Number of bins is 50
        chart = ChartFactory.createHistogram("Late Rate", "Lambda value", "Probability",
                dataset, PlotOrientation.VERTICAL, true, false, false);
        frame = new ChartFrame("", chart);
        frame.setVisible(true);
        frame.pack();




        List<IntegerTensor> switch1List = model.results.getIntegerTensorSamples(model.switchPoint1).asList();
        double[] switch1Array = new double[switch1List.size()];
        for (int i = 0; i < switch1List.size(); i++) {
            switch1Array[i] = switch1List.get(i).scalar();
        }

        List<IntegerTensor> switch2List = model.results.getIntegerTensorSamples(model.switchPoint2).asList();
        double[] switch2Array = new double[switch2List.size()];
        for (int i = 0; i < switch2List.size(); i++) {
            switch2Array[i] = switch2List.get(i).scalar();
        }

        dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        dataset.addSeries("swithpoint 1", switch1Array,10); // Number of bins is 50
        //dataset.addSeries("swithpoint 2", switch2Array,10); // Number of bins is 50
        chart = ChartFactory.createHistogram("Switchpoint", "Step", "Probability",
                dataset, PlotOrientation.VERTICAL, true, false, false);
        frame = new ChartFrame("", chart);
        frame.setVisible(true);
        frame.pack();

        dataset = new HistogramDataset();
        dataset.setType(HistogramType.RELATIVE_FREQUENCY);
        //dataset.addSeries("swithpoint 1", switch1Array,10); // Number of bins is 50
        dataset.addSeries("swithpoint 2", switch2Array,10); // Number of bins is 50
        chart = ChartFactory.createHistogram("Switchpoint", "Step", "Probability",
                dataset, PlotOrientation.VERTICAL, true, false, false);
        frame = new ChartFrame("", chart);
        frame.setVisible(true);
        frame.pack();

    }

}
