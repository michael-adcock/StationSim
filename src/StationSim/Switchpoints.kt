package StationSim

import io.improbable.keanu.algorithms.mcmc.MetropolisHastings
import io.improbable.keanu.network.BayesianNetwork
import io.improbable.keanu.randomfactory.RandomDoubleFactory
import io.improbable.keanu.util.csv.ReadCsv
import io.improbable.keanu.vertices.bool.nonprobabilistic.operators.binary.compare.GreaterThanVertex
import io.improbable.keanu.vertices.dbl.DoubleVertex
import io.improbable.keanu.vertices.dbl.nonprobabilistic.CastDoubleVertex
import io.improbable.keanu.vertices.dbl.nonprobabilistic.ConstantDoubleVertex
import io.improbable.keanu.vertices.dbl.probabilistic.ExponentialVertex
import io.improbable.keanu.vertices.dbl.probabilistic.GaussianVertex
import io.improbable.keanu.vertices.intgr.probabilistic.UniformIntVertex
import io.improbable.keanu.vertices.dbltensor.KeanuRandom
import io.improbable.keanu.vertices.generic.nonprobabilistic.ConstantVertex
import io.improbable.keanu.vertices.generic.nonprobabilistic.IfVertex
import io.improbable.keanu.vertices.intgr.probabilistic.PoissonVertex
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartFrame
import org.jfree.chart.ChartPanel
import org.jfree.chart.ChartUtilities
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.statistics.HistogramDataset
import org.jfree.data.statistics.HistogramType
import java.io.IOException
import java.lang.invoke.SwitchPoint
import java.util.HashMap
import java.util.stream.Collectors
import java.util.stream.IntStream


fun findSwitchpoint() {

    // Read in data from csv
    val csvReader = ReadCsv.fromFile("dummy.csv").expectHeader(false)

    // Place dat in hashMap: step to crowding density
    val data = HashMap<Int, Int>()
    for (csvLine in csvReader.readLines()) {
        data[Integer.parseInt(csvLine[0])] = Integer.parseInt(csvLine[1])
    }

    // define start and end
    val startStep : Int = data.keys.min()!!
    val endStep : Int = data.keys.max()!!


    // Define priors for before, during and after rates
    //val alpha = ConstantDoubleVertex(1.0 / data.values.average())
    val alpha = ConstantDoubleVertex(1.0)
    val lambdaOne = ExponentialVertex(alpha, alpha) //Why two inputs?
    val lambdaTwo = ExponentialVertex(alpha, alpha) //Why two inputs?
    val lambdaThree = ExponentialVertex(alpha, alpha) //Why two inputs?

    // Define switchpoint prior
    val tauOne = UniformIntVertex(ConstantVertex(startStep), ConstantVertex(endStep + 1))
    val tauTwo = UniformIntVertex(ConstantVertex(startStep), ConstantVertex(endStep + 1))


    // switchpoint before and after
    var rates = IntStream.range(startStep, endStep).boxed()
            .map<ConstantVertex<Int>>( { ConstantVertex(it) } )
            .map { step ->
                val switchpointOneGreaterThanStep = GreaterThanVertex<Int, Int>(tauOne, step)
                val switchpointTwoGreaterThanStep = GreaterThanVertex<Int, Int>(tauTwo, step)
                val first = IfVertex<Double>(switchpointOneGreaterThanStep, lambdaOne, lambdaTwo)
                IfVertex<Double>(switchpointTwoGreaterThanStep, first, lambdaThree)

            }

    //rates.forEach { element -> println(element.derivedValue) }

    // represent crowing with a Poisson distribution
    val crowding = rates
            .map<CastDoubleVertex> { CastDoubleVertex(it) }
            .map<PoissonVertex> {rate -> PoissonVertex(rate) }
            .collect(Collectors.toList())  // future versions of kotlin won't need collect apparently


    //observe data
    IntStream.range(0, crowding.size).forEach { step ->
        crowding[step].observe(data.getValue(step))
    }

    //run model
    val numSamples = 50000
    val net = BayesianNetwork(lambdaOne.connectedGraph)

    // Get samples from posterior
    val posteriorSamples = MetropolisHastings.getPosteriorSamples(net, net.latentVertices, numSamples, KeanuRandom.getDefaultRandom())

    // Most likely value for each parameter
    val switchPointOne = posteriorSamples.drop(1000).downSample(1).get(tauOne)
    val switchPointTwo = posteriorSamples.drop(1000).downSample(1).get(tauTwo)
    val before = posteriorSamples.drop(1000).downSample(1).get(lambdaOne)
    val during = posteriorSamples.drop(1000).downSample(1).get(lambdaTwo)
    val after = posteriorSamples.drop(1000).downSample(1).get(lambdaThree)
    //println(results.asList())
    println("Switchpoint 1 at: " + switchPointOne.mode)
    println("Switchpoint 2 at: " + switchPointTwo.mode)
    println("Before crowding rate: " + before.mode)
    println("During crowding rate: " + during.mode)
    println("After crowding rate: " + after.mode)

    //charts
    var dataset = HistogramDataset()
    dataset.setType(HistogramType.RELATIVE_FREQUENCY);
    dataset.addSeries("Histogram", before.asList().toDoubleArray(), 10);
    var plotTitle = "Before Crowding"
    var xaxis = "Number of People"
    var yaxis = "Frequency"
    var orientation = PlotOrientation.VERTICAL
    var show = true
    var toolTips = false
    var urls = false
    var chart = ChartFactory.createHistogram(plotTitle, xaxis, yaxis,
            dataset, orientation, show, toolTips, urls)
    var frame = ChartFrame("Crowding Density", chart)
    frame.setVisible(true);
    frame.pack();

    dataset = HistogramDataset()
    dataset.setType(HistogramType.RELATIVE_FREQUENCY);
    dataset.addSeries("Histogram", during.asList().toDoubleArray(), 10);
    plotTitle = "During Crowding"
    chart = ChartFactory.createHistogram(plotTitle, xaxis, yaxis,
            dataset, orientation, show, toolTips, urls)
    frame = ChartFrame("Crowding Density", chart)
    frame.setVisible(true);
    frame.pack();


    dataset = HistogramDataset()
    dataset.setType(HistogramType.RELATIVE_FREQUENCY);
    dataset.addSeries("Histogram", after.asList().toDoubleArray(), 10);
    plotTitle = "After Crowding"
    chart = ChartFactory.createHistogram(plotTitle, xaxis, yaxis,
            dataset, orientation, show, toolTips, urls)
    frame = ChartFrame("Crowding Density", chart)
    frame.setVisible(true);
    frame.pack();


}


fun main(args: Array<String>) {
    findSwitchpoint()
}
