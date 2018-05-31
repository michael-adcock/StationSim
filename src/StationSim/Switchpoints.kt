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
import java.util.HashMap
import java.util.stream.Collectors
import java.util.stream.IntStream


fun findSwitchpoint() {

    println("Kotlin")

    // Read in data from csv
    val csvReader = ReadCsv.fromFile("dummy.csv").expectHeader(false)

    val data = HashMap<Int, Int>()
    for (csvLine in csvReader.readLines()) {
        data[Integer.parseInt(csvLine[0])] = Integer.parseInt(csvLine[1])
    }

    //for( i in 0..data.size - 1) {
    //    println("key : " + i + "\tvalue: " + data.getValue(i))
    //}



    val meanCount = data.values.average()
    val countLength = data.size
    val startStep : Int = data.keys.min()!!
    val endStep : Int = data.keys.max()!!
    val startStepVertex = ConstantVertex(startStep)
    val endStepVertex = ConstantVertex(endStep + 1) // Why plus one?

    //println(startStep.derivedValue)
    //println(endStep.derivedValue)



    //val alpha = ConstantDoubleVertex(1.0 / meanCount)
    val alpha = ConstantDoubleVertex(1.0)
    val lambdaOne = ExponentialVertex(alpha, alpha) //Why two inputs?
    val lambdaTwo = ExponentialVertex(alpha, alpha) //Why two inputs?
    //val lambdaThree = ExponentialVertex(alpha, alpha) //Why two inputs?

    val tauOne = UniformIntVertex(startStepVertex, endStepVertex)
    //val tauTwo = UniformIntVertex(0, countLength)

    println("LambdaOne: " + lambdaOne.value)
    println("LambdaTwo: " + lambdaTwo.value)
    println("TauOne: " + tauOne.value)


    val rates = IntStream.range(startStep, endStep).boxed()
            .map<ConstantVertex<Int>>( { ConstantVertex(it) } )
            .map { step ->
                val switchpointGreaterThanStep = GreaterThanVertex<Int, Int>(tauOne, step)
                IfVertex<Double>(switchpointGreaterThanStep, lambdaOne, lambdaTwo)
            }


    //for (x : IfVertex<Double> in rates) {
    //    println(x.derivedValue)
    //}



    val crowding = rates
            .map<CastDoubleVertex> { CastDoubleVertex(it) }
            .map<PoissonVertex> {rate -> PoissonVertex(rate) }
            .collect(Collectors.toList())  // future versions of kotlin won't need collect apparently

    //for (x : PoissonVertex in crowding) {
    //    println(x.value)
    //}


    //println(lambdaOne.connectedGraph)

    IntStream.range(0, crowding.size).forEach { step ->
        crowding[step].observe(data.getValue(step))
    }


    //for (step in 0..crowding.size - 1) {
    //    val observation = data.getValue(step)
    //    crowding[step].observe(observation)
    //}

    //for (x : PoissonVertex in crowding) {
    //    println(x.value)
    //}



    val numSamples = 50000
    val net = BayesianNetwork(lambdaOne.connectedGraph)
    val posteriorSamples = MetropolisHastings.getPosteriorSamples(net, net.latentVertices, numSamples, KeanuRandom.getDefaultRandom())


    val results = posteriorSamples.drop(1000).downSample(5).get(tauOne)
    //println(results.asList())
    println("Switchpoint at: " + results.mode)

}


fun main(args: Array<String>) {
    findSwitchpoint()
}

/*
It uses data of yearly number of coal mining accidents in the UK. The aim is to infer the "switch point" year, when the rate of accidents changes to a lower rate (this could be due to the introduction of a new safety process). The number of accidents in each year is modelled by a poisson distribution, with different rates before and after this switch point. These different rates are hyper parameters to the Poisson distribution, and are modelled as exponential vertexes.

It uses Metropolis Hastings Monte Carlo to sample the posterior distribution of the switch point, since this uses non continuous Integer Vertices you cannot use the MAP method.
 */